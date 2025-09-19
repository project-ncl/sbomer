grammar Query;

query: WS* statement (WS+ statement)* WS* EOF;
statement: term | sort;
term: MINUS? atom;
sort: SORT COLON field=WORD (COLON direction=(ASC | DESC))?;
atom: qualified_field COLON value_list;
qualified_field: WORD (DOT WORD)?;

value_list: value (COMMA value)*;
value: op=(GT | LT | GTE | LTE | CONTAINS)? (WORD | STRING);

// --- LEXER RULES ---

SORT: 'sort';

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

DOT: '.';

// UNIFIED TOKEN: Can be a key or a value. Can start with a letter or number.
WORD: [a-zA-Z0-9_][a-zA-Z0-9\-_]*;
STRING: '"' ( '\\"' | ~'"' )*? '"';

WS: [ \t\r\n]+;
