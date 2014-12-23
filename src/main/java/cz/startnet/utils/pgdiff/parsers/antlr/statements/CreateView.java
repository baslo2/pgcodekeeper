package cz.startnet.utils.pgdiff.parsers.antlr.statements;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.As_clauseContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Create_view_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Name_or_func_callsContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Query_expressionContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Set_function_specificationContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Simple_tableContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Table_primaryContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParserBaseVisitor;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.GenericColumn.ViewReference;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgFunction;
import cz.startnet.utils.pgdiff.schema.PgSelect;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import cz.startnet.utils.pgdiff.schema.PgView;

public class CreateView extends ParserAbstract {
    private Create_view_statementContext ctx;

    public CreateView(Create_view_statementContext ctx, PgDatabase db,
            Path filePath) {
        super(db, filePath);
        this.ctx = ctx;
    }

    @Override
    public PgStatement getObject() {
        String name = getName(ctx.name);
        String schemaName = getSchemaName(ctx.name);
        if (schemaName == null) {
            schemaName = getDefSchemaName();
        }
        PgView view = new PgView(name, getFullCtxText(ctx.getParent()), "");
        if (ctx.v_query != null) {
            view.setQuery(getFullCtxText(ctx.v_query));
            view.setSelect(parseSelect(ctx.v_query));
        }
        if (ctx.column_name != null) {
            for (String column : getNames(ctx.column_name.name)) {
                view.addColumnName(column);
            }
        }
        db.getSchema(schemaName).addView(view);
        fillObjLocation(view, ctx.name.getStart().getStartIndex(), schemaName,
                db.getSchema(schemaName).getView(name) != null);
        return view;
    }
    
    public SelectQueryVisitor getVisitor(PgSelect select) {
        return new SelectQueryVisitor(select);
    }

    private PgSelect parseSelect(Query_expressionContext ctx) {
        PgSelect select = new PgSelect(getFullCtxText(ctx), null);
        SelectQueryVisitor visitor = new SelectQueryVisitor(select);
        visitor.visit(ctx);
        return visitor.getSelect();
    }

    public class SelectQueryVisitor extends
            SQLParserBaseVisitor<Query_expressionContext> {
        // список алиасов запросов, игнорируются при заполнении колонок в селект
        private Queue<String> aliasNames = new LinkedList<>();
        //адреса объектов колонок, таблиц и функций
        private List<GenericColumn> columns = new ArrayList<>();
        // карта алиасов колонок, таблиц и функций
        private Map<String, GenericColumn> tableAliases = new HashMap<>();
        boolean isQiery = false;
        private PgSelect select;
        private boolean isTableRef = false;

        public SelectQueryVisitor(PgSelect select) {
            this.select = select;
        }

        @Override
        public Query_expressionContext visitTable_primary(
                Table_primaryContext ctx) {
            isTableRef = true;
            return super.visitTable_primary(ctx);
        }
        @Override
        public Query_expressionContext visitSimple_table(Simple_tableContext ctx) {
            if (ctx.query_specification() != null) {
                SelectQueryVisitor vis = new SelectQueryVisitor(select);
                vis.columns.addAll(columns);
                vis.visit(ctx.query_specification());
                columns.clear();
                columns.addAll(vis.getColumns());
                isQiery = true;
                tableAliases.putAll(vis.tableAliases);
                return null;
            }
            return super.visitSimple_table(ctx);
        }

        @Override
        public Query_expressionContext visitSet_function_specification(
                Set_function_specificationContext ctx) {
            if (ctx.COUNT() != null) {
                columns.add(new GenericColumn(null, getFullCtxText(ctx), null)
                        .setType(ViewReference.SYSTEM));
            } else {
                columns.add(new GenericColumn(null, ctx.general_set_function()
                        .set_function_type().getText(), null).setType(ViewReference.SYSTEM));
            }
            return null;
        }

        @Override
        public Query_expressionContext visitName_or_func_calls(
                Name_or_func_callsContext ctx) {
            if (ctx.function_calls_paren() != null) {
                addFunction(ctx);
                return null;
            } 
            if (isTableRef) {
                isTableRef = false;
                String tableName = getTableName(ctx.schema_qualified_name());
                columns.add(new GenericColumn(
                        tableName == null ? getDefSchemaName() : tableName,
                        getName(ctx.schema_qualified_name()), null).setType(ViewReference.TABLE));
                return null;
            }
            
            String colName = getName(ctx.schema_qualified_name());
            String colTable = getTableName(ctx.schema_qualified_name());
            String colSchema = getSchemaName(ctx.schema_qualified_name());
            if (colSchema == null || colSchema.equals(colTable)) {
                columns.add(new GenericColumn(null, colTable, colName));
            } else {
                columns.add(new GenericColumn(colSchema, colTable, colName));
            }
            return null;
        }
        
        private void addFunction(Name_or_func_callsContext ctx) {
            PgFunction func = new PgFunction(
                    getName(ctx.schema_qualified_name()), getFullCtxText(ctx), "");
            String schema = getSchemaName(ctx.schema_qualified_name());
            columns.add(new GenericColumn(schema, func.getSignature(), null)
                    .setType(ViewReference.FUNCTION));
        }
        

        @Override
        public Query_expressionContext visitAs_clause(As_clauseContext ctx) {
            String aliasName = ctx.identifier().getText();
            if (isQiery) {
                isQiery = false;
                aliasNames.add(aliasName);
                Iterator<GenericColumn> iter = columns.iterator();
                while (iter.hasNext()) {
                    GenericColumn col = iter.next();
                    if (col.table != null && col.table.equals(aliasName)) {
                        iter.remove();
                    }
                }
            } else {
                tableAliases.put(aliasName,
                            columns.get(columns.size() - 1));
            }
            return super.visitAs_clause(ctx);
        }

        private List<GenericColumn> getColumns() {
            // вытаскиваем таблицы из смешанного списка колонок и помещаем их в алиасы с их именами
            Iterator<GenericColumn> tableIter = columns.iterator();
            while (tableIter.hasNext()) {
                GenericColumn col = tableIter.next();
                if (col.getType() == ViewReference.TABLE) {
                    tableAliases.put(col.table, col);
                    tableIter.remove();
                }
            }
            Iterator<GenericColumn> iter = columns.iterator();
            List<GenericColumn> newColumns = new ArrayList<>();
            while (iter.hasNext()) {
                GenericColumn col = iter.next();
                // удаляем функции, и алиасы из подзапросов
                if (aliasNames.contains(col.table)) {
                    iter.remove();
                    continue;
                }
                switch (col.getType()) {
                case FUNCTION:
                case COLUMN:
                    GenericColumn unaliased = tableAliases.get(col.table);
                    if (unaliased != null) {
                        GenericColumn column = new GenericColumn(unaliased.schema,
                                unaliased.table, col.column);
                        if (unaliased.getType() == ViewReference.FUNCTION) {
                            column.setType(ViewReference.FUNCTION);
                        }
                        newColumns.add(column);
                    } else {
                        newColumns.add(col);
                    }
                    break;
                case TABLE:
                    break;
                }
            }
            columns.clear();
            columns.addAll(newColumns);
            return columns;
        }
        public PgSelect getSelect() {
            for (GenericColumn col : columns) {
                select.addColumn(col);
            }
            return select;
        }
    }
}
