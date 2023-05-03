package parser

import ErrorReporter
import ast.Expr
import ast.Stmt

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
        reporter.error(token.position, message)
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

    fun parse(): List<Stmt> {
        val statements: MutableList<Stmt> = ArrayList()

        while (!isAtEnd()) {
            val decl = declaration()
            if (decl != null) statements.add(decl)
        }

        if (statements.isEmpty()) {
            reporter.clear()
            current = 0
            try {
                val expr = expression()
                statements.add(Stmt.Expression(expr))
            } catch (_: ParseError) {
            }
        }

        return statements
    }


    // Recursive Descent Grammar

    private fun declaration(): Stmt? {
        try {
            if (match(TokenType.VAR)) return varDeclaration()
            return statement()
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expected variable name.")
        val initializer: Expr? = if (match(TokenType.EQUAL)) {
            expression()
        } else {
            null
        }
        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt {
        if (match(TokenType.PRINT)) return printStatement()
        if (match(TokenType.LEFT_BRACE)) return Stmt.Block(block())

        return expressionStatement()
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expected ; after expression.")
        return Stmt.Print(value)
    }

    private fun block(): List<Stmt> {
        val statements: MutableList<Stmt> = ArrayList()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val decl = declaration()
            if (decl != null) statements.add(decl)
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after block.")
        return statements
    }

    private fun expressionStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expected ; after expression.")
        return Stmt.Expression(value)
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        val expr = equality()
        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()
            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }
            throw parseError(equals, "Invalid assignment target.")
        }
        return expr
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

        if (match(TokenType.IDENTIFIER)) {
            return Expr.Variable(previous())
        }

        if (match(TokenType.LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.")
            return Expr.Grouping(expr)
        }

        throw parseError(peek(), "Expected expression.")
    }

}