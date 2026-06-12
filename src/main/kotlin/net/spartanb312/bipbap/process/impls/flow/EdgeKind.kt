package net.spartanb312.bipbap.process.impls.flow

enum class EdgeKind {
    FALL_THROUGH,
    JUMP,
    TRUE_BRANCH,
    FALSE_BRANCH,
    SWITCH_CASE,
    SWITCH_DEFAULT
}