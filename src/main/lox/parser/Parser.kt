package parser

import ErrorReporter
import ast.Expr
import ast.Stmt

class Parser(private val tokens: List<Token>, private val reporter: ErrorReporter) {
    private var current = 0
    private var loopDepth = 0

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
            if (match(TokenType.FUN)) return functionDeclaration("function")
            if (match(TokenType.VAR)) return varDeclaration()
            return statement()
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun functionDeclaration(kind: String): Stmt.Function {
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters: MutableList<Token> = ArrayList()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    reporter.error(peek().position, "Can't have more than 255 parameters.");
                }

                parameters.add(
                    consume(TokenType.IDENTIFIER, "Expect parameter name.")
                )
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")
        consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block()
        return Stmt.Function(name, parameters, body)
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
        if (match(TokenType.BREAK)) return breakStatement()
        if (match(TokenType.FOR)) return forStatement()
        if (match(TokenType.IF)) return ifStatement()
        if (match(TokenType.PRINT)) return printStatement()
        if (match(TokenType.RETURN)) return returnStatement()
        if (match(TokenType.WHILE)) return whileStatement()
        if (match(TokenType.LEFT_BRACE)) return Stmt.Block(block())

        return expressionStatement()
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'for'.")
        val initializer =
            if (match(TokenType.SEMICOLON)) {
                null
            } else if (match(TokenType.VAR)) {
                varDeclaration()
            } else {
                expressionStatement()
            }

        val condition = if (!check(TokenType.SEMICOLON)) {
            expression()
        } else {
            Expr.Literal(true)
        }
        consume(TokenType.SEMICOLON, "Expected ';' after loop condition.")

        val increment = if (!check(TokenType.RIGHT_PAREN)) {
            expression()
        } else {
            null
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after for clause.")

        loopDepth++
        var body = statement()
        loopDepth--
        if (increment != null) {
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        }
        body = Stmt.While(condition, body)
        if (initializer != null) {
            body = Stmt.Block(listOf(initializer, body))
        }

        return body
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after if.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expected ') after condition")
        val thenBranch = statement()
        val elseBranch = if (match(TokenType.ELSE)) {
            statement()
        } else {
            null
        }
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expected ; after expression.")
        return Stmt.Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (!check(TokenType.SEMICOLON)) {
            expression()
        } else {
            null
        }
        consume(TokenType.SEMICOLON, "Expected ';' after return value.")
        return Stmt.Return(keyword, value)
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'while'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after condition.")
        loopDepth++
        val body = statement()
        loopDepth--
        return Stmt.While(condition, body)
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

    private fun breakStatement(): Stmt {
        if (loopDepth < 1) {
            parseError(previous(), "Can only use break statement within a loop.")
        }
        consume(TokenType.SEMICOLON, "Expected ; after break.")
        return Stmt.Break
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        val expr = or()
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
    private fun leftAssociative(con: (Expr, Token, Expr) -> Expr, higher: () -> Expr, vararg tokens: TokenType) =
        fun(): Expr {
            var expr = higher()
            while (match(*tokens)) {
                val operator = previous()
                val right = higher()
                expr = con(expr, operator, right)
            }
            return expr
        }

    private val or = leftAssociative(Expr::Logical, { and() }, TokenType.OR)
    private val and = leftAssociative(Expr::Logical, { equality() }, TokenType.AND)

    private val equality =
        leftAssociative(Expr::BinaryOp, { comparison() }, TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)
    private val comparison =
        leftAssociative(
            Expr::BinaryOp,
            { term() },
            TokenType.GREATER,
            TokenType.GREATER_EQUAL,
            TokenType.LESS,
            TokenType.LESS_EQUAL
        )
    private val term = leftAssociative(Expr::BinaryOp, { factor() }, TokenType.MINUS, TokenType.PLUS)
    private val factor = leftAssociative(Expr::BinaryOp, { unary() }, TokenType.STAR, TokenType.SLASH)
    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.UnaryOp(operator, right)
        }
        return call()
    }

    private fun call(): Expr {
        var expr = primary()
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            } else {
                break;
            }
        }
        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments: MutableList<Expr> = ArrayList()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    reporter.error(peek().position, "Can't have more than 255 arguments.")
                }
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }
        val paren = consume(TokenType.RIGHT_PAREN, "Expected ')' after argument list.")
        return Expr.Call(callee, paren, arguments)
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