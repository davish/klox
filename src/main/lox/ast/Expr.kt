package ast

import parser.Token

sealed interface Expr {
    class Assign(val name: Token, val value: Expr) : Expr
    class BinaryOp(val left: Expr, val operator: Token, val right: Expr) : Expr
    class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr
    class Get(val obj: Expr, val name: Token) : Expr
    class Grouping(val expression: Expr) : Expr
    class Lambda(override val name: Token, override val params: List<Token>, override val body: List<Stmt>) : Expr,
        Function

    class Literal(val value: Any?) : Expr
    class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr
    class Set(val obj: Expr, val name: Token, val value: Expr) : Expr
    class Super(val keyword: Token, val method: Token) : Expr
    class This(val keyword: Token) : Expr
    class Variable(val name: Token) : Expr
    class UnaryOp(val operator: Token, val right: Expr) : Expr
}

