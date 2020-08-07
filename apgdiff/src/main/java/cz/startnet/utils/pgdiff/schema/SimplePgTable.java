package cz.startnet.utils.pgdiff.schema;

/**
 * Simple table object
 *
 * @since 4.1.1
 * @author galiev_mr
 *
 */
public class SimplePgTable extends AbstractRegularTable {

    public SimplePgTable(String name) {
        super(name);
    }

    @Override
    protected void appendColumns(StringBuilder sbSQL, StringBuilder sbOption) {
        sbSQL.append(" (\n");

        int start = sbSQL.length();
        for (AbstractColumn column : columns) {
            writeColumn((PgColumn) column, sbSQL, sbOption);
        }

        if (start != sbSQL.length()) {
            sbSQL.setLength(sbSQL.length() - 2);
            sbSQL.append('\n');
        }

        sbSQL.append(')');
    }

    @Override
    protected AbstractTable getTableCopy() {
        return new SimplePgTable(name);
    }

    @Override
    protected void compareTableTypes(AbstractPgTable newTable, StringBuilder sb) {
        if (newTable instanceof AbstractRegularTable) {
            ((AbstractRegularTable)newTable).convertTable(sb);
        }
    }

    @Override
    protected void convertTable(StringBuilder sb) {
        // no implements
    }
}