package interpreter

import ast.Stmt

class LoxFunction(private val declaration: Stmt.Function, private val closure: Environment) : LoxCallable {

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for (i in declaration.params.indices) {
            environment.define(declaration.params[i].lexeme, arguments[i])
        }
        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Interpreter.Return) {
            return returnValue.value
        }
        return null
    }

    override val arity: Int
        get() = declaration.params.size

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }
}