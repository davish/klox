data class Location(val line: Int, val col: Int)

fun within(offset: Offset, source: String): Location {
    var line = 1
    var col = -1
    for (i in 0..offset.offset) {
        if (i < source.length && source[i] == '\n') {
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
        val errorLine = if (newlineIdx >= start && newlineIdx < source.length) {
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
    private val errors: MutableList<LoxError> = ArrayList()

    val hadError: Boolean
        get() = errors.size != 0

    fun error(error: LoxError) {
        errors.add(error)
    }

    fun error(position: Position, message: String) {
        errors.add(LoxError(position, message))
    }

    fun clear() {
        errors.clear()
    }


    private fun deduplicateErrors(): List<LoxError> {
        var lastOffset = -2
        val result: MutableList<LoxError> = ArrayList()
        for (error in errors) {
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