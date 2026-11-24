package batch

import core.optics.ExternalDispersionsContainer
import core.state.StatesManager
import java.io.OutputStream
import java.io.PrintStream

/**
 * Environment of the headless batch mode: config path, silenced stdout, isolation of global state.
 */
object BatchEnvironment {
  const val CONFIG_PROPERTY = "plasmirror.config"
  const val STUB_CONFIG = "data/internal/state/batch-stub-config.json"

  /**
   * Points the old app to the stub config (the owner's live config is never read or written).
   * Must run before the first access to anything that touches `core.state.Config.kt`.
   */
  fun install() {
    if (System.getProperty(CONFIG_PROPERTY) == null) {
      System.setProperty(CONFIG_PROPERTY, STUB_CONFIG)
    }
  }

  /** Swallows everything the old code prints to stdout while [body] runs; the previous stream is restored in `finally`. */
  fun <T> withSilencedStdout(body: () -> T): T {
    val original = System.out
    System.setOut(PrintStream(object : OutputStream() {
      override fun write(b: Int) {}
      override fun write(b: ByteArray, off: Int, len: Int) {}
    }))
    try {
      return body()
    } finally {
      System.setOut(original)
    }
  }

  /** Empties the global registries of the old app: states and external dispersions. */
  fun resetGlobals() {
    StatesManager.states.clear()
    ExternalDispersionsContainer.externalDispersions.clear()
  }
}
