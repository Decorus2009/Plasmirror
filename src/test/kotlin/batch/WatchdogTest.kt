package batch

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith

internal class WatchdogTest {

  @Test
  fun returns_body_value_when_in_time() {
    var timeouts = 0
    val value = withTimeLimit(5000, { timeouts++ }) { 42 }
    assertThat(value, equalTo(42))
    assertThat(timeouts, equalTo(0))
  }

  @Test
  fun calls_on_timeout_and_returns_null() {
    var timeouts = 0
    val started = System.nanoTime()
    val value = withTimeLimit(100, { timeouts++ }) {
      Thread.sleep(5000)
      42
    }
    val elapsedMs = (System.nanoTime() - started) / 1_000_000
    assertThat(value, equalTo(null as Int?))
    assertThat(timeouts, equalTo(1))
    assertThat(elapsedMs < 2000, equalTo(true))
  }

  @Test
  fun body_exception_is_rethrown() {
    assertFailsWith<IllegalStateException> {
      withTimeLimit(5000, {}) { throw IllegalStateException("boom") }
    }
  }

  @Test
  fun body_error_is_rethrown_as_is() {
    assertFailsWith<StackOverflowError> {
      withTimeLimit(5000, {}) { throw StackOverflowError() }
    }
  }
}
