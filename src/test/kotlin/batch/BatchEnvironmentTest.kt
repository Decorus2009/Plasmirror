package batch

import core.optics.ExternalDispersionsContainer
import core.state.StatesManager
import core.state.config
import core.state.requireStates
import core.util.KnownPaths
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.sameInstance
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertFailsWith

internal class BatchEnvironmentTest {

  @Test
  fun config_path_comes_from_system_property() {
    // pom.xml passes -Dplasmirror.config=<stub> to the JVM of every test
    assertThat(KnownPaths.config, equalTo(System.getProperty(BatchEnvironment.CONFIG_PROPERTY)))
    assertThat(KnownPaths.config.endsWith("batch-stub-config.json"), equalTo(true))
  }

  @Test
  fun stub_config_gives_one_state_and_no_dispersions() {
    // requireStates() reads the config again, so the check does not depend on what other tests did to StatesManager
    assertThat(requireStates().size, equalTo(1))
    assertThat(config.commonData.externalDispersions.isEmpty(), equalTo(true))
    assertThat(StatesManager.states.size <= 1, equalTo(true))
  }

  @Test
  fun reset_globals_empties_states_and_dispersions() {
    BatchEnvironment.resetGlobals()
    assertThat(StatesManager.states.isEmpty(), equalTo(true))
    assertThat(ExternalDispersionsContainer.externalDispersions.isEmpty(), equalTo(true))
  }

  @Test
  fun silenced_stdout_is_restored() {
    val original = System.out
    val captured = ByteArrayOutputStream()
    val probe = PrintStream(captured, true)
    System.setOut(probe)
    try {
      BatchEnvironment.withSilencedStdout { println("hidden") }
      assertThat(System.out, sameInstance(probe))
      assertThat(captured.size(), equalTo(0))

      assertFailsWith<IllegalStateException> {
        BatchEnvironment.withSilencedStdout { throw IllegalStateException("boom") }
      }
      assertThat(System.out, sameInstance(probe))
    } finally {
      System.setOut(original)
    }
  }
}
