package interpreter

class LoxClass(val name: String, private val methods: Map<String, LoxFunction>) : LoxCallable {
    fun findMethod(name: String) = methods[name]

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)
        findMethod("init")?.bind(instance)?.call(interpreter, arguments)
        return instance
    }

    override val arity: Int = findMethod("init")?.arity ?: 0

    override fun toString(): String {
        return name
    }
}