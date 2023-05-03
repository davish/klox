package parser

import ErrorReporter
import ast.Expr

class Parser(private val tokens: List<Token>, private val reporter: ErrorReporter) {
    private var current = 0

    private class ParseError : RuntimeException()

    private fun isAtEnd() = peek().type == TokenType.EOF
    private fun peek() = tokens[current]
    private fun previous() = tokens[current - 1]

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type))
            return advance()
        throw parseError(peek(), message)
    }

    private fun parseError(token: Token, message: String): ParseError {
        reporter.parseError(token.position, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return

            when (peek().type) {
                TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF, TokenType.WHILE, TokenType.PRINT, TokenType.RETURN -> return
                else -> advance()
            }
        }
    }

    fun parse(): Expr? {
        val result = try {
            expression()
        } catch (error: ParseError) {
            null
        }

        if (peek().type != TokenType.EOF) {
            reporter.parseError(peek().position, "Unknown tokens at the end of the input.")
        }

        return result
    }


    // Recursive Descent Grammar

    private fun expression(): Expr {
        return equality()
    }

    /**
     * Helper function to generate a precedence class for left-associative binary operators.
     */
    private fun binaryOp(higher: () -> Expr, vararg tokens: TokenType) = fun(): Expr {
        var expr = higher()
        while (match(*tokens)) {
            val operator = previous()
            val right = higher()
            expr = Expr.BinaryOp(expr, operator, right)
        }
        return expr
    }

    private val equality = binaryOp({ comparison() }, TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)
    private val comparison =
        binaryOp({ term() }, TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)
    private val term = binaryOp({ factor() }, TokenType.MINUS, TokenType.PLUS)
    private val factor = binaryOp({ unary() }, TokenType.STAR, TokenType.SLASH)
    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.UnaryOp(operator, right)
        }
        return primary()
    }

    private fun primary(): Expr {
        if (match(TokenType.FALSE)) return Expr.Literal(false)
        if (match(TokenType.TRUE)) return Expr.Literal(true)
        if (match(TokenType.NIL)) return Expr.Literal(null)

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return Expr.Literal(previous().literal)
        }

        if (match(TokenType.LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.")
            return Expr.Grouping(expr)
        }
        throw parseError(peek(), "Expected expression.")
    }

}