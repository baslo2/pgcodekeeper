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
package ru.taximaxim.codekeeper.core.parsers.antlr.statements.mssql;

import java.util.Arrays;
import java.util.List;

import ru.taximaxim.codekeeper.core.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.core.parsers.antlr.generated.TSQLParser.Column_with_orderContext;
import ru.taximaxim.codekeeper.core.parsers.antlr.generated.TSQLParser.IdContext;
import ru.taximaxim.codekeeper.core.parsers.antlr.generated.TSQLParser.Index_includeContext;
import ru.taximaxim.codekeeper.core.parsers.antlr.generated.TSQLParser.Index_optionContext;
import ru.taximaxim.codekeeper.core.parsers.antlr.generated.TSQLParser.Index_optionsContext;
import ru.taximaxim.codekeeper.core.parsers.antlr.generated.TSQLParser.Index_restContext;
import ru.taximaxim.codekeeper.core.parsers.antlr.generated.TSQLParser.Index_sortContext;
import ru.taximaxim.codekeeper.core.parsers.antlr.generated.TSQLParser.Index_whereContext;
import ru.taximaxim.codekeeper.core.parsers.antlr.generated.TSQLParser.Name_list_in_bracketsContext;
import ru.taximaxim.codekeeper.core.parsers.antlr.generated.TSQLParser.Qualified_nameContext;
import ru.taximaxim.codekeeper.core.parsers.antlr.generated.TSQLParser.Table_constraintContext;
import ru.taximaxim.codekeeper.core.parsers.antlr.generated.TSQLParser.Table_constraint_bodyContext;
import ru.taximaxim.codekeeper.core.parsers.antlr.statements.ParserAbstract;
import ru.taximaxim.codekeeper.core.schema.AbstractConstraint;
import ru.taximaxim.codekeeper.core.schema.AbstractIndex;
import ru.taximaxim.codekeeper.core.schema.GenericColumn;
import ru.taximaxim.codekeeper.core.schema.MsConstraint;
import ru.taximaxim.codekeeper.core.schema.PgDatabase;
import ru.taximaxim.codekeeper.core.schema.PgObjLocation;

public abstract class MsTableAbstract extends ParserAbstract {

    protected MsTableAbstract(PgDatabase db) {
        super(db);
    }

    protected AbstractConstraint getMsConstraint(Table_constraintContext conCtx,
            String schema, String table) {
        String conName = conCtx.id() == null ? "" : conCtx.id().getText();
        AbstractConstraint con = new MsConstraint(conName);

        Table_constraint_bodyContext body = conCtx.table_constraint_body();
        con.setPrimaryKey(body.PRIMARY() != null);
        con.setUnique(body.UNIQUE() != null);

        if (body.REFERENCES() != null) {
            Qualified_nameContext ref = body.qualified_name();
            List<IdContext> ids = Arrays.asList(ref.schema, ref.name);
            String fschema = getSchemaNameSafe(ids);
            String ftable = ref.name.getText();

            PgObjLocation loc = addObjReference(ids, DbObjType.TABLE, null);

            GenericColumn ftableRef = loc.getObj();
            con.setForeignTable(ftableRef);
            con.addDep(ftableRef);

            Name_list_in_bracketsContext columns = body.pk;
            if (columns != null) {
                for (IdContext column : columns.id()) {
                    String col = column.getText();
                    con.addForeignColumn(col);
                    con.addDep(new GenericColumn(fschema, ftable, col, DbObjType.COLUMN));
                }
            }
        } else if (body.column_name_list_with_order() != null) {
            for (Column_with_orderContext column : body.column_name_list_with_order()
                    .column_with_order()) {
                String col = column.id().getText();
                con.addColumn(col);
                con.addDep(new GenericColumn(schema, table, col, DbObjType.COLUMN));
            }
        }

        con.setDefinition(getFullCtxText(conCtx.table_constraint_body()));
        return con;
    }

    protected void parseIndex(Index_restContext rest, AbstractIndex ind, String schema, String table) {
        Index_sortContext sort = rest.index_sort();
        for (Column_with_orderContext col : sort.column_name_list_with_order().column_with_order()) {
            IdContext colCtx = col.id();
            ind.addColumn(colCtx.getText());
            ind.addDep(new GenericColumn(schema, table, colCtx.getText(), DbObjType.COLUMN));
        }

        ind.setDefinition(getFullCtxText(sort));

        Index_includeContext include = rest.index_include();
        if (include != null) {
            for (IdContext col : include.name_list_in_brackets().id()) {
                ind.addInclude(col.getText());
                ind.addDep(new GenericColumn(schema, table, col.getText(), DbObjType.COLUMN));
            }
        }

        Index_whereContext wherePart = rest.index_where();
        if (wherePart != null){
            ind.setWhere(getFullCtxText(wherePart.where));
        }

        Index_optionsContext options = rest.index_options();
        if (options != null) {
            for (Index_optionContext option : options.index_option()) {
                String key = option.key.getText();
                if (!key.equalsIgnoreCase("ONLINE")) {
                    String value = option.index_option_value().getText();
                    ind.addOption(key, value);
                }
            }
        }

        IdContext tablespace = rest.id();
        if (tablespace != null) {
            ind.setTablespace(tablespace.getText());
        }
    }
}
