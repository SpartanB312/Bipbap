package net.spartanb312.bipbap.process.impls.flow

data class Edge(
    val source: Block,
    val target: Block,
    val kind: EdgeKind,
    val caseValue: Int? = null
)