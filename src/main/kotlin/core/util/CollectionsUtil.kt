package core.util

fun <T> MutableList<T>.mapInPlace(transform: (T) -> T) {
  forEachIndexed { index, value ->
    this[index] = transform(value)
  }
}