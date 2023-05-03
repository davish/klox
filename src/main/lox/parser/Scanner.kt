package parser

import ErrorReporter
import Position
import at
import point
import startLocation

class Scanner(private val source: String, private val reporter: ErrorReporter) {
    private val tokens: MutableList<Token> = ArrayList()

    private var start = startLocation()
    private var current = startLocation()

    private val keywords: MutableMap<String, TokenType> = hashMapOf(
        "and" to TokenType.AND,
        "class" to TokenType.CLASS,
        "else" to TokenType.ELSE,
        "false" to TokenType.FALSE,
        "for" to TokenType.FOR,
        "fun" to TokenType.FUN,
        "if" to TokenType.IF,
        "nil" to TokenType.NIL,
        "or" to TokenType.OR,
        "print" to TokenType.PRINT,
        "return" to TokenType.RETURN,
        "super" to TokenType.SUPER,
        "this" to TokenType.THIS,
        "true" to TokenType.TRUE,
        "var" to TokenType.VAR,
        "while" to TokenType.WHILE,
    )

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current;
            scanToken()
        }

        this.tokens.add(Token(TokenType.EOF, "", null, point(current)))
        return this.tokens
    }

    private fun isAtEnd() = current.offset >= source.length

    private fun scanToken() {
        val c = advance()
        when (c) {
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)
            '!' -> addToken(
                if (match('=')) {
                    TokenType.BANG_EQUAL
                } else {
                    TokenType.BANG
                }
            )

            '=' -> addToken(
                if (match('=')) {
                    TokenType.EQUAL_EQUAL
                } else {
                    TokenType.EQUAL
                }
            )

            '<' -> addToken(
                if (match('=')) {
                    TokenType.LESS_EQUAL
                } else {
                    TokenType.LESS
                }
            )

            '>' -> addToken(
                if (match('=')) {
                    TokenType.GREATER_EQUAL
                } else {
                    TokenType.GREATER
                }
            )

            '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance()
                } else if (match('*')) {
                    while (!(peek() == '*' && peekNext() == '/')) advance()
                    advance()
                    advance()
                } else {
                    addToken(TokenType.SLASH)
                }
            }

            // Whitespace
            ' ', '\r', '\t', '\n' -> {}

            '"' -> string()

            else -> {
                if (isDigit(c)) {
                    number()
                } else if (isAlpha(c)) {
                    identifier()
                } else {
                    reporter.parseError(pos(), "Unexpected character '$c'.")
                }
            }
        }
    }

    private fun isDigit(c: Char) = c in '0'..'9';
    private fun isAlpha(c: Char) = (c in 'a'..'z') ||
            (c in 'A'..'Z') ||
            c == '_';

    private fun isAlphaNumeric(c: Char) = isDigit(c) || isAlpha(c)

    private fun advance(): Char {
        val c = source.at(current)
        current = current.increment(source.at(current))
        return c
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source.at(current) != expected) return false
        advance()
        return true;
    }

    private fun peek(): Char {
        if (isAtEnd()) return Char(0)
        return source.at(current)
    }

    private fun peekNext(): Char {
        if (current.offset + 1 >= source.length) return Char(0)
        return source[current.offset + 1]
    }


    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            advance()
        }
        if (isAtEnd()) {
            reporter.parseError(point(current), "Unterminated string.")
            return
        }
        advance()
        val value = source.at(pos())
        addToken(TokenType.STRING, value.substring(1 until value.length - 1))
    }

    private fun number() {
        while (isDigit(peek())) advance()
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance()
        }
        addToken(TokenType.NUMBER, source.at(pos()).toDouble())
    }

    private fun identifier() {
        while (isAlphaNumeric(peek())) {
            advance()
        }
        val text = source.at(pos())
        addToken(keywords[text.lowercase()] ?: TokenType.IDENTIFIER)
    }

    private fun addToken(type: TokenType) {
        addToken(type, null)
    }

    private fun addToken(type: TokenType, literal: Any?) {
        val text = source.at(pos())
        tokens.add(Token(type, text, literal, pos()))
    }

    private fun pos() = Position(start, current)
}