data class Offset(val offset: Int) {
    fun increment(lastChar: Char) = Offset(offset = offset + 1)

}

fun String.at(loc: Offset): Char = this[loc.offset]

fun startLocation() = Offset(0)

data class Position(val start: Offset, val end: Offset)

fun String.at(pos: Position): String {
    return this.substring(pos.start.offset, pos.end.offset)
}

fun point(loc: Offset) = Position(loc, loc)