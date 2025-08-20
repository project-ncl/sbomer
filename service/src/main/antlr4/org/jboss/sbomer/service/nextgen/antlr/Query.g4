grammar Query;

query: term+ EOF;
term: MINUS? atom;
atom
    : IDENTIFIER COLON value_list // key:value
    | value                        // standalone value
    ;

value_list: value (COMMA value)*;
value: op=(GT | LT | GTE | LTE)? (IDENTIFIER | STRING);

// --- LEXER RULES ---

COLON: ':';
MINUS: '-';
COMMA: ',';
GT: '>';
LT: '<';
GTE: '>=';
LTE: '<=';

// UNIFIED TOKEN: Can be a key or a value. Can start with a letter or number.
IDENTIFIER: [a-zA-Z0-9_][a-zA-Z0-9_.-]*;

STRING: '"' ( '\\"' | ~'"' )*? '"';
WS: [ \t\r\n]+ -> skip;
