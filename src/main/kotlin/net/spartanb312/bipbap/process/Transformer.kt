package net.spartanb312.bipbap.process

import net.spartanb312.bipbap.config.Configurable
import net.spartanb312.bipbap.config.setting
import net.spartanb312.bipbap.process.resource.ResourceCache

abstract class Transformer(name: String) : Configurable(name) {
    open var enabled by setting("Enabled", false)
    abstract fun ResourceCache.transform()
}