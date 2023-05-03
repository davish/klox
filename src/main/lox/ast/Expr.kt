package ast

import parser.Token

sealed interface Expr {
    class BinaryOp(val left: Expr, val operator: Token, val right: Expr) : Expr
    class UnaryOp(val operator: Token, val right: Expr) : Expr
    class Grouping(val expression: Expr) : Expr
    class Literal(val value: Any?) : Expr
    class Variable(val name: Token) : Expr
    class Assign(val name: Token, val value: Expr) : Expr
}

