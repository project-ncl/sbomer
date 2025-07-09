grammar Query;


@header {
package org.jboss.sbomer.service.nextgen.antlr;
}

// use 'antlr4 Query.g4 -listener -o antlr' to generate classes


query: expression;

expression
    : LPAREN expression RPAREN ( ( AND | OR ) expression )?
    | predicate ( ( AND | OR ) expression )?
    ;

predicate
    : IDENTIFIER (EQUAL | NOT_EQUAL | GREATER_THAN | LESS_THAN | GREATER_THAN_OR_EQUAL | LESS_THAN_OR_EQUAL | CONTAINS) value
    ;

value
    : STRING
    | NUMBER
    ;

// Lexer Rules
AND: 'AND' | 'and';
OR: 'OR' | 'or';
LPAREN: '(';
RPAREN: ')';
EQUAL: '=';
NOT_EQUAL: '!=';
GREATER_THAN: '>';
LESS_THAN: '<';
GREATER_THAN_OR_EQUAL: '>=';
LESS_THAN_OR_EQUAL: '<=';
CONTAINS: '~';

IDENTIFIER: [a-zA-Z_] [a-zA-Z0-9_]*;
STRING: '"' ( ~'"' )* '"';
NUMBER: [0-9]+ ('.' [0-9]+)?;
WS: [ \t\r\n]+ -> skip;
