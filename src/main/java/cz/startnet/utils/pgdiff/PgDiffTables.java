/**
 * Copyright 2006 StartNet s.r.o.
 *
 * Distributed under MIT license
 */
package cz.startnet.utils.pgdiff;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cz.startnet.utils.pgdiff.schema.PgColumn;
import cz.startnet.utils.pgdiff.schema.PgColumnUtils;
import cz.startnet.utils.pgdiff.schema.PgForeignKey;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import cz.startnet.utils.pgdiff.schema.PgTable;
import cz.startnet.utils.pgdiff.schema.PgView;

/**
 * Diffs tables.
 *
 * @author fordfrog
 */
public class PgDiffTables {

    /**
     * Outputs statements for creation of clusters.
     *
     * @param writer           writer the output should be written to
     * @param oldSchema        original schema
     * @param newSchema        new schema
     * @param searchPathHelper search path helper
     */
    public static void dropClusters(final PrintWriter writer,
            final PgSchema oldSchema, final PgSchema newSchema,
            final SearchPathHelper searchPathHelper) {
        for (final PgTable newTable : newSchema.getTables()) {
            final PgTable oldTable;

            if (oldSchema == null) {
                oldTable = null;
            } else {
                oldTable = oldSchema.getTable(newTable.getName());
            }

            final String oldCluster;

            if (oldTable == null) {
                oldCluster = null;
            } else {
                oldCluster = oldTable.getClusterIndexName();
            }

            final String newCluster = newTable.getClusterIndexName();

            if (oldCluster != null && newCluster == null
                    && newTable.containsIndex(oldCluster)) {
                searchPathHelper.outputSearchPath(writer);
                writer.println();
                writer.print("ALTER TABLE ");
                writer.print(PgDiffUtils.getQuotedName(newTable.getName()));
                writer.println(" SET WITHOUT CLUSTER;");
            }
        }
    }

    /**
     * Outputs statements for dropping of clusters.
     *
     * @param writer           writer the output should be written to
     * @param oldSchema        original schema
     * @param newSchema        new schema
     * @param searchPathHelper search path helper
     */
    public static void createClusters(final PrintWriter writer,
            final PgSchema oldSchema, final PgSchema newSchema,
            final SearchPathHelper searchPathHelper) {
        for (final PgTable newTable : newSchema.getTables()) {
            final PgTable oldTable;

            if (oldSchema == null) {
                oldTable = null;
            } else {
                oldTable = oldSchema.getTable(newTable.getName());
            }

            final String oldCluster;

            if (oldTable == null) {
                oldCluster = null;
            } else {
                oldCluster = oldTable.getClusterIndexName();
            }

            final String newCluster = newTable.getClusterIndexName();

            if ((oldCluster == null && newCluster != null)
                    || (oldCluster != null && newCluster != null
                    && !newCluster.equals(oldCluster))) {
                searchPathHelper.outputSearchPath(writer);
                writer.println();
                writer.print("ALTER TABLE ");
                writer.print(PgDiffUtils.getQuotedName(newTable.getName()));
                writer.print(" CLUSTER ON ");
                writer.print(PgDiffUtils.getQuotedName(newCluster));
                writer.println(';');
            }
        }
    }

    /**
     * Outputs statements for altering tables.
     *
     * @param writer           writer the output should be written to
     * @param arguments        object containing arguments settings
     * @param oldSchema        original schema
     * @param newSchema        new schema
     * @param searchPathHelper search path helper
     */
    public static void alterTables(final PrintWriter writer,
            final PgDiffArguments arguments, final PgSchema oldSchema,
            final PgSchema newSchema, final SearchPathHelper searchPathHelper) {
        for (final PgTable newTable : newSchema.getTables()) {
            if (oldSchema == null
                    || !oldSchema.containsTable(newTable.getName())) {
                continue;
            }

            final PgTable oldTable = oldSchema.getTable(newTable.getName());
            
            updateTableColumns(
                    writer, arguments, oldTable, newTable, searchPathHelper);
            checkWithOIDS(writer, oldTable, newTable, searchPathHelper);
            checkInherits(writer, oldTable, newTable, searchPathHelper);
            checkTablespace(writer, oldTable, newTable, searchPathHelper);
            addAlterStatistics(writer, oldTable, newTable, searchPathHelper);
            addAlterStorage(writer, oldTable, newTable, searchPathHelper);
            alterComments(writer, oldTable, newTable, searchPathHelper);
        }
    }

    /**
     * Generate the needed alter table xxx set statistics when needed.
     *
     * @param writer           writer the output should be written to
     * @param oldTable         original table
     * @param newTable         new table
     * @param searchPathHelper search path helper
     */
    private static void addAlterStatistics(final PrintWriter writer,
            final PgTable oldTable, final PgTable newTable,
            final SearchPathHelper searchPathHelper) {
        final Map<String, Integer> stats = new HashMap<String, Integer>();

        for (final PgColumn newColumn : newTable.getColumns()) {
            final PgColumn oldColumn = oldTable.getColumn(newColumn.getName());

            if (oldColumn != null) {
                final Integer oldStat = oldColumn.getStatistics();
                final Integer newStat = newColumn.getStatistics();
                Integer newStatValue = null;

                if (newStat != null && (oldStat == null
                        || !newStat.equals(oldStat))) {
                    newStatValue = newStat;
                } else if (oldStat != null && newStat == null) {
                    newStatValue = Integer.valueOf(-1);
                }

                if (newStatValue != null) {
                    stats.put(newColumn.getName(), newStatValue);
                }
            }
        }

        for (final Map.Entry<String, Integer> entry : stats.entrySet()) {
            searchPathHelper.outputSearchPath(writer);
            writer.println();
            writer.print("ALTER TABLE ONLY ");
            writer.print(PgDiffUtils.getQuotedName(newTable.getName()));
            writer.print(" ALTER COLUMN ");
            writer.print(PgDiffUtils.getQuotedName(entry.getKey()));
            writer.print(" SET STATISTICS ");
            writer.print(entry.getValue());
            writer.println(';');
        }
    }

    /**
     * Generate the needed alter table xxx set storage when needed.
     *
     * @param writer           writer the output should be written to
     * @param oldTable         original table
     * @param newTable         new table
     * @param searchPathHelper search path helper
     */
    private static void addAlterStorage(final PrintWriter writer,
            final PgTable oldTable, final PgTable newTable,
            final SearchPathHelper searchPathHelper) {
        for (final PgColumn newColumn : newTable.getColumns()) {
            final PgColumn oldColumn = oldTable.getColumn(newColumn.getName());
            final String oldStorage = (oldColumn == null
                    || oldColumn.getStorage() == null
                    || oldColumn.getStorage().isEmpty()) ? null
                    : oldColumn.getStorage();
            final String newStorage = (newColumn.getStorage() == null
                    || newColumn.getStorage().isEmpty()) ? null
                    : newColumn.getStorage();

            if (newStorage == null && oldStorage != null) {
                searchPathHelper.outputSearchPath(writer);
                writer.println();
                writer.println(MessageFormat.format(Resources.getString(
                        "WarningUnableToDetermineStorageType"),
                        newTable.getName() + '.' + newColumn.getName()));

                continue;
            }

            if (newStorage == null || newStorage.equalsIgnoreCase(oldStorage)) {
                continue;
            }

            searchPathHelper.outputSearchPath(writer);
            writer.println();
            writer.print("ALTER TABLE ONLY ");
            writer.print(PgDiffUtils.getQuotedName(newTable.getName()));
            writer.print(" ALTER COLUMN ");
            writer.print(PgDiffUtils.getQuotedName(newColumn.getName()));
            writer.print(" SET STORAGE ");
            writer.print(newStorage);
            writer.print(';');
        }
    }

    /**
     * Adds statements for creation of new columns to the list of statements.
     *
     * @param statements          list of statements
     * @param arguments           object containing arguments settings
     * @param oldTable            original table
     * @param newTable            new table
     * @param dropDefaultsColumns list for storing columns for which default
     *                            value should be dropped
     */
    private static void addCreateTableColumns(final List<String> statements,
            final PgDiffArguments arguments, final PgTable oldTable,
            final PgTable newTable, final List<PgColumn> dropDefaultsColumns) {
        for (final PgColumn column : newTable.getColumns()) {
            if (!oldTable.containsColumn(column.getName())) {
                statements.add("\tADD COLUMN "
                        + column.getFullDefinition(arguments.isAddDefaults()));

                if (arguments.isAddDefaults() && !column.getNullValue()
                        && (column.getDefaultValue() == null
                        || column.getDefaultValue().isEmpty())) {
                    dropDefaultsColumns.add(column);
                }
            }
        }
    }

    /**
     * Adds statements for removal of columns to the list of statements.
     * @param statementsToDrop 
     *
     * @param statements list of statements
     * @param oldTable   original table
     * @param newTable   new table
     */
    private static void addDropTableColumns(Map<PgStatement, String> statementsToDrop,
            final List<String> statements,
            final PgTable oldTable, final PgTable newTable) {
        for (final PgColumn column : oldTable.getColumns()) {
            if (!newTable.containsColumn(column.getName())) {
                statements.add("\tDROP COLUMN "
                        + PgDiffUtils.getQuotedName(column.getName()));
                
                // begin depcy
                // get dependent PgViews of this column, add them in pairs 
                // <dependant, reason> to the map if dependent PgView is not 
                // contained in there
                Set<PgStatement> dependants = new LinkedHashSet<>(10);
                for (PgStatement dependant : PgDiff.getDependantsSet(column, dependants)){
                    if ((dependant instanceof PgView || dependant instanceof PgForeignKey) 
                            && !statementsToDrop.containsKey(dependant)){
                        String reason = "column " + column.getName() + " of table " 
                                + oldTable.getName() + " is dropped";
                        statementsToDrop.put(dependant, reason);
                    }
                }// end depcy
            }
        }
    }

    /**
     * Adds statements for modification of columns to the list of statements.
     * @param statementsToDrop 
     *
     * @param statements          list of statements
     * @param arguments           object containing arguments settings
     * @param oldTable            original table
     * @param newTable            new table
     * @param dropDefaultsColumns list for storing columns for which default
     *                            value should be dropped
     */
    private static void addModifyTableColumns(Map<PgStatement, String> statementsToDrop, 
            final List<String> statements,
            final PgDiffArguments arguments, final PgTable oldTable,
            final PgTable newTable, final List<PgColumn> dropDefaultsColumns) {
        for (final PgColumn newColumn : newTable.getColumns()) {
            if (!oldTable.containsColumn(newColumn.getName())) {
                continue;
            }

            final PgColumn oldColumn = oldTable.getColumn(newColumn.getName());
            final String newColumnName =
                    PgDiffUtils.getQuotedName(newColumn.getName());

            if (!oldColumn.getType().equals(newColumn.getType())) {
                
                // begin depcy
                // get dependent PgViews of this column, add them in pairs 
                // <dependant, reason> to the map if dependent PgView is not 
                // contained in there
                Set<PgStatement> dependants = new LinkedHashSet<PgStatement>(10);
                for (PgStatement dependant : PgDiff.getDependantsSet(oldColumn, dependants)){
                    if ((dependant instanceof PgView || dependant instanceof PgForeignKey)
                            && !statementsToDrop.containsKey(dependant)){
                        String reason = "column " + oldColumn.getName() + " of table " 
                                    + oldTable.getName() + " is altered (type changed)";
                        statementsToDrop.put(dependant, reason);
                    }
                } // end depcy
                
                statements.add("\tALTER COLUMN " + newColumnName + " TYPE "
                        + newColumn.getType() + " /* "
                        + MessageFormat.format(
                        Resources.getString("TypeParameterChange"),
                        newTable.getName(), oldColumn.getType(),
                        newColumn.getType()) + " */");
            }

            final String oldDefault = (oldColumn.getDefaultValue() == null) ? ""
                    : oldColumn.getDefaultValue();
            final String newDefault = (newColumn.getDefaultValue() == null) ? ""
                    : newColumn.getDefaultValue();

            if (!oldDefault.equals(newDefault)) {
                if (newDefault.length() == 0) {
                    statements.add("\tALTER COLUMN " + newColumnName
                            + " DROP DEFAULT");
                } else {
                    statements.add("\tALTER COLUMN " + newColumnName
                            + " SET DEFAULT " + newDefault);
                }
            }

            if (oldColumn.getNullValue() != newColumn.getNullValue()) {
                if (newColumn.getNullValue()) {
                    statements.add("\tALTER COLUMN " + newColumnName
                            + " DROP NOT NULL");
                } else {
                    if (arguments.isAddDefaults()) {
                        final String defaultValue =
                                PgColumnUtils.getDefaultValue(
                                newColumn.getType());

                        if (defaultValue != null) {
                            statements.add("\tALTER COLUMN " + newColumnName
                                    + " SET DEFAULT " + defaultValue);
                            dropDefaultsColumns.add(newColumn);
                        }
                    }

                    statements.add("\tALTER COLUMN " + newColumnName
                            + " SET NOT NULL");
                }
            }
        }
    }

    /**
     * Checks whether there is a discrepancy in INHERITS for original and new
     * table.
     *
     * @param writer           writer the output should be written to
     * @param oldTable         original table
     * @param newTable         new table
     * @param searchPathHelper search path helper
     */
    private static void checkInherits(final PrintWriter writer,
            final PgTable oldTable, final PgTable newTable,
            final SearchPathHelper searchPathHelper) {
        for (final String tableName : oldTable.getInherits()) {
            if (!newTable.getInherits().contains(tableName)) {
                searchPathHelper.outputSearchPath(writer);
                writer.println();
                writer.println("ALTER TABLE "
                        + PgDiffUtils.getQuotedName(newTable.getName()));
                writer.println("\tNO INHERIT "
                        + PgDiffUtils.getQuotedName(tableName) + ';');
            }
        }

        for (final String tableName : newTable.getInherits()) {
            if (!oldTable.getInherits().contains(tableName)) {
                searchPathHelper.outputSearchPath(writer);
                writer.println();
                writer.println("ALTER TABLE "
                        + PgDiffUtils.getQuotedName(newTable.getName()));
                writer.println("\tINHERIT "
                        + PgDiffUtils.getQuotedName(tableName) + ';');
            }
        }
    }

    /**
     * Checks whether OIDS are dropped from the new table. There is no way to
     * add OIDS to existing table so we do not create SQL statement for addition
     * of OIDS but we issue warning.
     *
     * @param writer           writer the output should be written to
     * @param oldTable         original table
     * @param newTable         new table
     * @param searchPathHelper search path helper
     */
    private static void checkWithOIDS(final PrintWriter writer,
            final PgTable oldTable, final PgTable newTable,
            final SearchPathHelper searchPathHelper) {
        if (oldTable.getWith() == null && newTable.getWith() == null
                || oldTable.getWith() != null
                && oldTable.getWith().equals(newTable.getWith())) {
            return;
        }

        searchPathHelper.outputSearchPath(writer);
        writer.println();
        writer.println("ALTER TABLE "
                + PgDiffUtils.getQuotedName(newTable.getName()));

        if (newTable.getWith() == null
                || "OIDS=false".equalsIgnoreCase(newTable.getWith())) {
            writer.println("\tSET WITHOUT OIDS;");
        } else if ("OIDS".equalsIgnoreCase(newTable.getWith())
                || "OIDS=true".equalsIgnoreCase(newTable.getWith())) {
            writer.println("\tSET WITH OIDS;");
        } else {
            writer.println("\tSET " + newTable.getWith() + ";");
        }
    }

    /**
     * Checks tablespace modification.
     *
     * @param writer           writer
     * @param oldTable         old table
     * @param newTable         new table
     * @param searchPathHelper search path helper
     */
    private static void checkTablespace(final PrintWriter writer,
            final PgTable oldTable, final PgTable newTable,
            final SearchPathHelper searchPathHelper) {
        if (oldTable.getTablespace() == null && newTable.getTablespace() == null
                || oldTable.getTablespace() != null
                && oldTable.getTablespace().equals(newTable.getTablespace())) {
            return;
        }

        searchPathHelper.outputSearchPath(writer);
        writer.println();
        writer.println("ALTER TABLE "
                + PgDiffUtils.getQuotedName(newTable.getName()));
        writer.println("\tTABLESPACE " + newTable.getTablespace() + ';');
    }

    /**
     * Outputs statements for creation of new tables.
     *
     * @param writer           writer the output should be written to
     * @param oldSchema        original schema
     * @param newSchema        new schema
     * @param searchPathHelper search path helper
     */
    public static void createTables(final PrintWriter writer,
            final PgSchema oldSchema, final PgSchema newSchema,
            final SearchPathHelper searchPathHelper) {
        for (final PgTable table : newSchema.getTables()) {
            if (oldSchema == null || !oldSchema.containsTable(table.getName())) {
                searchPathHelper.outputSearchPath(writer);
                writer.println();
                writer.println(table.getCreationSQL());
            }
        }
    }

    /**
     * Outputs statements for dropping tables.
     *
     * @param writer           writer the output should be written to
     * @param oldSchema        original schema
     * @param newSchema        new schema
     * @param searchPathHelper search path helper
     */
    public static void dropTables(final PrintWriter writer,
            final PgSchema oldSchema, final PgSchema newSchema,
            final SearchPathHelper searchPathHelper) {
        
        if (oldSchema == null) {
            return;
        }
        
        for (final PgTable table : oldSchema.getTables()) {
            if (!newSchema.containsTable(table.getName())) {
                if (!PgDiff.isFullSelection(table)){
                    // TODO Обсудить с Саушкиным, нужен ли этот коммент
                    writer.println("-- table \"" + table.getName() + "\" was not "
                            + "dropped because it was not selected entirely");
                    writer.println();
                    continue;
                }
                // check all dependants, drop them if instanceof PgView
                // output search path, if necessary
                Set<PgStatement> dependantsSet = new LinkedHashSet<>(10);
                PgDiff.getDependantsSet(table, dependantsSet);
                // wrap Set into array for reverse iteration
                Object[] dependants = dependantsSet.toArray();
                
                for (int i = dependants.length - 1; i >= 0; i--){
                    PgStatement depnt = (PgStatement) dependants[i];
                    
                    if (depnt instanceof PgView) {
                        tempSwitchSearchPath(
                                PgDiffUtils.getQuotedName(depnt.getParent().getName()),
                                searchPathHelper, writer);
                    } else if (depnt instanceof PgForeignKey) {
                        if (depnt.getParent().compare(table)
                                && depnt.getParent().getParent().compare(table.getParent())) {
                            // if this fkey is a direct descendant of the table we're dropping
                            // skip it, postgres handles direct descendants itself
                            continue;
                        }
                        
                        tempSwitchSearchPath(
                                PgDiffUtils.getQuotedName(depnt.getParent().getParent().getName()),
                                searchPathHelper, writer);
                    }
                    
                    writer.println();
                    writer.println("-- DEPCY: Dropping an object that depends"
                            + " on the table we are about to drop: " + table.getName());
                    writer.println(depnt.getDropSQL());
                }
                
                searchPathHelper.outputSearchPath(writer);
                writer.println();
                writer.println(table.getDropSQL());
                
                // TODO remove, make optional or mark as a todo for DB programmer
                // futile attempt to restore a view that depends on the dropped table
                for (Object depnt : dependants){
                    if (depnt instanceof PgView){
                        PgView view = (PgView) depnt;
                        tempSwitchSearchPath(
                                PgDiffUtils.getQuotedName(view.getParent().getName()),
                                searchPathHelper, writer);
                        writer.println();
                        writer.println("-- DEPCY: Following view depends on the dropped table " 
                                + table.getName());
                        writer.println(view.getCreationSQL());
                    }
                }
            }
        }
    }
    
    // TODO refactor, put this elsewhere?
    private static void tempSwitchSearchPath(String switchTo, 
            final SearchPathHelper searchPathHelper, final PrintWriter writer){
        
        if (searchPathHelper.getWasOutput() == false ||
                !searchPathHelper.getSchemaName().equals(switchTo)){
            new SearchPathHelper(switchTo).outputSearchPath(writer);
            
            searchPathHelper.setWasOutput(false);
        }
    }
    
    /**
     * Outputs statements for addition, removal and modifications of table
     * columns.
     *
     * @param writer           writer the output should be written to
     * @param arguments        object containing arguments settings
     * @param oldTable         original table
     * @param newTable         new table
     * @param searchPathHelper search path helper
     */
    private static void updateTableColumns(final PrintWriter writer,
            final PgDiffArguments arguments, final PgTable oldTable,
            final PgTable newTable, final SearchPathHelper searchPathHelper) {
        final List<String> statements = new ArrayList<>();
        final List<PgColumn> dropDefaultsColumns = new ArrayList<>();
        
        // ordered pairs of <statementToDrop, reasonOfDrop>
        Map<PgStatement, String> statementsToDrop = new LinkedHashMap<>(10);
        
        addDropTableColumns(statementsToDrop, statements, oldTable, newTable);
        addCreateTableColumns( 
                statements, arguments, oldTable, newTable, dropDefaultsColumns);
        addModifyTableColumns(statementsToDrop, 
                statements, arguments, oldTable, newTable, dropDefaultsColumns);

        // write dependent PgViews/PgForeignKey drop sql in REVERSE order before table altering
        Set<Entry<PgStatement, String>> dependants = statementsToDrop.entrySet();
        // wrap Set into array for reverse iteration
        Object[] dependantsArray = dependants.toArray();
        
        for (int i = dependantsArray.length - 1; i >= 0; i--){
            @SuppressWarnings("unchecked")
            PgStatement depnt = ((Entry<PgStatement, String>) dependantsArray[i]).getKey();

            @SuppressWarnings("unchecked")
            String reason = ((Entry<PgStatement, String>) dependantsArray[i]).getValue();
            
            if (depnt instanceof PgView){
                tempSwitchSearchPath(PgDiffUtils.getQuotedName(
                        depnt.getParent().getName()), searchPathHelper, writer);
            }else if (depnt instanceof PgForeignKey){
                tempSwitchSearchPath(PgDiffUtils.getQuotedName(
                        depnt.getParent().getParent().getName()), searchPathHelper, writer);
            }

            writer.println();
            writer.println("-- DEPCY: dropping dependant object: " + reason);
            writer.println(depnt.getDropSQL());
        }// end write dependent PgViews/PgForeignKey drop sql code before table altering
        
        if (!statements.isEmpty()) {
            final String quotedTableName =
                    PgDiffUtils.getQuotedName(newTable.getName());
            searchPathHelper.outputSearchPath(writer);
            writer.println();
            writer.println("ALTER TABLE " + quotedTableName);

            for (int i = 0; i < statements.size(); i++) {
                writer.print(statements.get(i));
                writer.println((i + 1) < statements.size() ? "," : ";");
            }

            if (!dropDefaultsColumns.isEmpty()) {
                writer.println();
                writer.println("ALTER TABLE " + quotedTableName);

                for (int i = 0; i < dropDefaultsColumns.size(); i++) {
                    writer.print("\tALTER COLUMN ");
                    writer.print(PgDiffUtils.getQuotedName(
                            dropDefaultsColumns.get(i).getName()));
                    writer.print(" DROP DEFAULT");
                    writer.println(
                            (i + 1) < dropDefaultsColumns.size() ? "," : ";");
                }
            }
        }
        
        // write dependent PgViews create sql code after table altering
        for (Object dependant : dependants){
            @SuppressWarnings("unchecked")
            PgStatement depnt = ((Entry<PgStatement, String>) dependant).getKey();

            @SuppressWarnings("unchecked")
            String reason = ((Entry<PgStatement, String>) dependant).getValue();
            
            if (depnt instanceof PgView){
                PgView view = (PgView) depnt;
                tempSwitchSearchPath(
                        PgDiffUtils.getQuotedName(view.getParent().getName()),
                        searchPathHelper, writer);
                writer.println();
                writer.println("-- DEPCY: recreating dropped dependant object: "
                        + reason);
                writer.println(view.getCreationSQL());
            }
        }// end write dependent PgViews create sql code after table altering
    }

    /**
     * Outputs statements for tables and columns for which comments have
     * changed.
     *
     * @param writer           writer
     * @param oldTable         old table
     * @param newTable         new table
     * @param searchPathHelper search path helper
     */
    private static void alterComments(final PrintWriter writer,
            final PgTable oldTable, final PgTable newTable,
            final SearchPathHelper searchPathHelper) {
        if (oldTable.getComment() == null
                && newTable.getComment() != null
                || oldTable.getComment() != null
                && newTable.getComment() != null
                && !oldTable.getComment().equals(newTable.getComment())) {
            searchPathHelper.outputSearchPath(writer);
            writer.println();
            writer.print("COMMENT ON TABLE ");
            writer.print(PgDiffUtils.getQuotedName(newTable.getName()));
            writer.print(" IS ");
            writer.print(newTable.getComment());
            writer.println(';');
        } else if (oldTable.getComment() != null
                && newTable.getComment() == null) {
            searchPathHelper.outputSearchPath(writer);
            writer.println();
            writer.print("COMMENT ON TABLE ");
            writer.print(PgDiffUtils.getQuotedName(newTable.getName()));
            writer.println(" IS NULL;");
        }

        for (final PgColumn newColumn : newTable.getColumns()) {
            final PgColumn oldColumn = oldTable.getColumn(newColumn.getName());
            final String oldComment =
                    oldColumn == null ? null : oldColumn.getComment();
            final String newComment = newColumn.getComment();

            if (newComment != null && (oldComment == null ? newComment != null
                    : !oldComment.equals(newComment))) {
                searchPathHelper.outputSearchPath(writer);
                writer.println();
                writer.print("COMMENT ON COLUMN ");
                writer.print(PgDiffUtils.getQuotedName(newTable.getName()));
                writer.print('.');
                writer.print(PgDiffUtils.getQuotedName(newColumn.getName()));
                writer.print(" IS ");
                writer.print(newColumn.getComment());
                writer.println(';');
            } else if (oldComment != null && newComment == null) {
                searchPathHelper.outputSearchPath(writer);
                writer.println();
                writer.print("COMMENT ON COLUMN ");
                writer.print(PgDiffUtils.getQuotedName(newTable.getName()));
                writer.print('.');
                writer.print(PgDiffUtils.getQuotedName(newColumn.getName()));
                writer.println(" IS NULL;");
            }
        }
    }

    /**
     * Creates a new instance of PgDiffTables.
     */
    private PgDiffTables() {
    }
}
