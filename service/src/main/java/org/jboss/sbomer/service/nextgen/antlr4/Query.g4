grammar Query;

@header {
package org.jboss.sbomer.service.nextgen.antlr;
}

query: expression;

expression
    : LPAREN expression RPAREN ( ( AND | OR ) expression )?
    | predicate ( ( AND | OR ) expression )?
    ;

predicate
    : IDENTIFIER (EQUAL | NOT_EQUAL | GREATER_THAN | LESS_THAN | GREATER_THAN_OR_EQUAL | LESS_THAN_OR_EQUAL | CONTAINS) value
    ;

value: STRING_IN_QUOTES;

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
STRING_IN_QUOTES: '"' ( ~'"' )* '"';

WS: [ \t\r\n]+ -> skip;

UNEXPECTED_CHAR: . ;
