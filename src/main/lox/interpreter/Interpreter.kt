package interpreter

import ErrorReporter
import LoxError
import ast.Expr
import ast.Stmt
import parser.Token
import parser.TokenType


class Interpreter() {
    var reporter: ErrorReporter = ErrorReporter("")
    val globals = Environment()
    private var environment = globals
    private val locals: MutableMap<Expr, Int> = hashMapOf()

    init {
        globals.define("clock", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                return System.currentTimeMillis().toDouble() / 1000.0
            }

            override val arity: Int
                get() = 0
        })
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    fun interpret(statements: List<Stmt>) {
        try {
            for (stmt in statements) {
                execute(stmt)
            }
        } catch (error: RuntimeError) {
            reporter.error(error)
        }
    }

    class BreakSignal : RuntimeException()
    class Return(val value: Any?) : RuntimeException(null, null, false, false)

    fun execute(stmt: Stmt): Unit = when (stmt) {
        is Stmt.Break -> throw BreakSignal()
        is Stmt.Block -> executeBlock(stmt.statements, Environment(environment))
        is Stmt.Class -> {
            // Defining the identifier first allows references to the class from inside its methods.
            environment.define(stmt.name.lexeme, null)
            val superclass = if (stmt.superclass != null) {
                evaluate(stmt.superclass)
            } else null

            if (superclass !is LoxClass?) {
                throw RuntimeError(stmt.superclass?.name ?: stmt.name, "Superclass must be a class.")
            }

            if (superclass != null) {
                environment = Environment(environment)
                environment.define("super", superclass)
            }

            val methods =
                stmt.methods.associate { it.name.lexeme to LoxFunction(it, environment, it.name.lexeme == "init") }
            val klass = LoxClass(stmt.name.lexeme, superclass, methods)
            if (superclass != null) {
                environment = environment.enclosing ?: globals
            }
            environment.assign(stmt.name, klass)
        }

        is Stmt.Expression -> {
            evaluate(stmt.expression)
            Unit
        }

        is Stmt.Function -> {
            val function = LoxFunction(stmt, environment, false)
            environment.define(stmt.name.lexeme, function)
        }

        is Stmt.If -> if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        } else {
        }

        is Stmt.Print -> {
            val value = evaluate(stmt.expression)
            println(stringify(value))
        }

        is Stmt.Return -> {
            val value = if (stmt.value != null) {
                evaluate(stmt.value)
            } else {
                null
            }
            throw Return(value)
        }

        is Stmt.Var -> {
            val value = stmt.initializer?.let { evaluate(it) }
            environment.define(stmt.name.lexeme, value)
        }

        is Stmt.While -> {
            while (isTruthy(evaluate(stmt.condition))) {
                try {
                    execute(stmt.body)
                } catch (_: BreakSignal) {
                    break
                }
            }
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

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
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
        is Expr.Call -> {
            val arguments: MutableList<Any?> = ArrayList()
            for (argument in expr.arguments) {
                arguments.add(evaluate(argument))
            }
            val function = evaluate(expr.callee)
            if (function !is LoxCallable) {
                throw RuntimeError(expr.paren, "Can only call functions and classes.")
            }
            if (arguments.size != function.arity) {
                throw RuntimeError(expr.paren, "Expected ${function.arity} arguments ")
            }
            function.call(this, arguments)
        }

        is Expr.Get -> {
            val obj = evaluate(expr.obj)
            if (obj !is LoxInstance) {
                throw RuntimeError(expr.name, "Only instances have properties.")
            }
            obj.get(expr.name)
        }

        is Expr.Super -> {
            val distance = locals[expr] ?: throw RuntimeError(expr.keyword, "'super' not defined in this scope.")
            val superclass = environment.getAt(distance, "super")
            if (superclass !is LoxClass) {
                throw RuntimeError(expr.keyword, "'super' is not a class.")
            }
            val obj = environment.getAt(distance - 1, "this") ?: throw RuntimeError(
                expr.keyword,
                "'super' was not called in a class."
            )

            if (obj !is LoxInstance) {
                throw RuntimeError(expr.keyword, "'super' does not have an instance.")
            }

            val method = superclass.findMethod(expr.method.lexeme)
                ?: throw RuntimeError(expr.method, "Undefined property ${expr.method.lexeme}")

            method.bind(obj)
        }

        is Expr.Set -> {
            val obj = evaluate(expr.obj)
            if (obj !is LoxInstance) {
                throw RuntimeError(expr.name, "Only instances have fields.")
            }
            val value = evaluate(expr.value)
            obj.set(expr.name, value)
            value
        }

        is Expr.Grouping -> evaluate(expr.expression)
        is Expr.This -> lookupVariable(expr.keyword, expr)
        is Expr.Variable -> lookupVariable(expr.name, expr)
        is Expr.Assign -> {
            val value = evaluate(expr.value)
            val distance = locals[expr]
            if (distance != null) {
                environment.assignAt(distance, expr.name, value)
            } else {
                globals.assign(expr.name, value)
            }
            value
        }

        is Expr.Lambda -> {
            LoxFunction(expr, environment, false)
        }

        is Expr.Logical -> {
            run(fun(): Any? {
                val left = evaluate(expr.left)
                if (expr.operator.type == TokenType.OR) {
                    if (isTruthy(left)) return left
                } else if (expr.operator.type == TokenType.AND) {
                    if (!isTruthy(left)) return left
                }
                return evaluate(expr.right)
            })
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

    private fun lookupVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        return if (distance != null) {
            environment.getAt(distance, name.lexeme)
        } else {
            globals.get(name)
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
