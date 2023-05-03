package interpreter

import ErrorReporter
import LoxError
import ast.Expr
import parser.Token
import parser.TokenType

class Interpreter() {
    var reporter: ErrorReporter = ErrorReporter("")

    fun interpret(expression: Expr) {
        try {
            val value = evaluate(expression)
            println(stringify(value))
        } catch (error: RuntimeError) {
            reporter.runtimeError(error)
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

    fun evaluate(expr: Expr): Any? = when (expr) {
        is Expr.Literal -> expr.value
        is Expr.Grouping -> evaluate(expr.expression)
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


}
