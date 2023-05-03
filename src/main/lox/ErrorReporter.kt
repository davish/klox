data class Location(val line: Int, val col: Int)

fun within(offset: Offset, source: String): Location {
    if (offset.offset >= source.length) {
        throw Error("offset should be within the length of the file.")
    }
    var line = 1
    var col = -1
    for (i in 0..offset.offset) {
        if (source[i] == '\n') {
            line++
            col = -1
        } else {
            col++
        }
    }
    return Location(line, col)
}


open class LoxError(val position: Position, message: String) : RuntimeException(message) {
    fun display(source: String): String {
        var err = "Error: $message\n\n"
        val loc = within(position.start, source)
        val prefix = "    ${loc.line} | "
        val start = position.start.offset - loc.col
        val newlineIdx = source.indexOf('\n', start)
        val errorLine = if (newlineIdx >= start) {
            source.substring(start until newlineIdx)
        } else {
            source.substring(start)
        }
        err += "$prefix$errorLine"

        err += '\n'
        for (i in 0 until loc.col + prefix.length) {
            err += " "
        }
        err += "^-- Here.\n"
        return err
    }
}

class ErrorReporter(private val source: String) {
    private val parseErrors: MutableList<LoxError> = ArrayList()
    private val runtimeErrors: MutableList<LoxError> = ArrayList()

    val hadError: Boolean
        get() = parseErrors.size != 0

    val hadRuntimeError: Boolean
        get() = runtimeErrors.size != 0

    fun runtimeError(error: LoxError) {
        runtimeErrors.add(error)
    }

    fun parseError(position: Position, message: String) {
        parseErrors.add(LoxError(position, message))
    }


    private fun deduplicateErrors(): List<LoxError> {
        var lastOffset = -2
        val result: MutableList<LoxError> = ArrayList()
        for (error in parseErrors) {
            if (error.position.start.offset > lastOffset + 1) {
                result.add(error)
            }
            lastOffset = error.position.start.offset
        }
        return result
    }

    fun printAllErrors() {
        for (error in deduplicateErrors()) {
            println(error.display(source))
        }
    }
}