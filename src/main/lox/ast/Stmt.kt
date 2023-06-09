package ast

import parser.Token

sealed interface Stmt {
    class Block(val statements: List<Stmt>) : Stmt
    class Class(val name: Token, val superclass: Expr.Variable?, val methods: List<Function>) : Stmt
    class Expression(val expression: Expr) : Stmt
    class Function(override val name: Token, override val params: List<Token>, override val body: List<Stmt>) : Stmt,
        ast.Function

    class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt
    class Print(val expression: Expr) : Stmt
    class Return(val keyword: Token, val value: Expr?) : Stmt
    class Var(val name: Token, val initializer: Expr?) : Stmt
    class While(val condition: Expr, val body: Stmt) : Stmt
    object Break : Stmt
}