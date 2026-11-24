package batch

/**
 * Environment of the headless batch mode: config path, silenced stdout, isolation of global state.
 * Red skeleton: bodies are filled in by the code commit of step 1.
 */
object BatchEnvironment {
  const val CONFIG_PROPERTY = "plasmirror.config"
  const val STUB_CONFIG = "data/internal/state/batch-stub-config.json"

  fun install(): Unit = TODO("step 1")

  fun <T> withSilencedStdout(body: () -> T): T = TODO("step 1")

  fun resetGlobals(): Unit = TODO("step 1")
}
