package ru.taximaxim.codekeeper.ui.generators;

import java.util.Random;

import ru.taximaxim.codekeeper.core.PgDiffUtils;

/**
 * An implementation of a PostgreSql data generator for JSON type.
 *
 * @since 3.11.5
 * @author galiev_mr
 */
public class JsonPgData extends PgData<String> {

    public JsonPgData() {
        super(PgDataType.JSON, "{\"a\": \"b\"}", null, null); //$NON-NLS-1$
    }

    @Override
    public String generateValue() {
        switch (generator) {
        case CONSTANT: return PgDiffUtils.quoteString(start);
        case INCREMENT:
            return null;
        case RANDOM: return generateRandom();
        default:
            return null;
        }
    }

    @Override
    protected String generateRandom(Random ran) {
        return "'{\"" + genSymbols(ran.nextInt(length) + 1, true, true) + "\": \"" + //$NON-NLS-1$ //$NON-NLS-2$
                genSymbols(ran.nextInt(length) + 1, false, true) + "\"}'"; //$NON-NLS-1$
    }

    @Override
    public int getMaxValues() {
        return length == 0 ? 0 : (int) Math.pow(26, length);
    }

    @Override
    public String valueFromString(String s) {
        return s;
    }
}
