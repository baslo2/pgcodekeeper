package cz.startnet.utils.pgdiff.schema;

import java.util.Objects;

import cz.startnet.utils.pgdiff.hashers.Hasher;

/**
 * Typed table object
 *
 * @since 4.1.1
 * @author galiev_mr
 *
 */
public class TypedPgTable extends AbstractRegularTable {

    private final String ofType;

    public TypedPgTable(String name, String ofType) {
        super(name);
        this.ofType = ofType;
    }

    @Override
    protected void appendColumns(StringBuilder sbSQL, StringBuilder sbOption) {
        sbSQL.append(" OF ").append(ofType);

        if (!columns.isEmpty()) {
            sbSQL.append(" (\n");

            int start = sbSQL.length();
            for (AbstractColumn column : columns) {
                writeColumn((PgColumn) column, sbSQL, sbOption);
            }

            if (start != sbSQL.length()) {
                sbSQL.setLength(sbSQL.length() - 2);
                sbSQL.append("\n)");
            }
        }
    }

    public String getOfType() {
        return ofType;
    }

    @Override
    protected void compareTableTypes(AbstractPgTable newTable, StringBuilder sb) {
        if (newTable instanceof TypedPgTable) {
            String newType  = ((TypedPgTable)newTable).getOfType();
            if (!Objects.equals(ofType, newType)) {
                sb.append(getAlterTable(true, false))
                .append(" OF ")
                .append(newType)
                .append(';');
            }
        } else {
            sb.append(getAlterTable(true, false))
            .append(" NOT OF")
            .append(';');

            if (newTable instanceof AbstractRegularTable) {
                ((AbstractRegularTable)newTable).convertTable(sb);
            }
        }
    }

    @Override
    protected boolean isColumnsOrderChanged(AbstractTable newTable) {
        return false;
    }

    @Override
    protected AbstractTable getTableCopy() {
        return new TypedPgTable(name, getOfType());
    }

    @Override
    public boolean compare(PgStatement obj) {
        if (obj instanceof TypedPgTable && super.compare(obj)) {
            TypedPgTable table = (TypedPgTable) obj;
            return Objects.equals(ofType, table.getOfType());
        }

        return false;
    }

    @Override
    public void computeHash(Hasher hasher) {
        super.computeHash(hasher);
        hasher.put(ofType);
    }

    @Override
    protected void convertTable(StringBuilder sb) {
        sb.append(getAlterTable(true, false))
        .append(" OF ").append(getOfType()).append(';');
    }
}
