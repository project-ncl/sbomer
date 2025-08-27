grammar Query;

query: statement (WS+ statement)* WS* EOF;
statement: term | sort;
term: MINUS? atom;
sort: SORT COLON sort_field;
sort_field: IDENTIFIER | IDENTIFIER MINUS DIRECTION;
atom: IDENTIFIER COLON value_list;

value_list: value (COMMA value)*;
value: op=(GT | LT | GTE | LTE | CONTAINS)? (IDENTIFIER | STRING);

// --- LEXER RULES ---

COLON: ':';
MINUS: '-';
COMMA: ',';
SORT: 'sort';
DIRECTION: ('asc' | 'desc');
GT: '>';
LT: '<';
GTE: '>=';
LTE: '<=';
CONTAINS: '~';

// UNIFIED TOKEN: Can be a key or a value. Can start with a letter or number.
IDENTIFIER: [a-zA-Z0-9_][a-zA-Z0-9\-]*;

STRING: '"' ( '\\"' | ~'"' )*? '"';

WS: [ \t\r\n]+;
