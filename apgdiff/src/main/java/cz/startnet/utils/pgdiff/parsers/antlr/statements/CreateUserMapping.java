package cz.startnet.utils.pgdiff.parsers.antlr.statements;

import java.util.Arrays;

import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Create_user_mapping_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Define_foreign_optionsContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Foreign_optionContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.User_mapping_nameContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.User_nameContext;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgUserMapping;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public class CreateUserMapping extends ParserAbstract {

    private final Create_user_mapping_statementContext ctx;

    public CreateUserMapping(Create_user_mapping_statementContext ctx, PgDatabase db) {
        super(db);
        this.ctx = ctx;
    }

    @Override
    public void parseObject() {
        User_mapping_nameContext userMapping = ctx.user_mapping_name();
        User_nameContext userName = userMapping.user_name();

        if (userName == null) {
            return;
        }
        String server = userMapping.identifier().getText();

        PgUserMapping usm = new PgUserMapping(userName.getText(), server);
        addDepSafe(usm, Arrays.asList(userMapping.identifier()), DbObjType.SERVER, true);

        Define_foreign_optionsContext options = ctx.define_foreign_options();
        if (options != null) {
            for (Foreign_optionContext option : options.foreign_option()) {
                fillOptionParams(option.character_string().getText(),
                        option.col_label().getText(), false, usm::addOption);
            }
        }
        addSafe(db, usm, Arrays.asList(userMapping));
    }

    @Override
    protected String getStmtAction() {
        StringBuilder sb = new StringBuilder();
        sb.append(ACTION_CREATE).append(' ').append(DbObjType.USER_MAPPING).append(" ");
        sb.append(getFullCtxText(ctx.user_mapping_name()));
        return sb.toString();
    }
}
