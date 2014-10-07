package cz.startnet.utils.pgdiff.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.Log;
import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.parsers.CreateFunctionParser;
import cz.startnet.utils.pgdiff.parsers.Parser;
import cz.startnet.utils.pgdiff.schema.PgColumn;
import cz.startnet.utils.pgdiff.schema.PgConstraint;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgExtension;
import cz.startnet.utils.pgdiff.schema.PgForeignKey;
import cz.startnet.utils.pgdiff.schema.PgFunction;
import cz.startnet.utils.pgdiff.schema.PgIndex;
import cz.startnet.utils.pgdiff.schema.PgPrivilege;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import cz.startnet.utils.pgdiff.schema.PgSelect;
import cz.startnet.utils.pgdiff.schema.PgSequence;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import cz.startnet.utils.pgdiff.schema.PgTable;
import cz.startnet.utils.pgdiff.schema.PgTrigger;
import cz.startnet.utils.pgdiff.schema.PgView;

public class JdbcLoader {
    /*
     * Trigger firing conditions
     */
    private final int TRIGGER_TYPE_ROW = 1 << 0;
    private final int TRIGGER_TYPE_BEFORE = 1 << 1;
    private final int TRIGGER_TYPE_INSERT = 1 << 2;
    private final int TRIGGER_TYPE_DELETE = 1 << 3;
    private final int TRIGGER_TYPE_UPDATE = 1 << 4;
    private final int TRIGGER_TYPE_TRUNCATE = 1 << 5;
    private final int TRIGGER_TYPE_INSTEAD = 1 << 6;
    
    /*
     * Prepared statements to be executed
     */
    private PreparedStatement prepStatTables;
    private PreparedStatement prepStatViews;
    private PreparedStatement prepStatTriggers;
    private PreparedStatement prepStatFuncName;
    private PreparedStatement prepStatFunctions;
    private PreparedStatement prepStatLanguages;
    private PreparedStatement prepStatSequences;
    private PreparedStatement prepStatConstraints;
    private PreparedStatement prepStatIndecies;
    private PreparedStatement prepStatColumnsOfSchema;
    
    private Map<String, Long> cachedSchemaByName = new HashMap<String, Long>();
    private Map<Long, String> cachedRolesNamesByOid = new HashMap<Long, String>();
    private Map<Long, PgType> cachedTypeNamesByOid = new HashMap<Long, PgType>();
    private Map<Long, Map<Integer, String>> cachedColumnNamesByTableOid = new HashMap<Long, Map<Integer,String>>();
    
    private String host;
    private int port;
    private String user;
    private String pass;
    private String dbName;
    private String encoding;
    
    private Connection connection;
    private DatabaseMetaData metaData;
    
    public JdbcLoader(String host, int port, String user, String pass, String dbName, String encoding) {
        this.host = host;
        this.port = port == 0 ? ApgdiffConsts.JDBC_CONSTS.JDBC_DEFAULT_PORT : port;
        this.user = user.isEmpty() ? System.getProperty("user.name") : user;
        this.dbName = dbName;
        this.encoding = encoding;
        this.pass = pass.isEmpty() ? getPgPassPassword() : pass;
    }

    private String getPgPassPassword(){
        File pgpass = new File(System.getProperty("user.home") + "/.pgpass");
        
        try (BufferedReader br = new BufferedReader(new FileReader(pgpass))){        
            String line;
            String [] koko = {
                                host, 
                                String.valueOf(port), 
                                dbName, 
                                user
                            };
            
            while ((line = br.readLine()) != null) {
                try(Scanner sc = new Scanner(line)){
                    sc.useDelimiter(":");
                    
                    int tokenCounter = 0;
                    boolean fits = true;
                    
                    while(sc.hasNext()){
                        String token = sc.next();
                        
                        if (tokenCounter < 4){
                            if (token.equals(koko[tokenCounter]) || token.equals("*")){
                                
                            }else{
                                fits = false;                            
                            }
                        }else if (fits){
                            // assume for now that password has no colons
                            return token;
                        }
                        tokenCounter++;
                    }
                }
                
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not retreive pgpass password", e);
        }
        return "";
    }
    
    public PgDatabase getDbFromJdbc() throws IOException{
        PgDatabase d = new PgDatabase();
        try {
            Class.forName(ApgdiffConsts.JDBC_CONSTS.JDBC_DRIVER);
            connection = DriverManager.getConnection(
                   "jdbc:postgresql://" + host + ":" + port + "/" + dbName, user, pass);
            setTimeZone();
            prepareStatements();
            prepareData();
            metaData = connection.getMetaData();

            try(Statement stmnt = connection.createStatement(); 
                    ResultSet res = stmnt.executeQuery(JdbcQueries.QUERY_SCHEMAS)){
                while (res.next()) {
                    prepareDataForSchema(res.getLong("oid"));
                    String schemaName = res.getString("nspname");
                    PgSchema schema = getSchema(schemaName);
                    
                    if (!schemaName.equals("public")){
                        schema.setOwner(res.getString("owner"));
                    }
                    setPrivileges(schema, schemaName, res.getString("nspacl"), res.getString("owner"));
                    
                    String comment = res.getString("comment");
                    if (!schemaName.equals("public") && comment != null && !comment.isEmpty()){
                        schema.setComment("'" + comment + "'");
                    }
                    
                    if (schemaName.equals("public")){
                        d.replaceSchema(d.getSchema("public"), schema);                    
                    }else{
                        d.addSchema(schema);
                    }
                }   
            }
            
            try(Statement stmnt = connection.createStatement(); 
                    ResultSet res = stmnt.executeQuery(JdbcQueries.QUERY_EXTENSIONS)){
                while(res.next()){
                    PgExtension extension = getExtension(res);
                    if (extension != null){
                        d.addExtension(extension);
                    }
                }
            }
        } catch (SQLException e) {
            throw new IOException("Database JDBC access error occured", e);
        } catch (ClassNotFoundException e) {
            throw new IOException("JDBC driver class not found", e);
        }finally{
            closeResources();
        }
        return d;
    }
    
    private void setTimeZone() throws SQLException {
        try(Statement stmnt = connection.createStatement()){
            stmnt.execute("SET timezone = 'UTC'");
        }
    }

    private void prepareData() throws SQLException {
        try(Statement stmnt = connection.createStatement()){
            // fill in namespace map
            try(ResultSet res = stmnt.executeQuery("SELECT oid::bigint, nspname FROM pg_catalog.pg_namespace")){
                while(res.next()){
                    cachedSchemaByName.put(res.getString("nspname"), res.getLong("oid"));
                }                
            }
            
            // fill in rolenames
            try(ResultSet res = stmnt.executeQuery("SELECT oid::bigint, rolname FROM pg_catalog.pg_roles")){
                while (res.next()){
                    cachedRolesNamesByOid.put(res.getLong("oid"), res.getString("rolname"));
                }
            }
            
            // fill in data types
            try(ResultSet res = stmnt.executeQuery("SELECT t.oid::bigint, t.typname, t.typlen, "
                    + "t.typelem::regtype AS castedType, t.typarray, n.nspname, proc.proname, nsp.nspname  "
                    + "FROM pg_catalog.pg_type t LEFT JOIN pg_catalog.pg_namespace n ON t.typnamespace = n.oid "
                    + "LEFT JOIN pg_catalog.pg_proc proc ON proc.oid = t.typmodout "
                    + "LEFT JOIN pg_catalog.pg_namespace nsp ON t.typnamespace = nsp.oid")){
                while (res.next()){
                    PgType type = new PgType(res.getString("typname"), res.getString("castedType"), res.getLong("typarray"), res.getInt("typlen"), res.getString("proname"), res.getString("nspname"));
                    cachedTypeNamesByOid.put(res.getLong("oid"), type);
                }                
            }
        }
    }

    private void prepareStatements() throws SQLException{
        prepStatTables = connection.prepareStatement(JdbcQueries.QUERY_TABLES_PER_SCHEMA);
        prepStatViews = connection.prepareStatement(JdbcQueries.QUERY_VIEWS_PER_SCHEMA);
        prepStatTriggers = connection.prepareStatement(JdbcQueries.QUERY_TRIGGERS_PER_TABLE);
        prepStatFuncName = connection.prepareStatement("SELECT proname, nsp.nspname FROM pg_catalog.pg_proc proc LEFT JOIN pg_catalog.pg_namespace nsp ON proc.pronamespace = nsp.oid WHERE proc.oid = ?");
        prepStatFunctions = connection.prepareStatement(JdbcQueries.QUERY_FUNCTIONS_PER_SCHEMA);
        prepStatLanguages = connection.prepareStatement("SELECT lanname FROM pg_catalog.pg_language WHERE oid = ?");
        prepStatSequences = connection.prepareStatement(JdbcQueries.QUERY_SEQUENCES_PER_SCHEMA);
        prepStatConstraints = connection.prepareStatement(JdbcQueries.QUERY_TABLE_CONSTRAINTS);
        prepStatIndecies = connection.prepareStatement(JdbcQueries.QUERY_INDEX);
        prepStatColumnsOfSchema = connection.prepareStatement(JdbcQueries.QUERY_COLUMNS_PER_SCHEMA);
    }

    private void closeResources() {
        try {
            connection.close();
        } catch (Exception e) {
            Log.log(Log.LOG_INFO, "Could not close JDBC connection", e);
        }
        try {
            prepStatTables.close();
        } catch (Exception e) {
            Log.log(Log.LOG_INFO, "Could not close prepared statement for tables", e);
        }
        try {
            prepStatTriggers.close();
        } catch (Exception e) {
            Log.log(Log.LOG_INFO, "Could not close prepared statement for triggers", e);
        }
        try {
            prepStatFuncName.close();
        } catch (Exception e) {
            Log.log(Log.LOG_INFO, "Could not close prepared statement for function names", e);
        }
        try {
            prepStatFunctions.close();
        } catch (Exception e) {
            Log.log(Log.LOG_INFO, "Could not close prepared statement for functions", e);
        }
        try {
            prepStatLanguages.close();
        } catch (Exception e) {
            Log.log(Log.LOG_INFO, "Could not close prepared statement for languages", e);
        }
        try {
            prepStatSequences.close();
        } catch (Exception e) {
            Log.log(Log.LOG_INFO, "Could not close prepared statement for sequences", e);
        }
        try {
            prepStatConstraints.close();
        } catch (Exception e) {
            Log.log(Log.LOG_INFO, "Could not close prepared statement for constraints", e);
        }
        try {
            prepStatIndecies.close();
        } catch (Exception e) {
            Log.log(Log.LOG_INFO, "Could not close prepared statement for indecies", e);
        }
        try {
            prepStatColumnsOfSchema.close();
        } catch (Exception e) {
            Log.log(Log.LOG_INFO, "Could not close prepared statement for schema columns", e);
        }
    }
    
    private PgSchema getSchema(String schema) throws SQLException{
        PgSchema s = new PgSchema(schema, "");

        // setting current schema as default
        try(Statement stmnt = connection.createStatement()){
            stmnt.execute("SET search_path TO " + schema + ";");
        }
        
        // TABLES
        prepStatTables.setLong(1, getSchemaOidByName(schema));
        try(ResultSet res = prepStatTables.executeQuery()){
            Long previousTableOid = 0L;
            PgTable previousTable = null;
            while (res.next()) {
                /**
                 * Костыль: на данный момент не получается собрать имена нескольких 
                 * наследуемых таблиц в массив строк, поэтому, при наличии нескольких 
                 * таблиц в разделе INHERITS таблицы X, выводится соответствующее 
                 * количество строк в ResultSet'e с relname = X
                 */
                if (previousTableOid.equals(res.getLong("oid")) && previousTable != null){
                    previousTable.addInherits(res.getString("inherited"));
                }else{
                    PgTable table = getTable(res, schema);
                    s.addTable(table);
                    previousTableOid = res.getLong("oid");
                    previousTable = table;
                }
            }
        }
        
        // VIEWS
        prepStatViews.setLong(1, getSchemaOidByName(schema));
        try(ResultSet res = prepStatViews.executeQuery()){
            while (res.next()) {
                PgView view = getView(res, schema);
                s.addView(view);
            }
        }
        
        // FUNCTIONS
        prepStatFunctions.setLong(1, getSchemaOidByName(schema));
        try(ResultSet res = prepStatFunctions.executeQuery()){
            while (res.next()){
                PgFunction function = getFunction(res, schema);
                if (function != null){
                    s.addFunction(function);
                }
            }
        }

        // SEQUENCES
        prepStatSequences.setString(1, schema);
        try(ResultSet res = prepStatSequences.executeQuery()){
            PgSequence sequence = null;
            int previousSeqOid = 0;
            while(res.next()){
                if (previousSeqOid != res.getInt("sequence_oid")){
                    sequence = getSequence(res, schema);
                    if (sequence != null){
                        s.addSequence(sequence);
                    }
                }else if (sequence != null && res.getInt("referenced_column") != 0){
                    Integer[] ownedColumnNumbers = {res.getInt("referenced_column")};
                    sequence.setOwnedBy(res.getString("referenced_table_name") + "." + getColumnNames(ownedColumnNumbers, res.getLong("referenced_table_oid")).get(0));
                }
                previousSeqOid = res.getInt("sequence_oid");
            }
        }
        return s;
    }
    
    private PgExtension getExtension(ResultSet res) throws SQLException {
        PgExtension e = new PgExtension(res.getString("extname"), "");
//        e.setVersion(res.getString("extversion"));
        e.setOwner(getRoleNameByOid(res.getLong("extowner")));
        e.setSchema(getScheNameByOid(res.getLong("extnamespace")));
        
        String comment = res.getString("description");
        e.setComment(comment != null && !comment.isEmpty() ? "'" + comment + "'" : null);
        return e;
    }

    private PgConstraint getConstraint(ResultSet res, String schemaName, String tableName) throws SQLException {
        String constraintName = res.getString("conname");
        String definition = "";
        PgConstraint c = new PgConstraint(constraintName, "", getSearchPath(schemaName));
        
        List<String> columnNames = getColumnNames((Integer[])res.getArray("conkey").getArray(), res.getLong("conrelid"));
        switch (res.getString("contype")){
            case "f":
                c = new PgForeignKey(constraintName, "", getSearchPath(schemaName));
                List<String> referencedColumnNames = getColumnNames((Integer[])res.getArray("confkey").getArray(), res.getLong("confrelid"));
                definition = "FOREIGN KEY (";
                // TODO code reuse
                for(int i = 0; i < columnNames.size(); i++){
                    String columnName = columnNames.get(i);
                    definition = definition.concat(columnName);
                    if(i < columnNames.size() - 1){
                        definition = definition.concat(", ");
                    }
                }
                
                SimpleEntry<String, String> referencedTableName = getTableNameByOid(res.getLong("confrelid"));
                String schemaPrefix = "";
                if (!referencedTableName.getKey().equals(schemaName)){
                    schemaPrefix = referencedTableName.getKey() + ".";
                }
                definition = definition.concat(") REFERENCES " + schemaPrefix + referencedTableName.getValue() + "(");
                
                for(int i = 0; i < referencedColumnNames.size(); i++){
                    String columnName = referencedColumnNames.get(i);
                    definition = definition.concat(columnName);
                    if(i < referencedColumnNames.size() - 1){
                        definition = definition.concat(", ");
                    }
                }
                definition = definition.concat(")");
                
                switch (res.getString("confmatchtype")){
                    case "f":
                        definition = definition.concat(" MATCH FULL");
                        break;
                    case "p":
                        definition = definition.concat(" MATCH PARTIAL");
                        break;
                }
                
                switch(res.getString("confupdtype")){
                    case "r":
                        definition = definition.concat(" ON UPDATE RESTRICT");
                        break;
                    case "c":
                        definition = definition.concat(" ON UPDATE CASCADE");
                        break;
                    case "n":
                        definition = definition.concat(" ON UPDATE SET NULL");
                        break;
                    case "d":
                        definition = definition.concat(" ON UPDATE SET DEFAULT");
                        break;
                }
                
                switch(res.getString("confdeltype")){
                    case "r":
                        definition = definition.concat(" ON DELETE RESTRICT");
                        break;
                    case "c":
                        definition = definition.concat(" ON DELETE CASCADE");
                        break;
                    case "n":
                        definition = definition.concat(" ON DELETE SET NULL");
                        break;
                    case "d":
                        definition = definition.concat(" ON DELETE SET DEFAULT");
                        break;
                }
                break;
            case "p":
                definition = "PRIMARY KEY (";
                for(int i = 0; i < columnNames.size(); i++){
                    String columnName = columnNames.get(i);
                    definition = definition.concat(columnName);
                    if(i < columnNames.size() - 1){
                        definition = definition.concat(", ");
                    }
                }
                definition = definition.concat(")");
                break;
            case "c":
                definition = "CHECK (" + res.getString("consrc") + ")";
                break;
            case "u":
                definition = "UNIQUE (";
                for(int i = 0; i < columnNames.size(); i++){
                    String columnName = columnNames.get(i);
                    definition = definition.concat(columnName);
                    if(i < columnNames.size() - 1){
                        definition = definition.concat(", ");
                    }
                }
                definition = definition.concat(")");
                break;
        }
        
        c.setDefinition(definition);
        
        // set table name
        c.setTableName(tableName);
        
        String comment = res.getString("description");
        if (comment != null && !comment.isEmpty()){
            c.setComment("'" + comment + "'");
        }
        
        return c;
    }
    
    private long timeNanosec = 0L;
    
    /**
     * Output to stderr time of some operation (required to be called twice)  
     */
    private void t(String mes){
        if (!mes.isEmpty())
            System.err.println(mes + " " + (System.nanoTime() - timeNanosec)/1000000 + " msec");
        timeNanosec = System.nanoTime();
    }
    
    private PgView getView(ResultSet res, String schemaName) throws SQLException {
        String viewName = res.getString("relname");
        
        String viewDef = res.getString("definition").trim();
        if (viewDef == null){
            // TODO throw exception, log, output to console?
            System.err.println("View without definition (locked): " + viewName);
            viewDef = "";
        }else if (viewDef.charAt(viewDef.length() - 1) == ';'){
            viewDef = viewDef.substring(0, viewDef.length() - 1);
        }
        
        PgView v = new PgView(viewName, viewDef, getSearchPath(schemaName));
        v.setQuery(viewDef);
        // skip column names (aliases), as they are not used by us
        
        // we skip PgSelect, as it does not affect export (does it?)
        // TODO can query selected columns from pg_catalog
        // prevent NPE, because select in PgView is not initialized
        v.setSelect(new PgSelect("", ""));
        
        // Query columns default values and comments
        ResultSet res2 = metaData.getColumns(null, schemaName, viewName, null);
        while(res2.next()){
            String colName = res2.getString("COLUMN_NAME");
            String colDefault = res2.getString("COLUMN_DEF");
            if (colDefault != null){
                v.addColumnDefaultValue(colName, colDefault);
            }
            String colComment = res2.getString("REMARKS");
            if (colComment != null){
                v.addColumnComment(colName, colComment);
            }
        }
        
        // OWNER
        v.setOwner(getRoleNameByOid(res.getLong("relowner")));
        
        // Query view privileges
        // UGLY way
        String viewAclQuery = "SELECT relacl FROM pg_catalog.pg_class WHERE relname = '" + 
                                viewName + "' AND relnamespace = " + getSchemaOidByName(schemaName);
        try(Statement stmnt = connection.createStatement(); 
                ResultSet res3 = stmnt.executeQuery(viewAclQuery)){
            if (res3.next()){
                setPrivileges(v, viewName, res3.getString("relacl"), v.getOwner());
            }
        }
        
        // COMMENT
        String comment = res.getString("comment");
        if (comment != null && !comment.isEmpty()){
            v.setComment("'" + comment + "'");
        }
        
        return v;
    }

    private PgTable getTable(ResultSet res, String schemaName) throws SQLException{
        Long tableOid = res.getLong("oid");
        String tableName = res.getString("relname");
        StringBuilder tableDef = new StringBuilder(); 
        tableDef.append("CREATE TABLE ".concat(tableName).concat(" (\n"));
        
        List<PgColumn> columns = new ArrayList<PgColumn>(5);
        
        Integer[] colNumbers = (Integer[])res.getArray("col_numbers").getArray();
        for (int i = 0; i < colNumbers.length; i++) {
            if (colNumbers[i] < 1){
                continue;
            }
            String[] colNames = (String[])res.getArray("col_names").getArray();
            Long[] colTypes = (Long[])res.getArray("col_types").getArray();
            String[] colDefaults = (String[])res.getArray("col_defaults").getArray();
            String[] colComments = (String[])res.getArray("col_comments").getArray();
            Integer[] colTypeMod = (Integer[])res.getArray("col_typemod").getArray();
            Boolean[] colNotNull = (Boolean[])res.getArray("col_notnull").getArray();
            
            String columnName = colNames[i];
            PgColumn column = new PgColumn(columnName);
            
            PgType columnType = cachedTypeNamesByOid.get(colTypes[i]);
            String columnTypeName = getTypeNameByOid(colTypes[i], schemaName);
            
            if (colTypeMod[i] != -1 && columnType.getTypmodout() != null && !columnType.getTypmodout().isEmpty()){
                StringBuilder query = new StringBuilder();
                query.append("SELECT ").append(columnType.getTypmodout()).append("(").append(String.valueOf(colTypeMod[i])).append(")");
                try(Statement stmnt = connection.createStatement();
                        ResultSet res2 = stmnt.executeQuery(query.toString())){
                    if (res2.next()){
                        columnTypeName = columnTypeName.concat(res2.getString(1));
                    }
                }
            }
            column.setType(columnTypeName);
            
            tableDef.append("   " + columnName + " " + columnTypeName);
            
            String columnDefault = colDefaults[i];
            if (columnDefault != null && !columnDefault.isEmpty()){
                tableDef.append(" DEFAULT " + columnDefault);
                column.setDefaultValue(columnDefault);
            }
            
            if (colNotNull[i]){
                tableDef.append(" NOT NULL");
                column.setNullValue(false);
            }
            
            tableDef.append(",\n");
            String comment = colComments[i];
            if (comment != null && !comment.isEmpty()){
                column.setComment("'" + comment + "'");                
            }
            columns.add(column);
        }
        tableDef.append(");");
        
        PgTable t = new PgTable(tableName, tableDef.toString(), getSearchPath(schemaName));
        // INHERITS
        String inherits = res.getString("inherited");
        if(inherits != null && !inherits.isEmpty()){
            t.addInherits(inherits);
        }else{
            for(PgColumn column : columns){
                t.addColumn(column);
            }
        }

        // STORAGE PARAMETERS
        Array arr = res.getArray("reloptions");
        if (arr != null){
            StringBuilder storageParameters = new StringBuilder();
            String[] options = (String[])arr.getArray();
            for(int i = 0; i < options.length; i++){
                storageParameters.append(options[i]);
                
                if (i < options.length - 1){
                    storageParameters.append(", ");
                }
            }
            if (storageParameters.length() > 0){
                storageParameters.insert(0, "(").append(")");
                t.setWith(storageParameters.toString());
            }
        }
        
        // Table COMMENTS
        String comment = res.getString("table_comment");
        if (comment != null && !comment.isEmpty()){
            t.setComment("'" + comment + "'");                
        }
        
        // PRIVILEGES, OWNER
        t.setOwner(getRoleNameByOid(res.getLong("relowner")));
        setPrivileges(t, t.getName(), res.getString("aclarray"), t.getOwner());
        
        // Query CONSTRAINTS
        prepStatConstraints.setLong(1, tableOid);
        try(ResultSet resConstraints = prepStatConstraints.executeQuery()){
            while (resConstraints.next()){
                PgConstraint constraint = getConstraint(resConstraints, schemaName, t.getName());
                if (constraint != null){
                    t.addConstraint(constraint);
                }
            }
        }
        
        // Query INDECIES
        prepStatIndecies.setLong(1, tableOid);
        try(ResultSet resIndecies = prepStatIndecies.executeQuery()){
            while (resIndecies.next()){
                PgIndex index = getIndex(resIndecies, t.getName(), tableOid);
                if (index != null){
                    t.addIndex(index);
                }
            }
        }
        
        // Query TRIGGERS
        prepStatTriggers.setLong(1, tableOid);
        try(ResultSet resTriggers = prepStatTriggers.executeQuery()){
            while(resTriggers.next()){
                PgTrigger trigger = getTrigger(resTriggers, schemaName);
                if (trigger != null){
                    t.addTrigger(trigger);
                }
            }
        }

        return t;
    }

    /**
     * Returns trigger object.
     * <br>
     * Available trigger firing conditions:
     *      boolean onDelete;
     *      boolean onInsert;
     *      boolean onUpdate;
     *      boolean onTruncate;
     *     
     *      boolean forEachRow;
     *      boolean before;
     * @param schemaName 
     */
    private PgTrigger getTrigger(ResultSet res, String schemaName) throws SQLException{
        
        String triggerName = res.getString("tgname");
        PgTrigger t = new PgTrigger(triggerName, "", getSearchPath(schemaName));
        
        int firingConditions = res.getInt("tgtype");
        if ((firingConditions & TRIGGER_TYPE_DELETE) != 0){
            t.setOnDelete(true);
        }
        if ((firingConditions & TRIGGER_TYPE_INSERT) != 0){
            t.setOnInsert(true);
        }
        if ((firingConditions & TRIGGER_TYPE_UPDATE) != 0){
            t.setOnUpdate(true);
        }
        if ((firingConditions & TRIGGER_TYPE_TRUNCATE) != 0){
            t.setOnTruncate(true);
        }
        if ((firingConditions & TRIGGER_TYPE_ROW) != 0){
            t.setForEachRow(true);
        }
        if ((firingConditions & TRIGGER_TYPE_BEFORE) != 0){
            t.setBefore(true);
        }else{
            t.setBefore(false);
        }
        
        String tableName = getTableNameByOid(res.getLong("tgrelid")).getValue();
        t.setTableName(tableName);
        
        String functionName = getFunctionNameByOid(res.getLong("tgfoid"), schemaName).concat("()");
        t.setFunction(functionName);
        return t;
    }
    
    private PgIndex getIndex(ResultSet res, String tableName, Long tableOid) throws SQLException {
        String schemaName = getScheNameByOid(res.getLong("relnamespace"));
        
        String indexName = res.getString("relname");
        PgIndex i = new PgIndex(indexName, "", getSearchPath(schemaName));
        i.setTableName(tableName);

        String definition = res.getString("definition"); 
        i.setDefinition(definition.substring(definition.indexOf("USING ")));
        
        i.setUnique(res.getBoolean("indisunique"));
        setOwner(i, res.getLong("relowner"));
        
        return i;
    }
    
    private void setOwner(PgStatement statement, Long ownerOid){
        setOwner(statement, getRoleNameByOid(ownerOid));
    }
    
    private void setOwner(PgStatement statement, String ownerName){
        statement.setOwner(ownerName);
    }

    /**
     * Returns function object accordingly to data stored in current res row
     * (except for aggregate functions). 
     * Defines function body from Postgres pg_get_functiondef() output.
     * <br><br>
     * 
     * Use this select query to define function manually (not completed):
     * "SELECT proname, prolang, prosrc, proisagg AS isaggregate, proiswindow AS iswindow, 
     *         prosecdef AS issecuritydefiner, proleakproof AS isleakproof, 
     *         proisstrict AS isnullonnull FROM pg_catalog.pg_proc WHERE pronamespace = ?"
     */
    private PgFunction getFunction(ResultSet res, String schemaName) throws SQLException{
        for(String depType : (String[]) res.getArray("deps").getArray()){
            if (depType.equals("e")){
                return null;
            }
        }
        
        String functionName = res.getString("proname");
        PgFunction f = new PgFunction(functionName, "", getSearchPath(schemaName));
        
        String definition = res.getString("probody");
        String languageName = getLangNameByOid(res.getLong("prolang"));
        int langFirstOccurenceIndex = definition.indexOf("LANGUAGE " + languageName);
        String body = definition.substring(langFirstOccurenceIndex);
        
        // TODO Some debug modifications are done to function body
        // BEGIN debug modifications
        if (body.charAt(body.length() - 1) == '\n'){
            body = body.substring(0, body.length() - 1);
        }
        body = body.replaceAll("\\$function\\$", "\\$\\$");
        body = body.replace("LANGUAGE " + languageName + "\n SECURITY DEFINER\nAS ", "LANGUAGE " + languageName + " SECURITY DEFINER\n    AS ");
        body = body.replace("LANGUAGE " + languageName + "\n IMMUTABLE STRICT SECURITY DEFINER\nAS ", "LANGUAGE " + languageName + " IMMUTABLE STRICT SECURITY DEFINER\n    AS ");
        body = body.replace("LANGUAGE " + languageName + "\n STABLE SECURITY DEFINER\nAS ", "LANGUAGE " + languageName + " STABLE SECURITY DEFINER\n    AS ");
        body = body.replace("LANGUAGE " + languageName + "\n IMMUTABLE\nAS ", "LANGUAGE " + languageName + " IMMUTABLE\n    AS ");
        body = body.replace("LANGUAGE " + languageName + "\nAS","LANGUAGE " + languageName + "\n    AS");
        body = body.replace("LANGUAGE " + languageName + "\n IMMUTABLE SECURITY DEFINER\nAS ", "LANGUAGE " + languageName + " IMMUTABLE SECURITY DEFINER\n    AS ");
        body = body.replace("LANGUAGE " + languageName + "\n STABLE STRICT SECURITY DEFINER\nAS ", "LANGUAGE " + languageName + " STABLE STRICT SECURITY DEFINER\n    AS ");
        body = body.replace("LANGUAGE " + languageName + "\n STRICT SECURITY DEFINER\nAS ", "LANGUAGE " + languageName + " STRICT SECURITY DEFINER\n    AS ");
        // END debug modifications
        
        f.setBody(body);
        
        // RETURN TYPE
        Array proargmodes = res.getArray("proargmodes");
        boolean returnsTable = false;
        StringBuilder returnedTableArguments = new StringBuilder();
        if (proargmodes != null && Arrays.asList((String[])proargmodes.getArray()).contains("t")){
            String [] argModes = (String[])proargmodes.getArray();
            String [] argNames = (String[])res.getArray("proargnames").getArray();
            Long [] argTypeOids = (Long[])res.getArray("proallargtypes").getArray();
            for(int i = 0; i < argModes.length; i++){
                String type = argModes[i];
                if (type.equals("t")){
                    returnsTable = true;
                    if(returnedTableArguments.length() > 0){
                        returnedTableArguments.append(", ");
                    }
                    returnedTableArguments.append(argNames[i] + " " + getTypeNameByOid(argTypeOids[i], schemaName));
                }
            }            
        }
        
        if (returnsTable){
            f.setReturns("TABLE(" + returnedTableArguments + ")");
        }else if (res.getBoolean("proretset")){
            f.setReturns("SETOF " + getTypeNameByOid(res.getLong("prorettype"), schemaName));
        }else{
            f.setReturns(getTypeNameByOid(res.getLong("prorettype"), schemaName));
        }
        
        // ARGUMENTS
        String arguments = res.getString("proarguments");
        if (!arguments.isEmpty()){
            CreateFunctionParser.parseArguments(new Parser("(" + arguments + ")"), f);
        }
        
        // OWNER
        setOwner(f, res.getLong("proowner"));
        
        // PRIVILEGES
        String signatureWithoutDefaults = functionName + "(" + res.getString("proarguments_without_default") + ")";
        setPrivileges(f, signatureWithoutDefaults, res.getString("aclArray"), f.getOwner());
        
        // COMMENT
        String comment = res.getString("comment");
        f.setComment(comment != null && !comment.isEmpty() ? "'" + comment + "'" : null);
        return f;
    }
    
    private PgSequence getSequence(ResultSet res, String schemaName) throws SQLException {
        String sequenceName = res.getString("sequence_name");
        PgSequence s = new PgSequence(sequenceName, "", getSearchPath(schemaName));
        s.setCycle(res.getBoolean("cycle_option"));
        s.setIncrement(res.getString("increment"));
        
        // The data type of the sequence: In PostgreSQL, this is currently always bigint
        String maxValue = res.getString("maximum_value");
        s.setMaxValue(maxValue.equals(String.valueOf(Long.MAX_VALUE)) ? null : maxValue);
        String minValue = res.getString("minimum_value"); 
        s.setMinValue(minValue.equals("1") ? null : minValue);
        
        s.setStartWith(res.getString("start_value"));
        s.setCache(String.valueOf(1));
        // TODO SELECT cache_value FROM tableName;
        
        Integer[] ownedColumnNumbers = {res.getInt("referenced_column")};
        if (!ownedColumnNumbers[0].equals(Integer.valueOf(0))){
            // TODO can getColumnNames return an empty map, so that IndexOutOfBoundsException is thrown?
            s.setOwnedBy(res.getString("referenced_table_name") + "." + getColumnNames(ownedColumnNumbers, res.getLong("referenced_table_oid")).get(0));
        }
        
        setOwner(s, res.getLong("relowner"));
        
        // PRIVILEGES
        setPrivileges(s, sequenceName, res.getString("aclArray"), s.getOwner());
        
        return s;
    }
    
    private void setPrivileges(PgStatement st, String stSignature, String aclItemsArrayAsString, String owner){
        if (aclItemsArrayAsString == null){
            return;
        }
        String stType = "";
        int possiblePrivilegeCount = 12;
        if (st instanceof PgSequence){
            stType = "SEQUENCE";
            possiblePrivilegeCount = 3;
        }else if (st instanceof PgFunction){
            stType = "FUNCTION";
            possiblePrivilegeCount = 1;
        }else if (st instanceof PgTable || st instanceof PgView){
            stType = "TABLE";
            possiblePrivilegeCount = 7;
        }else if (st instanceof PgSchema){
            stType = "SCHEMA";
            possiblePrivilegeCount = 2;
        }else{
            throw new IllegalStateException("Not supported PgStatement class");
        }
        String revokePublic = "ALL ON " + stType + " " + stSignature + " FROM PUBLIC";
        String revokeMaindb = "ALL ON " + stType + " " + stSignature + " FROM " + PgDiffUtils.getQuotedName(owner);
        st.addPrivilege(new PgPrivilege(true, revokePublic, "REVOKE " + revokePublic));
        st.addPrivilege(new PgPrivilege(true, revokeMaindb, "REVOKE " + revokeMaindb));
        
        LinkedHashMap<String, String> grants = new JdbcAclParser().parse(aclItemsArrayAsString, possiblePrivilegeCount, owner);
        for(String granteeName : grants.keySet()){
            String privDefinition = grants.get(granteeName) + " ON " + stType + " " + stSignature + " TO " + granteeName;
            st.addPrivilege(new PgPrivilege(false, privDefinition, "GRANT " + privDefinition));
        }
    }
    
    private String getSearchPath(String schema){
        return "SET search_path = " + schema + ", pg_catalog;";
    }

    private void prepareDataForSchema(Long schemaOid) throws SQLException{
        // fill in map with columns of tables and indecies of schema
        prepStatColumnsOfSchema.setLong(1, schemaOid);
        try(ResultSet res = prepStatColumnsOfSchema.executeQuery();){
            cachedColumnNamesByTableOid.clear();
            Long previousTableOid = 0L;
            Map<Integer, String> previousMap = null;
            while (res.next()){
                Integer columnNumber = res.getInt("attnum");
                if (columnNumber < 1){
                    continue;
                }
                Long tableOid = res.getLong("attrelid");
                String columnName = res.getString("attname");
                if (!previousTableOid.equals(tableOid)){
                    previousTableOid = tableOid;
                    previousMap = new HashMap<Integer, String>();
                    previousMap.put(columnNumber, columnName);
                    cachedColumnNamesByTableOid.put(tableOid, previousMap);
                }else{
                    previousMap.put(columnNumber, columnName);                
                }
            }
        }
    }
    
    /**
     * Returns an array of column names, resolved from conCols Array object 
     * by pg_attribute.attnum and tableOid
     * 
     * @param conCols   Array of ints containing column numbers (references pg_attribute.attnum)
     * @param tableOid  Oid of table - owner of these columns
     * @return
     */
    private List<String> getColumnNames(Integer[] cols, Long tableOid) throws SQLException{
        Map <Integer, String> tableColumns = cachedColumnNamesByTableOid.get(tableOid);
        // if requested table is in different schema
        if (tableColumns == null){
            try(    Statement st = connection.createStatement();
                    ResultSet res = st.executeQuery("SELECT attname, attnum FROM "
                            + "pg_catalog.pg_attribute WHERE attrelid = " + tableOid);){
                tableColumns = new HashMap<Integer, String>();
                while(res.next()){
                    tableColumns.put(res.getInt("attnum"), res.getString("attname"));
                }
                cachedColumnNamesByTableOid.put(tableOid, tableColumns);
            }
        }
        List<String> result = new ArrayList<String>(5);
        for(Integer n : cols){
            result.add(tableColumns.get(n));
        }
        return result;
    }

    private String getFunctionNameByOid(Long functionOid, String targetSchemaName) throws SQLException{
        prepStatFuncName.setLong(1, functionOid);
        try(ResultSet res = prepStatFuncName.executeQuery()){
            if (res.next()){
                String funcSchemaName = res.getString("nspname");
                return funcSchemaName.equals(targetSchemaName) ? 
                        res.getString("proname") : funcSchemaName.concat(".").concat(res.getString("proname"));
            }            
        }
        return null;
    }

    /**
     * Returns schemaName.objectName pair for the table (and similar, such as 
     * Index or View) by seeking its oid in pg_catalog.pg_class  
     * @param tableOid
     * @return
     */
    private SimpleEntry<String, String> getTableNameByOid(Long tableOid) throws SQLException{
        String query = "SELECT relname, nspname FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON c.relnamespace = n.oid WHERE c.oid = '" + tableOid + "'";
        
        try(Statement stmt = connection.createStatement();
                ResultSet res = stmt.executeQuery(query)){
            if(res.next()){
                String schemaName = res.getString("nspname");
                String tableName = res.getString("relname");
                return new SimpleEntry<String, String>(schemaName, tableName);
            }else{
                throw new IllegalStateException("Could not resolve table/index/view/etc "
                        + "schemaName.name pair for oid = " + tableOid);
            }
        }
    }

    private Long getSchemaOidByName(String schema){
        return cachedSchemaByName.get(schema);
    }
    
    private String getLangNameByOid (Long langOid) throws SQLException{
        prepStatLanguages.setLong(1, langOid);
        try(ResultSet res = prepStatLanguages.executeQuery()){
            if (res.next()){
                return res.getString("lanname");
            }
        }
        return null;
    }
    
    /**
     * Returns the name of a type, whose <code>oid = typeOid</code>. If the type's schema name 
     * differs from targetSchemaName, the returned type name is schema-qualified.
     */
    private String getTypeNameByOid(Long typeOid, String targetSchemaName) throws SQLException {
        PgType type = cachedTypeNamesByOid.get(typeOid);
        return type.getParentSchema().equals("pg_catalog") || (type.getParentSchema().equals(targetSchemaName)) ? 
                type.getTypeName() : type.getParentSchema().concat(".").concat(type.getTypeName());
    }
    
    /**
     * Returns the role name by its oid. If role oid is 0, returns "PUBLIC". 
     * If no role with such oid exists, returns null.
     */
    private String getRoleNameByOid(Long roleOid){
        return roleOid == 0 ? "PUBLIC" : cachedRolesNamesByOid.get(roleOid);
    }
    
    private String getScheNameByOid(Long schemaOid){
        Iterator<Long> iterOid = cachedSchemaByName.values().iterator();
        Iterator<String> iterNames = cachedSchemaByName.keySet().iterator();

        while(iterOid.hasNext() && iterNames.hasNext()){
            Long next = iterOid.next();
            if (next.equals(schemaOid)){
                return iterNames.next();
            }
            iterNames.next();
        }
        return null;
    }
}
