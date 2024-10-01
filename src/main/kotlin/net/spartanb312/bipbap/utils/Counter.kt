package net.spartanb312.bipbap.utils

import java.util.concurrent.atomic.AtomicInteger

class Counter {
    private val count = AtomicInteger(0)
    fun add(num: Int = 1) = count.getAndAdd(num)
    fun get() = count.get()
}

fun count(block: Counter.() -> Unit): Counter = Counter().apply(block)
