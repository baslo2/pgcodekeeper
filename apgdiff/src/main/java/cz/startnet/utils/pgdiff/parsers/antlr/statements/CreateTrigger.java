package cz.startnet.utils.pgdiff.parsers.antlr.statements;

import org.antlr.v4.runtime.tree.ParseTreeWalker;

import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Create_trigger_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Names_referencesContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Schema_qualified_nameContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.When_triggerContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParserBaseListener;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.GenericColumn.ViewReference;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import cz.startnet.utils.pgdiff.schema.PgTable;
import cz.startnet.utils.pgdiff.schema.PgTrigger;
import cz.startnet.utils.pgdiff.schema.PgView;
import ru.taximaxim.codekeeper.apgdiff.Log;

public class CreateTrigger extends ParserAbstract {
    private final Create_trigger_statementContext ctx;
    public CreateTrigger(Create_trigger_statementContext ctx, PgDatabase db) {
        super(db);
        this.ctx = ctx;
    }

    @Override
    public PgStatement getObject() {
        String name = getName(ctx.name);
        String schemaName =getSchemaName(ctx.name);
        if (schemaName==null) {
            schemaName = getDefSchemaName();
        }
        PgTrigger trigger = new PgTrigger(name, getFullCtxText(ctx.getParent()));
        trigger.setTableName(ctx.tabl_name.getText());
        trigger.setBefore(ctx.before_true != null);
        if (ctx.ROW() != null) {
            trigger.setForEachRow(true);
        }
        if (ctx.STATEMENT() != null) {
            trigger.setForEachRow(false);
        }
        trigger.setOnDelete(ctx.delete_true != null);
        trigger.setOnInsert(ctx.insert_true!= null);
        trigger.setOnUpdate(ctx.update_true != null);
        trigger.setOnTruncate(ctx.truncate_true != null);
        trigger.setFunction(getFullCtxText(ctx.func_name), getFullCtxText(ctx.func_name.name) + "()");



        GenericColumn gc = new GenericColumn(
                schemaName,
                getFullCtxText(ctx.func_name),
                null);
        gc.setType(ViewReference.FUNCTION);
        trigger.addDep(gc);



        for (Names_referencesContext column : ctx.names_references()) {
            String colName;
            for (Schema_qualified_nameContext nameCol : column.name){
                colName = getName(nameCol);
                trigger.addUpdateColumn(colName);
            }
        }
        WhenListener whenListener = new WhenListener();
        ParseTreeWalker.DEFAULT.walk(whenListener, ctx);
        trigger.setWhen(whenListener.getWhen());

        if (db.getSchema(schemaName) == null) {
            logSkipedObject(schemaName, "TRIGGER", trigger.getTableName());
            return null;
        } else {
            PgTable pgTable = db.getSchema(schemaName).getTable(trigger.getTableName());
            if (pgTable != null){
                pgTable.addTrigger(trigger);
            } else {
                PgView pgView = db.getSchema(schemaName).getView(trigger.getTableName());
                if (pgView != null){
                    pgView.addTrigger(trigger);
                } else {
                    Log.log(Log.LOG_ERROR,
                            new StringBuilder().append("TABLE ")
                            .append(trigger.getTableName())
                            .append(" not found on schema ").append(schemaName)
                            .append(" That's why rule ").append(name)
                            .append("will be skipped").toString());
                }
                return null;
            }
        }
        return trigger;
    }

    public static class WhenListener extends SQLParserBaseListener {
        private String when;
        @Override
        public void exitWhen_trigger(When_triggerContext ctx) {
            when = getFullCtxText(ctx.when_expr);
        }
        public String getWhen() {
            return when;
        }
    }
}
