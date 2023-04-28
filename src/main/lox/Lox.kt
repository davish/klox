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

private fun run(src: String) {
    source = src
    val scanner = Scanner(src)
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
    var err = "Error: $message\n\n"
    val prefix = "    ${pos.start.line} | "
    if (source != null) {
        val start = pos.start.offset - pos.start.col
        val newlineIdx = source!!.indexOf('\n', start)
        val errorLine = if (newlineIdx >= start) {
            source!!.substring(start..newlineIdx)
        } else {
            source!!.substring(start)
        }
        err += "$prefix$errorLine"
    }
    err += '\n'
    for (i in 0 until pos.start.col + prefix.length) {
        err += " "
    }
    err += "^-- Here.\n"
    println(err)
    hadError = true
}



