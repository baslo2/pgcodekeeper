package cz.startnet.utils.pgdiff.parsers.antlr.expr;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.loader.SupportedVersion;
import cz.startnet.utils.pgdiff.parsers.antlr.AntlrParser;
import cz.startnet.utils.pgdiff.parsers.antlr.QNameParser;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Data_typeContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Function_args_parserContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.IdentifierContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Schema_qualified_nameContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Schema_qualified_name_nontypeContext;
import cz.startnet.utils.pgdiff.parsers.antlr.statements.ParserAbstract;
import cz.startnet.utils.pgdiff.schema.DbObjNature;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.IFunction;
import cz.startnet.utils.pgdiff.schema.IRelation;
import cz.startnet.utils.pgdiff.schema.ISchema;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgFunction;
import cz.startnet.utils.pgdiff.schema.system.PgSystemStorage;
import ru.taximaxim.codekeeper.apgdiff.Log;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.utils.Pair;

public abstract class AbstractExpr {

    // TODO get postgresql version.
    // Need to get version. I can get it from JdbcLoader(READER),
    // but I can't get it from PgDumpLoader(WRITER).
    protected final PgSystemStorage systemStorage;
    protected final String schema;
    private final AbstractExpr parent;
    private final Set<GenericColumn> depcies;

    protected final PgDatabase db;

    public Set<GenericColumn> getDepcies() {
        return Collections.unmodifiableSet(depcies);
    }

    public AbstractExpr(String schema, PgDatabase db) {
        this.schema = schema;
        parent = null;
        depcies = new LinkedHashSet<>();
        this.db = db;
        systemStorage = PgSystemStorage.getObjectsFromResources(SupportedVersion.VERSION_9_5);
    }

    protected AbstractExpr(AbstractExpr parent) {
        this.schema = parent.schema;
        this.parent = parent;
        depcies = parent.depcies;
        this.db = parent.db;
        this.systemStorage = parent.systemStorage;
    }

    protected List<Pair<String, String>> findCte(String cteName) {
        return parent == null ? null : parent.findCte(cteName);
    }

    protected boolean hasCte(String cteName) {
        return findCte(cteName) != null;
    }

    /**
     * @param schema optional schema qualification of name, may be null
     * @param name alias of the referenced object
     * @param column optional referenced column alias, may be null
     * @return a pair of (Alias, Dealiased name) where Alias is the given name.
     *          Dealiased name can be null if the name is internal to the query
     *          and is not a reference to external table.<br>
     *          null if the name is not found
     */
    protected Entry<String, GenericColumn> findReference(String schema, String name, String column) {
        return parent == null ? null : parent.findReference(schema, name, column);
    }

    /**
     * @param schema optional schema qualification of name, may be null
     * @param name alias of the referenced object
     * @return a pair of (Alias, ColumnsList) where Alias is the given name.
     *          ColumnsList list of columns as pair 'columnName-columnType' of the internal query.<br>
     */
    protected Entry<String, List<Pair<String, String>>> findReferenceComplex(String name) {
        return parent == null ? null : parent.findReferenceComplex(name);
    }

    protected GenericColumn addRelationDepcy(List<IdentifierContext> ids) {
        String schemaName = null;
        IdentifierContext schemaNameCtx = QNameParser.getSchemaNameCtx(ids);
        String relationName = QNameParser.getFirstName(ids);
        if (schemaNameCtx != null) {
            schemaName = schemaNameCtx.getText();
        } else if (db.getSchema(schema).containsRelation(relationName)) {
            schemaName = schema;
        } else {
            for (ISchema s : systemStorage.getSchemas()) {
                if (s.containsRelation(relationName)) {
                    schemaName = s.getName();
                    break;
                }
            }
            if (schemaName == null) {
                Log.log(Log.LOG_WARNING, "Could not find schema for relation: " + relationName);
                schemaName = schema;
            }
        }

        GenericColumn depcy = new GenericColumn(schemaName, relationName, DbObjType.TABLE);
        if (!isSystemSchema(schemaName)) {
            depcies.add(depcy);
        }
        return depcy;
    }

    protected void addTypeDepcy(Data_typeContext type) {
        Schema_qualified_name_nontypeContext typeName = type.predefined_type().schema_qualified_name_nontype();

        if (typeName != null) {
            IdentifierContext qual = typeName.identifier();
            String schemaName = qual == null ? this.schema : qual.getText();

            if (!isSystemSchema(schemaName)) {
                depcies.add(new GenericColumn(schemaName,
                        typeName.identifier_nontype().getText(), DbObjType.TYPE));
            }
        }
    }

    /**
     * @return column with its type
     */
    protected Pair<String, String> addColumnDepcy(Schema_qualified_nameContext qname) {
        List<IdentifierContext> ids = qname.identifier();
        String column = QNameParser.getFirstName(ids);
        String columnType = TypesSetManually.COLUMN;
        Pair<String, String> pair = new Pair<>(column, null);

        // TODO table-less columns are pending full analysis
        if (ids.size() > 1) {
            String schemaName = QNameParser.getThirdName(ids);
            String columnParent = QNameParser.getSecondName(ids);

            Entry<String, GenericColumn> ref = findReference(schemaName, columnParent, column);
            Entry<String, List<Pair<String, String>>> refComplex;
            if (ref != null) {
                GenericColumn referencedTable = ref.getValue();
                if (referencedTable != null) {
                    columnType = addFilteredColumnDepcy(
                            referencedTable.schema, referencedTable.table, column);
                } else if ((refComplex = findReferenceComplex(columnParent)) != null) {
                    columnType = refComplex.getValue().stream()
                            .filter(entry -> column.equals(entry.getKey()))
                            .map(Entry::getValue)
                            .findAny()
                            .orElseGet(() -> {
                                Log.log(Log.LOG_WARNING, "Column " + column +
                                        "not found in complex " + columnParent);
                                return TypesSetManually.COLUMN;
                            });
                } else {
                    Log.log(Log.LOG_WARNING, "Complex not found: " + columnParent);
                }
            } else {
                Log.log(Log.LOG_WARNING, "Unknown column reference: "
                        + schemaName + ' ' + columnParent + ' ' + column);
            }
        }

        pair.setValue(columnType);

        return pair;
    }

    /**
     * Add a dependency only from the column of the user object. Always return its type.
     *
     * @param relation user or system object which contains column 'colName'
     * @param colName dependency from this column will be added
     * @return column type
     */
    private String addFilteredColumnDepcy(String schemaName, String relationName, String colName) {
        IRelation relation = findRelations(schemaName, relationName)
                .findAny()
                .orElse(null);
        if (relation == null) {
            Log.log(Log.LOG_WARNING, "Relation not found: " + schemaName + '.' + relationName);
            return TypesSetManually.COLUMN;
        }
        Optional<String> type = relation.getRelationColumns()
                .filter(col -> col.getFirst().equals(colName))
                .findAny()
                .map(Pair::getSecond);
        if (type.isPresent()) {
            if (DbObjNature.USER == relation.getStatementNature()) {
                depcies.add(new GenericColumn(relation.getContainingSchema().getName(),
                        relation.getName(), colName, DbObjType.COLUMN));
            }
            return type.get();
        } else {
            Log.log(Log.LOG_WARNING, "Column " + colName + " not found in relation "
                    + relation.getName());
            return TypesSetManually.COLUMN;
        }
    }

    protected void addColumnsDepcies(Schema_qualified_nameContext table, List<IdentifierContext> cols) {
        List<IdentifierContext> ids = table.identifier();
        String schemaName = QNameParser.getSchemaName(ids, schema);
        String tableName = QNameParser.getFirstName(ids);
        for (IdentifierContext col : cols) {
            String columnName = col.getText();
            addFilteredColumnDepcy(schemaName, tableName, columnName);
        }
    }

    protected void addColumnsDepcies(String schemaName, String tableOrView, List<Pair<String, String>> cols) {
        String sName = schemaName != null ? schemaName : this.schema;
        for (Pair<String, String> col : cols) {
            String columnName = col.getFirst();
            addFilteredColumnDepcy(sName, tableOrView, columnName);
        }
    }

    protected void addFunctionDepcy(IFunction function) {
        if (DbObjNature.USER == function.getStatementNature()) {
            depcies.add(new GenericColumn(function.getContainingSchema().getName(),
                    function.getName(), DbObjType.FUNCTION));
        }
    }

    /**
     * Use only in contexts where function can be pinpointed only by its name.
     * Such as ::regproc casts.
     */
    protected void addFunctionDepcyNotOverloaded(List<IdentifierContext> ids) {
        IdentifierContext schemaNameCtx = QNameParser.getSchemaNameCtx(ids);
        String schemaName;
        if (schemaNameCtx != null) {
            schemaName = schemaNameCtx.getText();
            if (isSystemSchema(schemaName)) {
                return;
            }
        } else {
            schemaName = schema;
        }

        String functionName = QNameParser.getFirstName(ids);
        PgFunction function = db.getSchema(schemaName).getFunctions().stream()
                .filter(f -> functionName.equals(f.getBareName()))
                .findAny().orElse(null);
        if (function != null) {
            depcies.add(new GenericColumn(schemaName, function.getName(), DbObjType.FUNCTION));
        }
    }

    protected void addFunctionSigDepcy(String signature) {
        SQLParser p = AntlrParser.makeBasicParser(SQLParser.class, signature, "function signature");
        Function_args_parserContext sig = p.function_args_parser();
        List<IdentifierContext> ids = sig.schema_qualified_name().identifier();

        String schemaName = null;
        IdentifierContext schemaNameCtx = QNameParser.getSchemaNameCtx(ids);
        if (schemaNameCtx != null) {
            schemaName = schemaNameCtx.getText();
        } else {
            if (db.getSchema(schema).containsFunction(signature)) {
                schemaName = schema;
            }
            for (ISchema s : systemStorage.getSchemas()) {
                if (s.containsFunction(signature)) {
                    schemaName = s.getName();
                    break;
                }
            }

            if (schemaName == null) {
                Log.log(Log.LOG_WARNING, "Could not find schema for function: " + signature);
                schemaName = schema;
            }
        }

        if (!isSystemSchema(schemaName)) {
            depcies.add(new GenericColumn(schemaName,
                    PgDiffUtils.getQuotedName(QNameParser.getFirstName(ids)) +
                    ParserAbstract.getFullCtxText(sig.function_args()), DbObjType.FUNCTION));
        }
    }

    protected void addSchemaDepcy(List<IdentifierContext> ids) {
        String schemaName = QNameParser.getFirstName(ids);
        if (!isSystemSchema(schemaName)) {
            depcies.add(new GenericColumn(schemaName, DbObjType.SCHEMA));
        }
    }

    protected Stream<IRelation> findRelations(String schemaName, String relationName) {
        Stream<IRelation> foundRelations;
        if (schemaName != null) {
            if (isSystemSchema(schemaName)) {
                foundRelations = systemStorage.getSchema(schemaName).getRelations()
                        .map(r -> (IRelation) r);
            } else {
                foundRelations = db.getSchema(schemaName).getRelations();
            }
        } else {
            foundRelations = Stream.concat(db.getSchema(schema).getRelations(),
                    systemStorage.getSchema(PgSystemStorage.SCHEMA_PG_CATALOG).getRelations());
        }

        return foundRelations.filter(r -> r.getName().equals(relationName));
    }

    protected boolean isSystemSchema(String schemaName) {
        return PgSystemStorage.SCHEMA_PG_CATALOG.equals(schemaName)
                || PgSystemStorage.SCHEMA_INFORMATION_SCHEMA.equals(schemaName);
    }
}