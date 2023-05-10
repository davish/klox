package interpreter

interface LoxCallable {
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
    val arity: Int
}