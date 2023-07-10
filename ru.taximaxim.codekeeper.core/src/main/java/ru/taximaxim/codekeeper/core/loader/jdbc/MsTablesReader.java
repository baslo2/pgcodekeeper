/*******************************************************************************
 * Copyright 2017-2023 TAXTELECOM, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ru.taximaxim.codekeeper.core.loader.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import ru.taximaxim.codekeeper.core.MsDiffUtils;
import ru.taximaxim.codekeeper.core.loader.QueryBuilder;
import ru.taximaxim.codekeeper.core.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.core.parsers.antlr.expr.launcher.MsExpressionAnalysisLauncher;
import ru.taximaxim.codekeeper.core.schema.AbstractColumn;
import ru.taximaxim.codekeeper.core.schema.AbstractSchema;
import ru.taximaxim.codekeeper.core.schema.GenericColumn;
import ru.taximaxim.codekeeper.core.schema.MsColumn;
import ru.taximaxim.codekeeper.core.schema.MsTable;
import ru.taximaxim.codekeeper.core.schema.MsType;

public class MsTablesReader extends JdbcReader {

    public MsTablesReader(JdbcLoaderBase loader) {
        super(loader);
    }

    @Override
    protected void processResult(ResultSet res, AbstractSchema schema) throws SQLException, XmlReaderException {
        String tableName = res.getString("name");
        loader.setCurrentObject(new GenericColumn(schema.getName(), tableName, DbObjType.TABLE));
        MsTable table = new MsTable(tableName);

        if (res.getBoolean("is_memory_optimized")) {
            table.addOption("MEMORY_OPTIMIZED", "ON");
        }

        if (res.getBoolean("durability")) {
            table.addOption("DURABILITY", res.getString("durability_desc"));
        }

        if (res.getBoolean("data_compression")) {
            table.addOption("DATA_COMPRESSION", res.getString("data_compression_desc"));
        }

        table.setFileStream(res.getString("file_stream"));
        table.setAnsiNulls(res.getBoolean("uses_ansi_nulls"));
        Object isTracked = res.getObject("is_tracked");
        if (isTracked != null) {
            table.setTracked((Boolean)isTracked);
        }

        boolean isTextImage = false;
        for (XmlReader col : XmlReader.readXML(res.getString("cols"))) {
            isTextImage = isTextImage || col.getBoolean("ti");
            table.addColumn(getColumn(col, schema, loader, null));
        }

        if (isTextImage) {
            table.setTextImage(res.getString("text_image"));
        }

        String tablespace = res.getString("space_name");
        if (tablespace != null) {
            StringBuilder sb = new StringBuilder(MsDiffUtils.quoteName(tablespace));

            String partCol = res.getString("part_column");
            if (partCol != null) {
                sb.append('(').append(MsDiffUtils.quoteName(partCol)).append(')');
            }

            table.setTablespace(sb.toString());
        }

        loader.setOwner(table, res.getString("owner"));

        schema.addTable(table);
        loader.setPrivileges(table, XmlReader.readXML(res.getString("acl")));
    }

    // 'MsType type' used only for MsTypesReader processing to extract type depcy
    // from column object since it is temporary
    static AbstractColumn getColumn(XmlReader col, AbstractSchema schema,
            JdbcLoaderBase loader, MsType type) {
        MsColumn column = new MsColumn(col.getString("name"));
        String exp = col.getString("def");
        column.setExpression(exp);
        if (exp == null) {
            boolean isUserDefined = col.getBoolean("ud");
            if (!isUserDefined) {
                column.setCollation(col.getString("cn"));
            }

            column.setType(JdbcLoaderBase.getMsType(column, col.getString("st"), col.getString("type"),
                    isUserDefined, col.getInt("size"), col.getInt("pr"), col.getInt("sc")));
            column.setNullValue(col.getBoolean("nl"));
        }

        column.setSparse(col.getBoolean("sp"));
        column.setRowGuidCol(col.getBoolean("rgc"));
        column.setPersisted(col.getBoolean("ps"));

        String maskingFunction = col.getString("mf");
        if (maskingFunction != null) {
            column.setMaskingFunction("'" + maskingFunction + "'");
        }

        if (col.getBoolean("ii")) {
            column.setIdentity(Integer.toString(col.getInt("s")), Integer.toString(col.getInt("i")));
            column.setNotForRep(col.getBoolean("nfr"));
        }

        String def = col.getString("dv");
        if (def != null) {
            column.setDefaultValue(def);
            column.setDefaultName(col.getString("dn"));
            loader.submitMsAntlrTask(def, p -> p.expression_eof().expression().get(0),
                    ctx -> schema.getDatabase().addAnalysisLauncher(
                            new MsExpressionAnalysisLauncher(type == null ? column : type,
                                    ctx, loader.getCurrentLocation())));
        }
        return column;
    }

    @Override
    protected String getSchemaColumn() {
        return "res.schema_id";
    }

    @Override
    protected void fillQueryBuilder(QueryBuilder builder) {
        addMsPriviligesPart(builder);
        addMsColumnsPart(builder);
        addMsTablespacePart(builder);
        addMsOwnerPart(builder);

        builder
        .column("res.name")
        .column("c.name AS part_column")
        .column("ds.name AS file_stream")
        .column("dsx.name AS text_image")
        .column("res.uses_ansi_nulls")
        .column("res.is_memory_optimized")
        .column("res.durability")
        .column("res.durability_desc")
        .column("sp.data_compression")
        .column("sp.data_compression_desc")
        .column("ctt.is_track_columns_updated_on AS is_tracked")
        .from("sys.tables res WITH (NOLOCK)")
        .join("JOIN sys.indexes ind WITH (NOLOCK) on ind.object_id = res.object_id")
        .join("JOIN sys.partitions sp WITH (NOLOCK) ON sp.object_id = res.object_id AND ind.index_id = sp.index_id AND sp.index_id IN (0,1) AND sp.partition_number = 1")
        .join("LEFT JOIN sys.data_spaces ds WITH (NOLOCK) ON res.filestream_data_space_id = ds.data_space_id")
        .join("LEFT JOIN sys.data_spaces dsx WITH (NOLOCK) ON dsx.data_space_id=res.lob_data_space_id")
        .join("LEFT JOIN sys.index_columns ic WITH (NOLOCK) ON ic.partition_ordinal > 0 AND ic.index_id = ind.index_id and ic.object_id = res.object_id")
        .join("LEFT JOIN sys.columns c WITH (NOLOCK) ON c.object_id = ic.object_id AND c.column_id = ic.column_id")
        .join("LEFT JOIN sys.change_tracking_tables ctt WITH (NOLOCK) ON ctt.object_id = res.object_id")
        .where("res.type = 'U'");
    }

    private void addMsColumnsPart(QueryBuilder builder) {
        String cols = "CROSS APPLY (\n"
                + "  SELECT * FROM (\n"
                + "    SELECT\n"
                + "      c.name,\n"
                + "      c.column_id AS id,\n"
                + "      SCHEMA_NAME(t.schema_id) AS st,\n"
                + "      t.name AS type,\n"
                + "      CASE WHEN c.max_length>=0 AND t.name IN (N'nchar', N'nvarchar') THEN c.max_length/2 ELSE c.max_length END AS size,\n"
                + "      c.precision AS pr,\n"
                + "      c.scale AS sc,\n"
                + "      c.is_sparse AS sp,\n"
                + "      c.collation_name AS cn,\n"
                + "      object_definition(c.default_object_id) AS dv,\n"
                + "      dc.name AS dn,\n"
                + "      c.is_nullable AS nl,\n"
                + "      c.is_identity AS ii,\n"
                + "      ic.seed_value AS s,\n"
                + "      ic.increment_value AS i,\n"
                + "      ic.is_not_for_replication AS nfr,\n"
                + "      c.is_rowguidcol AS rgc,\n"
                + "      cc.is_persisted AS ps,\n"
                + "      t.is_user_defined AS ud,\n"
                + "      mc.masking_function AS mf,\n"
                + "      cc.definition AS def,\n"
                + "      CASE WHEN t.name IN ('GEOMETRY', 'GEOGRAPHY')\n"
                + "        OR TYPE_NAME(t.system_type_id) IN ('TEXT', 'NTEXT','IMAGE' ,'XML')\n"
                + "        OR (TYPE_NAME(t.system_type_id) IN ('VARCHAR', 'NVARCHAR', 'VARBINARY') AND c.max_length = -1)\n"
                + "        THEN 1 ELSE 0 END AS ti\n"
                + "      FROM sys.columns c WITH (NOLOCK)\n"
                + "      JOIN sys.types t WITH (NOLOCK) ON c.user_type_id = t.user_type_id\n"
                + "      LEFT JOIN sys.computed_columns cc WITH (NOLOCK) ON cc.object_id = c.object_id AND c.column_id = cc.column_id\n"
                + "      LEFT JOIN sys.masked_columns mc WITH (NOLOCK) ON mc.object_id = c.object_id AND c.column_id = mc.column_id\n"
                + "      LEFT JOIN sys.identity_columns ic WITH (NOLOCK) ON c.object_id = ic.object_id AND c.column_id = ic.column_id\n"
                + "      LEFT JOIN sys.default_constraints dc WITH (NOLOCK) ON dc.parent_object_id = c.object_id AND c.column_id = dc.parent_column_id\n"
                + "      LEFT JOIN sys.objects so WITH (NOLOCK) ON so.object_id = c.object_id\n"
                + "      WHERE c.object_id = res.object_id\n"
                + "  ) cc ORDER BY cc.id\n"
                + "  FOR XML RAW, ROOT\n"
                + ") cc (cols)";

        builder.column("cc.cols");
        builder.join(cols);
    }

    private void addMsTablespacePart(QueryBuilder builder) {
        String cols = "CROSS APPLY (\n"
                + "  SELECT TOP 1 dsp.name\n"
                + "  FROM sys.indexes ind WITH (NOLOCK)\n"
                + "  LEFT JOIN sys.data_spaces dsp WITH (NOLOCK) on dsp.data_space_id = ind.data_space_id\n"
                + "  WHERE ind.object_id = res.object_id\n"
                + ") tt";

        builder.column("tt.name AS space_name");
        builder.join(cols);
    }
}
