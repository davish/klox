package analysis

import ErrorReporter
import ast.Expr
import ast.Stmt
import interpreter.Interpreter
import parser.Token
import java.util.*

class Resolver(val interpreter: Interpreter, val reporter: ErrorReporter) {
    private enum class FunctionType {
        NONE,
        FUNCTION
    }

    private val scopes: Stack<MutableMap<String, Boolean>> = Stack()
    private var currentFunction = FunctionType.NONE

    private fun beginScope() {
        scopes.push(hashMapOf())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return
        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme)) {
            reporter.error(name.position, "There is already a variable with this name in scope.")
        }
        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        val scope = scopes.peek()
        scope[name.lexeme] = true
    }

    private fun resolve(stmt: Stmt): Unit = when (stmt) {
        is Stmt.Block -> {
            beginScope()
            resolve(stmt.statements)
            endScope()
        }

        is Stmt.Var -> {
            declare(stmt.name)
            if (stmt.initializer != null) {
                resolve(stmt.initializer)
            }
            define(stmt.name)
        }

        is Stmt.Function -> {
            declare(stmt.name)
            define(stmt.name)
            resolveFunction(stmt, FunctionType.FUNCTION)
        }

        is Stmt.Expression -> resolve(stmt.expression)
        is Stmt.If -> {
            resolve(stmt.condition)
            resolve(stmt.thenBranch)
            if (stmt.elseBranch != null) resolve(stmt.elseBranch)
            Unit
        }

        is Stmt.Print -> resolve(stmt.expression)
        is Stmt.Return -> {
            if (currentFunction == FunctionType.NONE) {
                reporter.error(stmt.keyword.position, "Can't return from top-level code.")
            }
            if (stmt.value != null) resolve(stmt.value) else Unit
        }

        is Stmt.While -> {
            resolve(stmt.condition)
            resolve(stmt.body)
        }

        Stmt.Break -> Unit
    }

    private fun resolveFunction(function: ast.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type
        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()
        currentFunction = enclosingFunction
    }

    private fun resolve(expr: Expr): Unit = when (expr) {
        is Expr.Variable -> {
            if (!scopes.isEmpty() && scopes.peek()[expr.name.lexeme] == false) {
                reporter.error(expr.name.position, "Can't read local variable in its own initializer.")
            }
            resolveLocal(expr, expr.name)
        }

        is Expr.Assign -> {
            resolve(expr.value)
            resolveLocal(expr, expr.name)
        }

        is Expr.Lambda -> resolveFunction(expr, FunctionType.FUNCTION)
        is Expr.BinaryOp -> {
            resolve(expr.left)
            resolve(expr.right)
        }

        is Expr.Call -> {
            resolve(expr.callee)
            expr.arguments.forEach(::resolve)
        }

        is Expr.Grouping -> resolve(expr.expression)
        is Expr.Literal -> Unit
        is Expr.Logical -> {
            resolve(expr.left)
            resolve(expr.right)
        }

        is Expr.UnaryOp -> resolve(expr.right)
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in (scopes.size - 1 downTo 0)) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
            }
        }
    }

    fun resolve(statements: List<Stmt>) {
        for (statement in statements) {
            resolve(statement)
        }
    }
}

