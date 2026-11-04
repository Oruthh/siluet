package dev.siluet.lexer

internal enum class TokenType {
    TEXT,
    OPEN_EXPR,
    CLOSE_EXPR,
    AT,
    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    PIPE,

    IDENTIFIER,
    STRING_LITERAL,
    NUMBER_LITERAL,
    BOOLEAN_LITERAL,
    NULL_LITERAL,

    KEYWORD_IN,
    KEYWORD_BY,
    KEYWORD_AS,
    EQUALS,
    NOT_EQUALS,
    LESS_THAN,
    LESS_OR_EQUAL,
    GREATER_THAN,
    GREATER_OR_EQUAL,
    AND,
    OR,
    NOT,
    DOT,
    COMMA,
    ASSIGN,

    EOF
}