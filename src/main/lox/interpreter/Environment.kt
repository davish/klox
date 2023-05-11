package interpreter

import parser.Token

class Environment(val enclosing: Environment?) {
    constructor() : this(null)

    private val values: MutableMap<String, Any?> = HashMap()

    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }

        if (enclosing != null) return enclosing.get(name)

        throw Interpreter.RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun getAt(distance: Int, name: String) = ancestor(distance).values[name]
    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance).values[name.lexeme] = value
    }

    private fun ancestor(distance: Int): Environment {
        var environment: Environment = this
        for (i in 0 until distance) {
            environment = environment.enclosing ?: throw RuntimeException("Not enough scopes.")
        }
        return environment
    }

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }
        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }
        throw Interpreter.RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }
}