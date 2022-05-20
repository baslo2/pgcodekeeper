package cz.startnet.utils.pgdiff.loader.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.antlr.v4.runtime.CommonTokenStream;

import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.loader.JdbcQueries;
import cz.startnet.utils.pgdiff.loader.SupportedVersion;
import cz.startnet.utils.pgdiff.parsers.antlr.statements.AlterTable;
import cz.startnet.utils.pgdiff.schema.AbstractConstraint;
import cz.startnet.utils.pgdiff.schema.AbstractSchema;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.PgConstraint;
import cz.startnet.utils.pgdiff.schema.PgStatementContainer;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.utils.Pair;

public class ConstraintsReader extends JdbcReader {

    private static final String ADD_CONSTRAINT = "ALTER TABLE noname ADD CONSTRAINT noname ";

    public ConstraintsReader(JdbcLoaderBase loader) {
        super(JdbcQueries.QUERY_CONSTRAINTS, loader);
    }

    @Override
    protected void processResult(ResultSet result, AbstractSchema schema) throws SQLException {
        if (SupportedVersion.VERSION_11.isLE(loader.version) && result.getInt("conparentid") != 0) {
            return;
        }

        PgStatementContainer cont = schema.getStatementContainer(result.getString(CLASS_RELNAME));
        if (cont != null) {
            cont.addConstraint(getConstraint(result, schema, cont.getName()));
        }
    }

    private AbstractConstraint getConstraint(ResultSet res, AbstractSchema schema, String tableName)
            throws SQLException {
        String schemaName = schema.getName();

        String constraintName = res.getString("conname");
        loader.setCurrentObject(new GenericColumn(schemaName, tableName, constraintName, DbObjType.CONSTRAINT));
        PgConstraint c = new PgConstraint(constraintName);

        String definition = res.getString("definition");
        checkObjectValidity(definition, DbObjType.CONSTRAINT, constraintName);
        String tablespace = res.getString("spcname");
        loader.submitAntlrTask(ADD_CONSTRAINT + definition + ';',
                p -> new Pair<>(p.sql().statement(0).schema_statement().schema_alter()
                        .alter_table_statement().table_action(0), (CommonTokenStream) p.getTokenStream()),
                pair -> new AlterTable(null, schema.getDatabase(), tablespace, pair.getSecond()).parseAlterTableConstraint(
                        pair.getFirst(), c, schemaName, tableName, loader.getCurrentLocation()));
        loader.setAuthor(c, res);

        String comment = res.getString("description");
        if (comment != null && !comment.isEmpty()) {
            c.setComment(loader.args, PgDiffUtils.quoteString(comment));
        }
        return c;
    }

    @Override
    protected String getClassId() {
        return "pg_constraint";
    }
}
