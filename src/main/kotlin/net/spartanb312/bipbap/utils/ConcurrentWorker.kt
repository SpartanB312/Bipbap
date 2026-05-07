package net.spartanb312.bipbap.utils

import kotlinx.coroutines.*
import java.lang.Runnable
import java.lang.Thread
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

open class ConcurrentWorker(threadCount: Int) {

    private val coroutineDispatcher = Executors.newFixedThreadPool(threadCount, WorkContextThreadFactory()).asCoroutineDispatcher()
    private val coroutineJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(coroutineDispatcher + coroutineJob)

    fun cancelAndClose () {
        coroutineJob.cancel()
        coroutineDispatcher.close()
    }

    fun <T> parallelForEach(values: Iterable<T>, block: suspend (T) -> Unit) {
        parallelMap(values) {
            block(it)
        }
    }

    fun <T> parallelForEach(values: Sequence<T>, block: suspend (T) -> Unit) {
        parallelForEach(values.toList(), block)
    }

    fun <T, R> parallelMap(values: Iterable<T>, block: suspend (T) -> R): List<R> {
        return runBlocking {
            parallelMapAsync(values, block)
        }
    }

    fun <T, R> parallelMap(values: Sequence<T>, block: suspend (T) -> R): List<R> {
        return parallelMap(values.toList(), block)
    }

    suspend fun <T> parallelForEachAsync(values: Iterable<T>, block: suspend (T) -> Unit) {
        parallelMapAsync(values) {
            block(it)
        }
    }

    suspend fun <T> parallelForEachAsync(values: Sequence<T>, block: suspend (T) -> Unit) {
        parallelForEachAsync(values.toList(), block)
    }

    suspend fun <T, R> parallelMapAsync(values: Iterable<T>, block: suspend (T) -> R): List<R> {
        val tasks = values.toList()
        if (tasks.isEmpty()) return emptyList()
        val results = tasks.map { value ->
            coroutineScope.async {
                try {
                    Result.success(block(value))
                } catch (throwable: Throwable) {
                    Result.failure(throwable)
                }
            }
        }.awaitAll()
        results.mapNotNull { it.exceptionOrNull() }.throwIfNotEmpty()
        return results.map { it.getOrThrow() }
    }

    suspend fun <T, R> parallelMapAsync(values: Sequence<T>, block: suspend (T) -> R): List<R> {
        return parallelMapAsync(values.toList(), block)
    }

    private class WorkContextThreadFactory : ThreadFactory {
        private val counter = AtomicInteger(1)

        override fun newThread(runnable: Runnable): Thread {
            return Thread(runnable, "Bipbap-Worker-${counter.getAndIncrement()}").apply {
                isDaemon = true
            }
        }
    }

    private fun List<Throwable>.throwIfNotEmpty() {
        if (isEmpty()) return
        val first = first()
        drop(1).forEach(first::addSuppressed)
        throw first
    }

}