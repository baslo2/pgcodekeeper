lexer grammar CHLexer;

options {
    caseInsensitive = true;
}

@header {package ru.taximaxim.codekeeper.core.parsers.antlr.generated;}

// NOTE: don't forget to add new keywords to the parser rule "keyword"!


// case sensitive data types

AGGREGATE_FUNCTION options { caseInsensitive = false; } : 'Simple'? 'AggregateFunction';
// ARRAY_TYPE: 'Array';
ENUM               options { caseInsensitive = false; } : 'Enum' ('8'|'16') | [eE] [nN] [uU] [mM];
FIXED_STRING       options { caseInsensitive = false; } : 'FixedString';
FLOAT              options { caseInsensitive = false; } : 'Float' ('32' | '64') | [fF] [lL] [oO] [aA] [tT];
INT_TYPE           options { caseInsensitive = false; } : 'U'? 'Int' ('8' | '16' | '32' | '64' | '128' | '256');
IPV4               options { caseInsensitive = false; } : 'IPv4' | [iI] [nN] [eE] [tT] '4';
IPV6               options { caseInsensitive = false; } : 'IPv6' | [iI] [nN] [eE] [tT] '6';
LOW_CARDINALITY    options { caseInsensitive = false; } : 'LowCardinality';
MAP                options { caseInsensitive = false; } : 'Map';
MULTI_POLYGON      options { caseInsensitive = false; } : 'MultiPolygon';
NESTED             options { caseInsensitive = false; } : 'Nested';
NOTHING            options { caseInsensitive = false; } : 'Nothing';
NULLABLE           options { caseInsensitive = false; } : 'Nullable';
OBJECT_TYPE        options { caseInsensitive = false; } : 'Object';
POINT              options { caseInsensitive = false; } : 'Point';
POLYGIN            options { caseInsensitive = false; } : 'Polygon';
RING               options { caseInsensitive = false; } : 'Ring';
STRING             options { caseInsensitive = false; } : 'String';
TUPLE              options { caseInsensitive = false; } : 'Tuple';
// UUID_TYPE: 'UUID';
INTERVAL_TYPE options { caseInsensitive = false; }
    : 'IntervalDay'
    | 'IntervalHour'
    | 'IntervalMicrosecond'
    | 'IntervalMillisecond'
    | 'IntervalMinute'
    | 'IntervalMonth'
    | 'IntervalNanosecond'
    | 'IntervalQuarter'
    | 'IntervalSecond'
    | 'IntervalWeek'
    | 'IntervalYear'
    ;

// Keywords

ACCESS: 'ACCESS';
ADD: 'ADD';
ADMIN: 'ADMIN';
AFTER: 'AFTER';
ALIAS: 'ALIAS';
ALL: 'ALL';
ALTER: 'ALTER';
AND: 'AND';
ANTI: 'ANTI';
ANY: 'ANY';
APPLY: 'APPLY';
ARBITRARY: 'ARBITRARY';
ARRAY: 'ARRAY';
AS: 'AS';
ASCENDING: 'ASC' | 'ASCENDING';
ASOF: 'ASOF';
ASSUME: 'ASSUME';
AST: 'AST';
ASYNC: 'ASYNC';
ATTACH: 'ATTACH';
AUTO_INCREMENT: 'AUTO_INCREMENT';
BEGIN: 'BEGIN';
BETWEEN: 'BETWEEN';
BIGINT: 'BIGINT';
BINARY: 'BINARY';
BIT: 'BIT';
BLOB: 'BLOB';
BOOLEAN: 'BOOL' 'EAN'?;
BOTH: 'BOTH';
BY: 'BY';
BYTE: 'BYTE';
BYTEA: 'BYTEA';
CACHE: 'CACHE';
CACHES: 'CACHES';
CASE: 'CASE';
CAST: 'CAST';
CHANGED: 'CHANGED';
CHANGEABLE_IN_READONLY: 'CHANGEABLE_IN_READONLY';
CHAR: 'CHAR';
CHARACTER: 'CHARACTER';
CHECK: 'CHECK';
CLEAR: 'CLEAR';
CLOB: 'CLOB';
CLUSTER: 'CLUSTER';
CLUSTERS: 'CLUSTERS';
CN: 'CN';
CODEC: 'CODEC';
COLLATE: 'COLLATE';
COLLECTION: 'COLLECTION';
COLUMN: 'COLUMN';
COLUMNS: 'COLUMNS';
COMMENT: 'COMMENT';
COMMIT: 'COMMIT';
CONFIG: 'CONFIG';
CONST: 'CONST';
CONSTRAINT: 'CONSTRAINT';
CREATE: 'CREATE';
CROSS: 'CROSS';
CUBE: 'CUBE';
CURRENT: 'CURRENT';
CURRENT_USER: 'CURRENT_USER';
DATABASE: 'DATABASE';
DATABASES: 'DATABASES';
DATE: 'DATE' '32'?;
DATETIME64: 'DATETIME64';
DATETIME: 'DATETIME' '32'?;
DAY: 'DAY' 'S'?;
DECIMAL: 'DEC' 'IMAL'?;
DECIMAL_BIT: 'DECIMAL' ('8' | '16' | '32' | '64' | '128' | '256');
DEDUPLICATE: 'DEDUPLICATE';
DEFAULT: 'DEFAULT';
DEFINER: 'DEFINER';
DELAY: 'DELAY';
DELETE: 'DELETE';
DELETED: 'DELETED';
DESC: 'DESC';
DESCENDING: 'DESCENDING';
DESCRIBE: 'DESCRIBE';
DETACH: 'DETACH';
DETACHED: 'DETACHED';
DICTGET: 'DICTGET';
DICTIONARIES: 'DICTIONARIES';
DICTIONARY: 'DICTIONARY';
DISK: 'DISK';
DISTINCT: 'DISTINCT';
DISTRIBUTED: 'DISTRIBUTED';
DIV : 'DIV';
DNS: 'DNS';
DOUBLE: 'DOUBLE';
DROP: 'DROP';
ELSE: 'ELSE';
EMPTY: 'EMPTY';
ENABLED: 'ENABLED';
END: 'END';
ENGINE: 'ENGINE';
ENGINES: 'ENGINES';
EMBEDDED: 'EMBEDDED';
EPHEMERAL: 'EPHEMERAL';
ESTIMATE: 'ESTIMATE';
EVENTS: 'EVENTS';
EXCEPT: 'EXCEPT';
EXISTS: 'EXISTS';
EXPLAIN: 'EXPLAIN';
EXPRESSION: 'EXPRESSION';
EXTENDED: 'EXTENDED';
EXTRACT: 'EXTRACT';
FETCH: 'FETCH';
FETCHES: 'FETCHES';
FIELDS: 'FIELDS';
FILE: 'FILE';
FILESYSTEM: 'FILESYSTEM';
FILL: 'FILL';
FINAL: 'FINAL';
FIRST: 'FIRST';
FIXED: 'FIXED';
FLUSH: 'FLUSH';
FOLLOWING: 'FOLLOWING';
FOR: 'FOR';
FORMAT: 'FORMAT';
FREEZE: 'FREEZE';
FROM: 'FROM';
FULL: 'FULL';
FUNCTION: 'FUNCTION';
FUNCTIONS: 'FUNCTIONS';
GEOMETRY: 'GEOMETRY';
GLOBAL: 'GLOBAL';
GRANT: 'GRANT';
GRANTEES: 'GRANTEES';
GRANTS: 'GRANTS';
GRANULARITY: 'GRANULARITY';
GROUP: 'GROUP';
GROUPING: 'GROUPING';
HAVING: 'HAVING';
HDFS: 'HDFS';
HIERARCHICAL: 'HIERARCHICAL';
HOST: 'HOST';
HOUR: 'HOUR' 'S'?;
ID: 'ID';
IDENTIFIED: 'IDENTIFIED';
IF: 'IF';
ILIKE: 'ILIKE';
IN: 'IN';
INDEX: 'INDEX';
INDEXES: 'INDEXES';
INDICES: 'INDICES';
INF: 'INF';
INJECTIVE: 'INJECTIVE';
INNER: 'INNER';
INSERT: 'INSERT';
INT: 'INT' '1'?;
INTEGER: 'INTEGER';
INTERPOLATE: 'INTERPOLATE';
INTERSECT: 'INTERSECT';
INTERVAL: 'INTERVAL';
INTO: 'INTO';
INTROSPECTION: 'INTROSPECTION';
INVOKER: 'INVOKER';
IS: 'IS';
IS_OBJECT_ID: 'IS_OBJECT_ID';
IP: 'IP';
JDBC: 'JDBC';
JOIN: 'JOIN';
JSON: 'JSON';
KEY: 'KEY';
KEYS: 'KEYS';
KILL: 'KILL';
LARGE: 'LARGE';
LAST: 'LAST';
LAYOUT: 'LAYOUT';
LEADING: 'LEADING';
LEFT: 'LEFT';
LIFETIME: 'LIFETIME';
LIKE: 'LIKE';
LIMIT: 'LIMIT';
LIVE: 'LIVE';
LOCAL: 'LOCAL';
LOGS: 'LOGS';
LONGBLOB: 'LONGBLOB';
LONGTEXT: 'LONGTEXT';
MANAGEMENT: 'MANAGEMENT';
MARK: 'MARK';
MASK: 'MASK';
MATERIALIZE: 'MATERIALIZE';
MATERIALIZED: 'MATERIALIZED';
MAX: 'MAX';
MEDIUMBLOB: 'MEDIUMBLOB';
MEDIUMINT: 'MEDIUMINT';
MEDIUMTEXT: 'MEDIUMTEXT';
MERGES: 'MERGES';
MICROSECOND: 'MICROSECOND' 'S'?;
MILLISECOND: 'MILLISECOND' 'S'?;
MIN: 'MIN';
MINUTE: 'MINUTE' 'S'?;
MOD : 'MOD';
MODIFY: 'MODIFY';
MONTH: 'MONTH' 'S'?;
MOVE: 'MOVE';
MOVES: 'MOVES';
MUTATION: 'MUTATION';
MYSQL: 'MYSQL';
NAME: 'NAME';
NAMED: 'NAMED';
NAN: 'NAN';
NANOSECOND: 'NANOSECOND' 'S'?;
NATIONAL: 'NATIONAL';
NCHAR: 'NCHAR';
NO: 'NO';
NONE: 'NONE';
NOT: 'NOT';
NULL: 'NULL';
NULLS: 'NULLS';
NUMERIC: 'NUMERIC';
NVARCHAR: 'NVARCHAR';
OBJECT: 'OBJECT';
ODBC: 'ODBC';
OFFSET: 'OFFSET';
ON: 'ON';
OPTIMIZE: 'OPTIMIZE';
OPTION: 'OPTION';
OR: 'OR';
ORDER: 'ORDER';
OUTER: 'OUTER';
OUTFILE: 'OUTFILE';
OVER: 'OVER';
OVERRIDABLE: 'OVERRIDABLE';
OVERRIDE: 'OVERRIDE';
PART: 'PART';
PARTITION: 'PARTITION';
PASTE: 'PASTE';
PERIODIC: 'PERIODIC';
PERMANENTLY : 'PERMANENTLY';
PERMISSIVE: 'PERMISSIVE';
PIPELINE: 'PIPELINE';
PLAN: 'PLAN';
POLICIES: 'POLICIES';
POLICY: 'POLICY';
POPULATE: 'POPULATE';
PRECEDING: 'PRECEDING';
PRECISION: 'PRECISION';
PREWHERE: 'PREWHERE';
PRIMARY: 'PRIMARY';
PRIVILEGES: 'PRIVILEGES';
PROCESSLIST: 'PROCESSLIST';
PROFILE: 'PROFILE';
PROFILES: 'PROFILES';
PROJECTION: 'PROJECTION';
QUARTER: 'QUARTER' 'S'?;
QUERY: 'QUERY';
QUOTA: 'QUOTA';
QUOTAS: 'QUOTAS';
QUEUES: 'QUEUES';
RANGE: 'RANGE';
READONLY: 'READONLY';
REAL: 'REAL';
REALM: 'REALM';
RECOMPRESS: 'RECOMPRESS';
REFRESH: 'REFRESH';
REGEXP: 'REGEXP';
RELOAD: 'RELOAD';
REMOTE: 'REMOTE';
REMOVE: 'REMOVE';
RENAME: 'RENAME';
REPLACE: 'REPLACE';
REPLICA: 'REPLICA';
REPLICATED: 'REPLICATED';
REPLICATION: 'REPLICATION';
RESET: 'RESET';
RESTART: 'RESTART';
RESTRICTIVE: 'RESTRICTIVE';
REVOKE: 'REVOKE';
RIGHT: 'RIGHT';
ROLE: 'ROLE';
ROLES: 'ROLES';
ROLLBACK: 'ROLLBACK';
ROLLUP: 'ROLLUP';
ROW: 'ROW';
ROWS: 'ROWS';
SAMPLE: 'SAMPLE';
SECOND: 'SECOND' 'S'?;
SECURITY: 'SECURITY';
SELECT: 'SELECT';
SEMI: 'SEMI';
SENDS: 'SENDS';
SERVER : 'SERVER';
SET: 'SET';
SETS: 'SETS';
SETTING: 'SETTING';
SETTINGS: 'SETTINGS';
SHOW: 'SHOW';
SHUTDOWN: 'SHUTDOWN';
SIGNED: 'SIGNED';
SINGLE: 'SINGLE';
SMALLINT: 'SMALLINT';
SOURCE: 'SOURCE';
SOURCES: 'SOURCES';
SQL: 'SQL';
START: 'START';
STATISTIC: 'STATISTIC';
STEP: 'STEP';
STOP: 'STOP';
SUBSTRING: 'SUBSTRING';
SYNC: 'SYNC';
SYNTAX: 'SYNTAX';
SYSTEM: 'SYSTEM';
TABLE: 'TABLE';
TABLES: 'TABLES';
TEMPORARY: 'TEMPORARY';
TEST: 'TEST';
TEXT: 'TEXT';
THEN: 'THEN';
TIES: 'TIES';
TIME: 'TIME';
TIMEOUT: 'TIMEOUT';
TIMESTAMP: 'TIMESTAMP';
TINYBLOB: 'TINYBLOB';
TINYINT: 'TINYINT';
TINYTEXT: 'TINYTEXT';
TO: 'TO';
TOP: 'TOP';
TOTALS: 'TOTALS';
TRAILING: 'TRAILING';
TRANSACTION: 'TRANSACTION';
TREE: 'TREE';
TRIM: 'TRIM';
TRUNCATE: 'TRUNCATE';
TTL: 'TTL';
TYPE: 'TYPE';
UNBOUNDED: 'UNBOUNDED';
UNFREEZE: 'UNFREEZE';
UNCOMPRESSED: 'UNCOMPRESSED';
UNION: 'UNION';
UNSIGNED: 'UNSIGNED';
UNTIL: 'UNTIL';
UPDATE: 'UPDATE';
URL: 'URL';
USAGE: 'USAGE';
USE: 'USE';
USER: 'USER';
USERS: 'USERS';
USING: 'USING';
UUID: 'UUID';
VALID: 'VALID';
VALUES: 'VALUES';
VARBINARY: 'VARBINARY';
VARCHAR: 'VARCHAR' '2'?;
VARYING: 'VARYING';
VIEW: 'VIEW';
VOLUME: 'VOLUME';
WATCH: 'WATCH';
WEEK: 'WEEK' 'S'?;
WHEN: 'WHEN';
WHERE: 'WHERE';
WINDOW: 'WINDOW';
WITH: 'WITH';
WRITABLE: 'WRITABLE';
YEAR: 'YEAR' 'S'? | 'YYYY';

// Tokens

IDENTIFIER
    : (LETTER | UNDERSCORE) (LETTER | UNDERSCORE | DEC_DIGIT)*
    ;

GLOBAL_VARIABLE: '@''@' IDENTIFIER;

FLOATING_LITERAL
    : NUMBER+ '.' EXPONENT?
    | NUMBER+ '.' NUMBER+ EXPONENT?
//  | '.' NUMBER+ EXPONENT?
    | NUMBER+ EXPONENT
    ;

NUMBER
    : '0' 'X' HEX_DIGIT (HEX_DIGIT | UNDERSCORE)*
    | DEC_DIGIT (DEC_DIGIT | UNDERSCORE)*
    ;

// It's important that quote-symbol is a single character.
STRING_LITERAL: QUOTE_SINGLE ( ~([\\']) | (BACKSLASH .) | (QUOTE_SINGLE QUOTE_SINGLE) )* QUOTE_SINGLE;
BINARY_LITERAL: 'X' QUOTE_SINGLE HEX_DIGIT* QUOTE_SINGLE;
// DOLLAR_LITERAL: '$' IDENTIFIER? '$' (DOLLAR_LITERAL |.)*? '$' IDENTIFIER? '$';

// Alphabet and allowed symbols

fragment LETTER: [A-Z];
fragment DEC_DIGIT: [0-9];
fragment HEX_DIGIT: [0-9A-F];

fragment EXPONENT : 'E' ('+'|'-')? DEC_DIGIT+;

ARROW: '->';
ASTERISK: '*';
BACKQUOTE: '`';
BACKSLASH: '\\';
COLON: ':';
COMMA: ',';
CONCAT: '||';
MINUS: '-';
DOT: '.';
EQ_DOUBLE: '==';
EQ_SINGLE: '=';
GE: '>=';
GT: '>';
LBRACE: '{';
LBRACKET: '[';
LE: '<=';
LPAREN: '(';
LT: '<';
NOT_EQ: '!=' | '<>';
PERCENT: '%';
PLUS: '+';
QUESTION: '?';
QUOTE_DOUBLE: '"';
QUOTE_SINGLE: '\'';
RBRACE: '}';
RBRACKET: ']';
RPAREN: ')';
SEMICOLON: ';';
SLASH: '/';
UNDERSCORE: '_';
CAST_EXPRESSION: ':'':';
NOT_DIST: '<' '=' '>';

BOM: '\ufeff';

// Comments and whitespace

BLOCK_COMMENT: '/*' (BLOCK_COMMENT |.)*? '*/' -> channel(HIDDEN);
LINE_COMMENT: '--' ~[\r\n]* -> channel(HIDDEN);

SPACE: ' ' -> channel(HIDDEN);
WHITESPACE: [\u000B\u000C] -> channel(HIDDEN);
NEW_LINE : [\n\r] -> channel(HIDDEN);
TAB : '\t' -> channel(HIDDEN);

/* Quoted Identifiers
*
* These are divided into four separate tokens, allowing distinction of valid quoted identifiers from invalid quoted
* identifiers without sacrificing the ability of the lexer to reliably recover from lexical errors in the input.
*/
DOUBLE_QUOTED_IDENTIFIER
    : UNTERMINATED_DOUBLE_QUOTED_IDENTIFIER '"'
    // unquote so that we may always call getText() and not worry about quotes
        {
            String __tx = getText();
            setText(__tx.substring(1, __tx.length() - 1).replace("\"\"", "\""));
        }
    ;

BACK_QUOTED_IDENTIFIER
    : UNTERMINATED_BACK_QUOTED_IDENTIFIER '`'
    // unquote so that we may always call getText() and not worry about quotes
        {
            String __tx = getText();
            setText(__tx.substring(1, __tx.length() - 1).replace("``", "`"));
        }
    ;

// This is a quoted identifier which only contains valid characters but is not terminated
fragment UNTERMINATED_BACK_QUOTED_IDENTIFIER
    : '`' ( '``' | ~[\u0000`] )*
    ;

fragment UNTERMINATED_DOUBLE_QUOTED_IDENTIFIER
    : '"' ( '""' | ~[\u0000"] )*
    ;