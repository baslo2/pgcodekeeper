package ru.taximaxim.codekeeper.core.schema;

/**
 * The superclass for general pgsql statement with search_path. That is any but
 * SCHEMA and EXTENSION.
 *
 * @author Alexander Levsha
 */
public abstract class PgStatementWithSearchPath extends PgStatement implements ISearchPath {

    public PgStatementWithSearchPath(String name) {
        super(name);
    }

    @Override
    public abstract AbstractSchema getContainingSchema();

    @Override
    public PgDatabase getDatabase() {
        return (PgDatabase)getContainingSchema().getParent();
    }
}
