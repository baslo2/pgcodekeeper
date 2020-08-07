package cz.startnet.utils.pgdiff.schema;

import java.util.Objects;

import cz.startnet.utils.pgdiff.hashers.Hasher;

/**
 * Partition foreign table object
 *
 * @since 4.1.1
 * @author galiev_mr
 */
public class PartitionForeignPgTable extends AbstractForeignTable {
    private final String partitionBounds;

    public PartitionForeignPgTable(String name, String serverName, String partitionBounds) {
        super(name, serverName);
        this.partitionBounds = partitionBounds;
    }

    public String getPartitionBounds() {
        return partitionBounds;
    }

    @Override
    protected boolean isNeedRecreate(AbstractTable newTable) {
        return super.isNeedRecreate(newTable)
                || !(Objects.equals(partitionBounds, ((PartitionForeignPgTable)newTable).getPartitionBounds()))
                || !inherits.equals(((AbstractPgTable)newTable).inherits);
    }

    @Override
    protected void appendColumns(StringBuilder sbSQL, StringBuilder sbOption) {
        sbSQL.append(" PARTITION OF ").append(inherits.get(0).getQualifiedName());

        if (!columns.isEmpty()) {
            sbSQL.append(" (\n");

            int start = sbSQL.length();
            for (AbstractColumn column : columns) {
                writeColumn((PgColumn) column, sbSQL, sbOption);
            }

            if (start != sbSQL.length()) {
                sbSQL.setLength(sbSQL.length() - 2);
                sbSQL.append("\n)");
            } else {
                sbSQL.setLength(sbSQL.length() - 3);
            }
        }

        sbSQL.append('\n');
        sbSQL.append(partitionBounds);
    }

    @Override
    protected void appendInherit(StringBuilder sbSQL) {
        // PgTable.inherits stores PARTITION OF table in this implementation
    }

    @Override
    protected AbstractTable getTableCopy() {
        return new PartitionForeignPgTable(name, serverName, partitionBounds);
    }

    @Override
    public boolean compare(PgStatement obj) {
        if (obj instanceof PartitionForeignPgTable && super.compare(obj)) {
            PartitionForeignPgTable table = (PartitionForeignPgTable) obj;
            return Objects.equals(partitionBounds, table.getPartitionBounds());
        }

        return false;
    }

    @Override
    public void computeHash(Hasher hasher) {
        super.computeHash(hasher);
        hasher.put(partitionBounds);
    }
}