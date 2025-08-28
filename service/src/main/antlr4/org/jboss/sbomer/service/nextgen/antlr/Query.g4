grammar Query;

query: WS* statement (WS+ statement)* WS* EOF;
statement: term | sort;
term: MINUS? atom;
sort: SORT COLON sort_field;
sort_field: WORD | WORD MINUS DIRECTION;
atom: WORD COLON value_list;

value_list: value (COMMA value)*;
value: op=(GT | LT | GTE | LTE | CONTAINS)? (WORD | STRING);
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
WORD: [a-zA-Z0-9_][a-zA-Z0-9\-_]*;
STRING: '"' ( '\\"' | ~'"' )*? '"';

WS: [ \t\r\n]+;
