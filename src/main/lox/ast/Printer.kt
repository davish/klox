package ast

fun print(expr: Expr): String = when (expr) {
    is Expr.BinaryOp -> "${print(expr.left)} ${expr.operator.lexeme} ${print(expr.right)}"
    is Expr.Grouping -> "(${print(expr.expression)})"
    is Expr.Literal -> if (expr.value == null) {
        "nil"
    } else {
        expr.value.toString()
    }

    is Expr.UnaryOp -> "${expr.operator.lexeme}${print(expr.right)}"
}