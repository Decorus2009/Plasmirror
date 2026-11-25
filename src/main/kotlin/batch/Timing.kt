package batch

/**
 * Time of one case (docs/specs/reference-9-golden-set.md 3.1): [runsNs] are the measured repetitions of the kernel,
 * [parseRunsNs] the same count of repetitions of the structure parse; the warm-up repetitions are not stored.
 * [stable]: every repetition (warm-up included) gave bit-for-bit the same series.
 */
data class Timing(val warmupRuns: Int, val runsNs: List<Long>, val parseRunsNs: List<Long>, val stable: Boolean)

private const val NS_IN_MS = 1e6

/** Median in milliseconds; for an even count the mean of the two middle values. */
fun medianMs(ns: List<Long>): Double {
  require(ns.isNotEmpty()) { "No measurements" }
  val sorted = ns.sorted()
  val mid = sorted.size / 2
  return if (sorted.size % 2 == 1) sorted[mid] / NS_IN_MS else (sorted[mid - 1] + sorted[mid]) / 2.0 / NS_IN_MS
}

fun minMs(ns: List<Long>): Double {
  require(ns.isNotEmpty()) { "No measurements" }
  return ns.minOrNull()!! / NS_IN_MS
}

/** Runs [body] and returns its value with the elapsed time of the [clock] in nanoseconds. */
internal fun <T> timedNs(clock: () -> Long = System::nanoTime, body: () -> T): Pair<T, Long> {
  val start = clock()
  val value = body()
  return value to clock() - start
}
