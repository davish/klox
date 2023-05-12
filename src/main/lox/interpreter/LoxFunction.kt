package interpreter

import ast.Function

class LoxFunction(
    private val declaration: Function,
    private val closure: Environment,
    private val isInitializer: Boolean
) : LoxCallable {

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for (i in declaration.params.indices) {
            environment.define(declaration.params[i].lexeme, arguments[i])
        }
        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Interpreter.Return) {
            if (isInitializer) {
                return closure.getAt(0, "this")
            }
            return returnValue.value
        }
        if (isInitializer) return closure.getAt(0, "this")
        return null
    }

    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return LoxFunction(declaration, environment, isInitializer)
    }

    override val arity: Int
        get() = declaration.params.size

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }
}