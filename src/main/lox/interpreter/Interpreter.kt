package interpreter

import ErrorReporter
import LoxError
import ast.Expr
import ast.Stmt
import parser.Token
import parser.TokenType


class Interpreter() {
    var reporter: ErrorReporter = ErrorReporter("")
    private var environment = Environment()

    fun interpret(statements: List<Stmt>) {
        try {
            for (stmt in statements) {
                execute(stmt)
            }
        } catch (error: RuntimeError) {
            reporter.error(error)
        }
    }

    fun execute(stmt: Stmt): Unit = when (stmt) {
        is Stmt.Block -> executeBlock(stmt.statements, Environment(environment))
        is Stmt.Expression -> {
            evaluate(stmt.expression)
            Unit
        }

        is Stmt.Print -> {
            val value = evaluate(stmt.expression)
            println(stringify(value))
        }

        is Stmt.Var -> {
            val value = stmt.initializer?.let { evaluate(it) }
            environment.define(stmt.name.lexeme, value)
        }
    }

    private fun stringify(obj: Any?): String = when (obj) {
        null -> "nil"
        is Double -> {
            val text = obj.toString()
            if (text.endsWith(".0")) {
                text.substring(0 until text.length - 2)
            } else {
                text
            }
        }

        else -> obj.toString()
    }

    private fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment
            for (statement in statements) {
                execute(statement)
            }
        } finally {
            this.environment = previous
        }
    }

    fun evaluateToStr(expr: Expr): String? {
        return try {
            val value = evaluate(expr)
            stringify(value)
        } catch (error: RuntimeError) {
            reporter.error(error)
            null
        }
    }

    private fun evaluate(expr: Expr): Any? = when (expr) {
        is Expr.Literal -> expr.value
        is Expr.Grouping -> evaluate(expr.expression)
        is Expr.Variable -> environment.get(expr.name)
        is Expr.Assign -> {
            val value = evaluate(expr.value)
            environment.assign(expr.name, value)
            value
        }

        is Expr.UnaryOp -> {
            val right = evaluate(expr.right)
            when (expr.operator.type) {
                TokenType.BANG -> !isTruthy(right)
                TokenType.MINUS -> {
                    checkNumberOperand(expr.operator, right)
                    -(right as Double)
                }

                else -> null
            }
        }

        is Expr.BinaryOp -> {
            val left = evaluate(expr.left)
            val right = evaluate(expr.right)

            when (expr.operator.type) {
                TokenType.MINUS -> {
                    checkNumberOperands(expr.operator, left, right)
                    left as Double - right as Double
                }

                TokenType.SLASH -> {
                    checkNumberOperands(expr.operator, left, right)
                    left as Double / right as Double
                }

                TokenType.STAR -> {
                    checkNumberOperands(expr.operator, left, right)
                    left as Double * right as Double
                }

                TokenType.PLUS -> {
                    if (left is Double && right is Double) {
                        left + right
                    } else if (left is String && right is String) {
                        left + right
                    } else {
                        throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.")
                    }
                }

                TokenType.GREATER -> {
                    checkNumberOperands(expr.operator, left, right)
                    left as Double > right as Double
                }

                TokenType.GREATER_EQUAL -> {
                    checkNumberOperands(expr.operator, left, right)
                    left as Double >= right as Double
                }

                TokenType.LESS -> {
                    checkNumberOperands(expr.operator, left, right)
                    (left as Double) < right as Double
                }

                TokenType.LESS_EQUAL -> {
                    checkNumberOperands(expr.operator, left, right)
                    left as Double <= right as Double
                }

                TokenType.BANG_EQUAL -> !isEqual(left, right)
                TokenType.EQUAL_EQUAL -> isEqual(left, right)

                else -> null
            }
        }
    }


    private fun isTruthy(obj: Any?) = when (obj) {
        is Boolean -> obj
        null -> false
        else -> true
    }

    private fun isEqual(a: Any?, b: Any?) = if (a == null && b == null) {
        true
    } else if (a == null) {
        false
    } else {
        a == b
    }

    class RuntimeError(private val token: Token, message: String) : LoxError(token.position, message)

    private fun checkNumberOperand(operator: Token, operand: Any?) = when (operand) {
        is Double -> {}
        else -> throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) = if (left is Double && right is Double) {
    } else {
        throw RuntimeError(operator, "Operands must be numbers")
    }


}
