package core.math

import org.hamcrest.CoreMatchers
import org.junit.Assert.assertThat
import org.junit.jupiter.api.assertThrows

inline fun <reified T : Exception> expectException(message: String, crossinline body: () -> Unit) {
  val exception: Exception = assertThrows<T> {
    body()
  }
  assertThat(exception.message, CoreMatchers.equalTo(message))
}
