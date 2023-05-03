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