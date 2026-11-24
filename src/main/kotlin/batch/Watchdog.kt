package batch

import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.ExecutionException

/**
 * Runs [body] in its own daemon thread. In time: returns its value (or rethrows its exception).
 * Out of time: calls [onTimeout] once and returns `null`. A thread cannot be stopped in the JVM, so the caller of a
 * timed-out body is expected to end the process (the body keeps the global state of the old program).
 */
fun <T> withTimeLimit(limitMs: Long, onTimeout: () -> Unit, body: () -> T): T? {
  val task = FutureTask<T> { body() }
  Thread(task, "batch-case").apply { isDaemon = true }.start()
  return try {
    task.get(limitMs, TimeUnit.MILLISECONDS)
  } catch (ex: TimeoutException) {
    onTimeout()
    null
  } catch (ex: ExecutionException) {
    throw ex.cause ?: ex
  }
}
