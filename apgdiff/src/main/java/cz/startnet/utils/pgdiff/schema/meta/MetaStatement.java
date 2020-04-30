package cz.startnet.utils.pgdiff.schema.meta;

import java.io.Serializable;

import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.IStatement;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public class MetaStatement implements IStatement, Serializable {

    private static final long serialVersionUID = -3372437548966681543L;

    private final GenericColumn object;

    private transient MetaStatement parent;

    public MetaStatement(GenericColumn object) {
        this.object = object;
    }

    @Override
    public String getName() {
        return object.getObjName();
    }

    @Override
    public DbObjType getStatementType() {
        return object.type;
    }

    public GenericColumn getObject() {
        return object;
    }

    public void setParent(MetaStatement parent) {
        this.parent = parent;
    }

    @Override
    public MetaStatement getParent() {
        return parent;
    }

    public void addChild(MetaStatement st) {
        // subclasses may override if needed
    }

    public MetaStatement getChild(String name, DbObjType type) {
        // subclasses may override if needed
        return null;
    }

    @Override
    public String getQualifiedName() {
        return object.getQualifiedName();
    }
}
