package ast

import parser.Token

interface Function {
    val params: List<Token>
    val body: List<Stmt>
    val name: Token
}