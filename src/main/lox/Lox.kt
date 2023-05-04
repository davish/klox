import ast.Stmt
import interpreter.Interpreter
import parser.Parser
import parser.Scanner
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    when {
        args.size > 1 -> {
            println("Usage: klox [script]")
            exitProcess(64)
        }

        args.size == 1 -> runFile(args[0])
        else -> runPrompt()
    }
}

private fun run(src: String, interpreter: Interpreter, parseReporter: ErrorReporter, runtimeReporter: ErrorReporter) {
    /// Lexing
    val scanner = Scanner(src, parseReporter)
    val tokens = scanner.scanTokens()

    /// Parsing
    val parser = Parser(tokens, parseReporter)
    val statements = parser.parse()

    /// If parsing wasn't successful, bail here.
    parseReporter.printAllErrors()
    if (parseReporter.hadError) return

    interpreter.reporter = runtimeReporter
    /// Interpret!
    if (statements.size == 1 && statements[0] is Stmt.Expression) {
        statements[0].also {
            if (it is Stmt.Expression) {
                interpreter.evaluateToStr(it.expression)?.let { str -> println(str) }
            }
        }
    }
    interpreter.interpret(statements)
    runtimeReporter.printAllErrors()
}

private fun runFile(path: String) {
    val bytes = Files.readAllBytes(Paths.get(path))
    val source = String(bytes, Charset.defaultCharset());
    val interpreter = Interpreter()
    val parseReporter = ErrorReporter(source)
    val runtimeReporter = ErrorReporter(source)
    run(source, interpreter, parseReporter, runtimeReporter)
    if (parseReporter.hadError) exitProcess(65)
    if (runtimeReporter.hadError) exitProcess(70)
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
        val parseReporter = ErrorReporter(line)
        val runtimeReporter = ErrorReporter(line)
        run(line, interpreter, parseReporter, runtimeReporter)
    }
}
