package ru.taximaxim.codekeeper.core.loader.jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import ru.taximaxim.codekeeper.core.PgDiffUtils;
import ru.taximaxim.codekeeper.core.parsers.antlr.AntlrParser;
import ru.taximaxim.codekeeper.core.parsers.antlr.PrivilegesParser;
import ru.taximaxim.codekeeper.core.parsers.antlr.PrivilegesParser.PrivilegeContext;
import ru.taximaxim.codekeeper.core.parsers.antlr.PrivilegesParser.PrivilegesContext;

/**
 * Parser for aclItem arrays
 *
 * @author ryabinin_av
 */
final class JdbcAclParser {

    public enum PrivilegeTypes {
        // SONAR-OFF
        a("INSERT"), r("SELECT"), w("UPDATE"), d("DELETE"), D("TRUNCATE"), x("REFERENCES"),
        t("TRIGGER"), X("EXECUTE"), U("USAGE"), C("CREATE"), T("CREATE_TEMP"), c("CONNECT");
        // SONAR-ON
        private final String privilegeType;

        PrivilegeTypes(String privilegeType) {
            this.privilegeType = privilegeType;
        }

        @Override
        public String toString() {
            return privilegeType;
        }
    }

    /**
     * Receives AclItem[] as String and parses it to list of Privilege objects.
     *
     * @param aclArrayAsString  String representation of AclItem array
     * @param maxTypes  Maximum available privilege bits for target DB object
     * @param order     Target order for privileges inside the privilege string
     *                  (not a result list sorting)
     * @param owner     Owner name (owner's privileges go first)
     * @return
     */
    public static List<Privilege> parse(String aclArrayAsString, int maxTypes, String order, String owner){
        List<Privilege> privileges = new ArrayList<>();

        // skip "empty" acl strings, such as "{}"
        if (aclArrayAsString.length() <= "{}".length()){
            return privileges;
        }

        PrivilegesContext ctx = AntlrParser.makeBasicParser(PrivilegesParser.class,
                aclArrayAsString, "jdbc privileges").privileges();

        for (PrivilegeContext acl : ctx.acls) {
            String grantor;
            String grantee = "";
            if (acl.qgrantor != null) {
                if (acl.qname != null) {
                    grantee = acl.qname.getText();
                }
                grantor = acl.qgrantor.getText();
            } else {
                if (acl.name != null) {
                    grantee = acl.name.getText();
                }
                grantor = acl.grantor.getText();
            }

            String grantsString = acl.priv.getText();

            Consumer<Privilege> adder = grantee.equals(owner) ? p -> privileges.add(0, p) : privileges::add;
            grantee = PgDiffUtils.getQuotedName(grantee);

            if (grantee.isEmpty()) {
                grantee = "PUBLIC";
            }

            // reorder chars according to order, split to two lists based on WITH GRANT OPTION
            List<Character> grantTypeCharsWithoutGo = new ArrayList<>(grantsString.length());
            List<Character> grantTypeCharsWithGo = new ArrayList<>(grantsString.length());
            for (int i = 0; i < order.length(); i++) {
                int index = grantsString.indexOf(String.valueOf(order.charAt(i)));
                if (index > -1) {
                    if (grantsString.length() > index + 1 && grantsString.charAt(index + 1) == '*') {
                        grantTypeCharsWithGo.add(order.charAt(i));
                    } else {
                        grantTypeCharsWithoutGo.add(order.charAt(i));
                    }
                }
            }

            if (grantTypeCharsWithoutGo.size() == maxTypes) {
                adder.accept(new Privilege(grantee, Arrays.asList("ALL"),
                        false, grantee.equals(owner) && grantor.equals(owner)));

            } else if (grantTypeCharsWithGo.size() == maxTypes) {
                adder.accept(new Privilege(grantee, Arrays.asList("ALL"),
                        true, false));

            } else if (grantTypeCharsWithoutGo.size() < maxTypes && grantTypeCharsWithGo.size() < maxTypes) {
                // add all grants without grant option
                addAllGrants(false, grantTypeCharsWithoutGo, grantee, adder);

                // add all grants with grant option
                addAllGrants(true, grantTypeCharsWithGo, grantee, adder);
            }
        }
        return privileges;
    }

    private static void addAllGrants(boolean isGO, List<Character> grantTypeChars,
            String grantee, Consumer<Privilege> adder) {
        List<String> grantTypesParsed = new ArrayList<>();
        for (int i = 0; i < grantTypeChars.size(); i++) {
            char c = grantTypeChars.get(i);
            grantTypesParsed.add(PrivilegeTypes.valueOf(String.valueOf(c)).toString());
        }
        if (!grantTypesParsed.isEmpty()) {
            adder.accept(new Privilege(grantee, grantTypesParsed, isGO, false));
        }
    }

    private JdbcAclParser() {
    }
}

class Privilege {
    final String grantee;
    final List<String> grantValues;
    final boolean isGO;
    /**
     * Default value - if grantor = grantee = owner and isGO is true (WITH GRANT OPTION)
     */
    final boolean isDefault;

    public Privilege(String grantee, List<String> grantValues, boolean isGO, boolean isDefault) {
        this.grantee = grantee;
        this.grantValues = grantValues;
        this.isGO = isGO;
        this.isDefault = isDefault;
    }

    public boolean isGrantAllToPublic() {
        return "PUBLIC".equals(grantee) && "ALL".equals(grantValues.get(0));
    }
}
