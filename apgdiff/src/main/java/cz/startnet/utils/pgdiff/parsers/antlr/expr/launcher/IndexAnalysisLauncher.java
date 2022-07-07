package cz.startnet.utils.pgdiff.parsers.antlr.expr.launcher;

import java.util.LinkedHashSet;
import java.util.Set;

import org.antlr.v4.runtime.ParserRuleContext;

import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Index_columnContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Index_restContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Storage_parametersContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Storage_parameter_optionContext;
import cz.startnet.utils.pgdiff.schema.PgIndex;
import cz.startnet.utils.pgdiff.schema.PgObjLocation;
import cz.startnet.utils.pgdiff.schema.meta.MetaContainer;

public class IndexAnalysisLauncher extends AbstractAnalysisLauncher {

    public IndexAnalysisLauncher(PgIndex stmt, Index_restContext ctx, String location) {
        super(stmt, ctx, location);
    }

    @Override
    public Set<PgObjLocation> analyze(ParserRuleContext ctx, MetaContainer meta) {
        Set<PgObjLocation> depcies = new LinkedHashSet<>();
        Index_restContext rest = (Index_restContext) ctx;

        for (Index_columnContext c : rest.index_columns().index_column()) {
            depcies.addAll(analyzeTableChildVex(c.column, meta));

            Storage_parametersContext params = c.storage_parameters();
            if (params != null) {
                for (Storage_parameter_optionContext o : params.storage_parameter_option()) {
                    depcies.addAll(analyzeTableChildVex(o.vex(), meta));
                }
            }
        }

        if (rest.index_where() != null){
            depcies.addAll(analyzeTableChildVex(rest.index_where().vex(), meta));
        }

        return depcies;
    }
}
