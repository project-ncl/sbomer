grammar Query;

query: WS* statement (WS+ statement)* WS* EOF;
statement: term | sort;
term: MINUS? atom;
sort: (direction=(SORT | SORT_ASC | SORT_DESC)) COLON field=WORD;
atom: WORD COLON value_list;

value_list: value (COMMA value)*;
value: op=(GT | LT | GTE | LTE | CONTAINS)? (WORD | STRING);
// --- LEXER RULES ---

SORT: 'sort';
SORT_ASC: 'sort-asc';
SORT_DESC: 'sort-desc';

COLON: ':';
MINUS: '-';
COMMA: ',';
ASC: 'asc';
DESC: 'desc';
GT: '>';
LT: '<';
GTE: '>=';
LTE: '<=';
CONTAINS: '~';

// UNIFIED TOKEN: Can be a key or a value. Can start with a letter or number.
WORD: [a-zA-Z0-9_][a-zA-Z0-9\-_]*;
STRING: '"' ( '\\"' | ~'"' )*? '"';

WS: [ \t\r\n]+;
