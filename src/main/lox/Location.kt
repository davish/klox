data class Location(val line: Int, val col: Int, val offset: Int) {
    fun increment(lastChar: Char) = Location(
        line = if (lastChar == '\n') {
            line
        } else {
            line + 1
        },
        col = if (lastChar == '\n') {
            0
        } else {
            col + 1
        },
        offset = offset + 1
    )
}


fun String.at(loc: Location): Char = this[loc.offset]

fun startLocation() = Location(1, 0, 0)

data class Position(val start: Location, val end: Location)

fun String.at(pos: Position): String {
    return this.substring(pos.start.offset, pos.end.offset)
}

fun point(loc: Location) = Position(loc, loc)