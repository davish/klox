import interpreter.Interpreter
import parser.Parser
import parser.Scanner
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess


fun main(args: Array<String>) {

//    fun pos(idx: Int) = Position(Offset(idx), Offset(idx))
//    val expression = Expr.BinaryOp(
//        Expr.UnaryOp(
//            Token(TokenType.MINUS, "-", null, pos(1)),
//            Expr.Literal(123)
//        ),
//        Token(TokenType.STAR, "*", null, pos(1)),
//        Expr.Grouping(Expr.Literal(45.67))
//    )
//    println(ast.print(expression))
//    return

    when {
        args.size > 1 -> {
            println("Usage: klox [script]")
            exitProcess(64)
        }

        args.size == 1 -> runFile(args[0])
        else -> runPrompt()
    }
}


private fun run(src: String, reporter: ErrorReporter, interpreter: Interpreter) {
    val scanner = Scanner(src, reporter)
    val tokens = scanner.scanTokens()
//    println(tokens)
    val parser = Parser(tokens, reporter)
    val expression = parser.parse()

    reporter.printAllErrors()
    if (reporter.hadError) return;
    if (expression == null) return

//    println(ast.print(expression))
    interpreter.interpret(expression)
}

private fun runFile(path: String) {
    val bytes = Files.readAllBytes(Paths.get(path))
    val source = String(bytes, Charset.defaultCharset());
    val interpreter = Interpreter()
    val reporter = ErrorReporter(source)
    interpreter.reporter = reporter
    run(source, reporter, interpreter)
    reporter.printAllErrors()
    if (reporter.hadError) exitProcess(65)
    if (reporter.hadRuntimeError) exitProcess(70)
}

private fun runPrompt() {
    println("***klox REPL***\nStart typing below.")
    val interpreter = Interpreter()
    while (true) {
        print("> ")
        val line = readlnOrNull() ?: return
        if (line == "exit()" || line == "quit") {
            return
        }
        val reporter = ErrorReporter(line)
        interpreter.reporter = reporter
        run(line, reporter, interpreter)
    }
}
