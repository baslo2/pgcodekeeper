package ru.taximaxim.codekeeper.core.parsers;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.taximaxim.codekeeper.core.PgDiffArguments;
import ru.taximaxim.codekeeper.core.TestUtils;
import ru.taximaxim.codekeeper.core.loader.ParserListenerMode;
import ru.taximaxim.codekeeper.core.loader.PgDumpLoader;
import ru.taximaxim.codekeeper.core.schema.GenericColumn;
import ru.taximaxim.codekeeper.core.schema.PgDatabase;
import ru.taximaxim.codekeeper.core.schema.PgObjLocation;

class ObjReferencesTest {

    private static final String REFS_POSTFIX = "_refs.txt";

    @ParameterizedTest
    @ValueSource(strings = {
            "aggregates",
            "foreign_data",
            "collate",
            "server",
            "user_mapping",
            "policy",
            "schema",
            "fts_configuration",
            "fts_parser",
            "fts_template",
            "create_function",
            "operator",
            "create_procedure",
            "sequence",
            "create_table",
            "alter_table",
            "type",
            "view",
            "database",
            "extension",
            "index",
            "triggers",
            "role",
            "rules",
            "domain",
            "other",
            "copy",
            "create_cast",
            "arrays",
            "case",
            "cluster",
            "conversion",
            "create_misc",
            "create_table_like",
            "date",
            "delete",
            "dependency",
            "drop_if_exists",
            "drop_operator",
            "enum",
            "event_trigger",
            "fast_default",
            "float8",
            "foreign_key",
            "functional_deps",
            "geometry",
            "groupingsets",
            "inherit",
            "insert_conflict",
            "insert",
            "interval",
            "join",
            "json_encoding",
            "jsonb",
            "lseg",
            "merge",
            "misc_functions",
            "misc_sanity",
            "name",
            "namespace",
            "numeric",
            "numeric_big",
            "numerology",
            "object_address",
            "oid",
            "oidjoins",
            "opr_sanity",
            "partition_aggregate",
            "partition_join",
            "partition_prune",
            "plancache",
            "plpgsql",
            "point",
            "polygon",
            "polymorphism",
            "privileges",
            "publication",
            "rangefuncs",
            "rangetypes",
            "reloptions",
            "rowtypes",
            "select",
            "set",
            "spgist",
            "strings",
            "subscription",
            "subselect",
            "sysviews",
            "time",
            "timestamp",
            "timestamptz",
            "timetz",
            "transactions",
            "tsdicts",
            "tsearch",
            "update",
            "window",
            "with"
    })
    void comparePgReferences(final String fileNameTemplate) throws IOException, InterruptedException {
        compareReferences(fileNameTemplate, false);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ms_aggregate",
            "ms_assemblies",
            "ms_database",
            "ms_function",
            "ms_index",
            "ms_procedures",
            "ms_table",
            "ms_server",
            "ms_roles",
            "ms_rule",
            "ms_triggers",
            "ms_type",
            "ms_view",
            "ms_schema",
            "ms_sequences",
            "ms_other",
            "ms_authorizations",
            "ms_availability_group",
            "ms_backup",
            "ms_broker_priority",
            "ms_certificates",
            "ms_control_flow",
            "ms_cursors",
            "ms_delete",
            "ms_drop",
            "ms_event",
            "ms_full_width_chars",
            "ms_insert",
            "ms_key",
            "ms_logins",
            "ms_merge",
            "ms_predicates",
            "ms_select",
            "ms_statements",
            "ms_transactions",
            "ms_update",
            "ms_users",
            "ms_xml_data_type",
            
    })
    void compareMsReferences(final String fileNameTemplate) throws IOException, InterruptedException {
        compareReferences(fileNameTemplate, true);
    }

    void compareReferences(String fileNameTemplate, boolean isMsSql) throws IOException, InterruptedException {
        PgDiffArguments args = new PgDiffArguments();
        args.setMsSql(isMsSql);

        String resource = fileNameTemplate + ".sql";
        PgDumpLoader loader = new PgDumpLoader(() -> getClass().getResourceAsStream(resource), resource, args);
        loader.setMode(ParserListenerMode.REF);
        PgDatabase db = loader.load();

        String expected = TestUtils
                .inputStreamToString(ObjReferencesTest.class
                        .getResourceAsStream(fileNameTemplate + REFS_POSTFIX))
                .strip();
        String actual = getRefsAsString(db.getObjReferences()).strip();

        Assertions.assertEquals(expected, actual);
    }

    private String getRefsAsString(Map<String, Set<PgObjLocation>> refs) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Set<PgObjLocation>> entry : refs.entrySet()) {
            entry.getValue().stream().sorted((e1, e2) -> Integer.compare(e1.getOffset(), e2.getOffset())).forEach(loc -> {
                sb.append("Reference: ");
                GenericColumn col = loc.getObj();
                if (col != null) {
                    sb.append("Object = ").append(col).append(", ");
                }
                sb.append("action = ").append(loc.getAction()).append(", ");
                sb.append("offset = ").append(loc.getOffset()).append(", ");
                sb.append("line number = ").append(loc.getLineNumber()).append(", ");
                sb.append("charPositionInLine = ").append(loc.getCharPositionInLine());
                sb.append(System.lineSeparator());
            });
        }

        return sb.toString();
    }
}
