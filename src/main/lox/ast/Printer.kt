package ast

fun prettyPrint(expr: Expr): String = when (expr) {
    is Expr.BinaryOp -> "${prettyPrint(expr.left)} ${expr.operator.lexeme} ${prettyPrint(expr.right)}"
    is Expr.Grouping -> "(${prettyPrint(expr.expression)})"
    is Expr.Literal -> if (expr.value == null) {
        "nil"
    } else {
        expr.value.toString()
    }

    is Expr.UnaryOp -> "${expr.operator.lexeme}${prettyPrint(expr.right)}"
    is Expr.Variable -> expr.name.lexeme
    is Expr.Assign -> "${expr.name} = ${prettyPrint(expr.value)}"
}

private fun sexp(label: String, vararg exprs: Expr): String {
    return "($label ${exprs.joinToString(" ", transform = ::print)})"
}

fun print(expr: Expr): String = when (expr) {
    is Expr.BinaryOp -> sexp(expr.operator.lexeme, expr.left, expr.right)
    is Expr.Grouping -> sexp("group", expr.expression)
    is Expr.Literal -> if (expr.value == null) "nil" else expr.value.toString()
    is Expr.UnaryOp -> sexp(expr.operator.lexeme, expr.right)
    is Expr.Variable -> expr.name.lexeme
    is Expr.Assign -> sexp("assign-${expr.name}", expr.value)
}
