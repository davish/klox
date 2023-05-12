package interpreter

class LoxClass(val name: String, private val superclass: LoxClass?, private val methods: Map<String, LoxFunction>) :
    LoxCallable {
    fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name)) {
            return methods[name]
        }

        if (superclass != null) {
            return superclass.findMethod(name)
        }

        return null
    }

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