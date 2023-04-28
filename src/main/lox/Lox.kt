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


var hadError = false
var source: String? = null

private fun run(source: String) {
    val scanner = Scanner(source)
    println(scanner.scanTokens())
}

fun runFile(path: String) {
    val bytes = Files.readAllBytes(Paths.get(path))
    run(String(bytes, Charset.defaultCharset()))
    if (hadError) exitProcess(65)
}

fun runPrompt() {
    while (true) {
        val line = readlnOrNull() ?: return
        if (line == "exit()") {
            return
        }
        run(line)
        hadError = false
    }
}

fun loxError(pos: Position, message: String) {
    println("$pos: $message")
    hadError = true
}



