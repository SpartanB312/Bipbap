package net.spartanb312.bipbap.process.resource

object NameGenerator {

    private val chars = listOf('i', 'I', 'l', '1')

    fun nextName(size: Int = 30): String {
        var name = ""
        repeat(size) { name += chars.random() }
        return name
    }

}