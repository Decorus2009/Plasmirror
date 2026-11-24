package batch

import core.validators.StateException
import core.validators.StructureDescriptionException
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

/**
 * Source of truth: ControlsController.kt:43-57, RangeController.kt:72-81, RandomizationController.kt:117-126
 * of Plasmirror (docs/specs/reference-7-batch-mode.md 4.2).
 */
internal class UiErrorsTest {

  @Test
  fun single_structure_exception() {
    val withText = uiError(CaseKind.SINGLE, StructureDescriptionException("bad layer"), "validation")
    assertThat(withText.header, equalTo("Structure description error"))
    assertThat(withText.message, equalTo("bad layer"))
    assertThat(withText.cls, equalTo("core.validators.StructureDescriptionException"))
    assertThat(withText.stage, equalTo("validation"))

    val withCause = uiError(CaseKind.SINGLE, StructureDescriptionException(null, RuntimeException("deep")), "validation")
    assertThat(withCause.message, equalTo("deep"))

    val bare = uiError(CaseKind.SINGLE, StructureDescriptionException(), "validation")
    assertThat(bare.message, equalTo("Unknown error"))
  }

  @Test
  fun single_state_exception() {
    val e = uiError(CaseKind.SINGLE, StateException("Computation range error", "Incorrect step"), "validation")
    assertThat(e.header, equalTo("Computation range error"))
    assertThat(e.message, equalTo("Incorrect step"))
    assertThat(e.rawMessage, equalTo("Incorrect step"))
  }

  @Test
  fun single_other_exception_prefers_cause_message() {
    val e = uiError(CaseKind.SINGLE, IllegalStateException("a", RuntimeException("b")), "compute")
    assertThat(e.header, equalTo("Unknown error"))
    assertThat(e.message, equalTo("b"))
    assertThat(e.rawMessage, equalTo("a"))

    val noCause = uiError(CaseKind.SINGLE, IllegalStateException("a"), "compute")
    assertThat(noCause.message, equalTo("a"))
    val nothing = uiError(CaseKind.SINGLE, IllegalStateException(), "compute")
    assertThat(nothing.message, equalTo("Unknown error"))
    assertThat(nothing.rawMessage, equalTo(null as String?))
  }

  @Test
  fun range_window_mapping() {
    val state = uiError(CaseKind.RANGE, StateException("Non-variable structure", "no var"), "init")
    assertThat(state.header, equalTo("Non-variable structure"))
    assertThat(state.message, equalTo("no var"))

    val other = uiError(CaseKind.RANGE, StructureDescriptionException("x"), "validation")
    assertThat(other.header, equalTo("Unknown error"))
    assertThat(other.message, equalTo("Unknown error while running ranged value computations"))
    assertThat(other.rawMessage, equalTo("x"))
  }

  @Test
  fun randomization_window_mapping() {
    val state = uiError(CaseKind.RANDOMIZATION, StateException("Parameters error", "p"), "validation")
    assertThat(state.header, equalTo("Parameters error"))

    val other = uiError(CaseKind.RANDOMIZATION, StructureDescriptionException("x"), "validation")
    assertThat(other.header, equalTo("Unknown error"))
    assertThat(other.message, equalTo("Unknown error while running randomized computations"))
    assertThat(other.rawMessage, equalTo("x"))
  }

  @Test
  fun error_that_is_not_exception_has_no_header() {
    // suspect B-8: the old program crashed here (see docs/port/suspects.md#b-8 of Lamella): recorded as a crash
    val e = uiError(CaseKind.SINGLE, StackOverflowError(), "compute")
    assertThat(e.header, equalTo(null as String?))
    assertThat(e.cls, equalTo("java.lang.StackOverflowError"))
    assertThat(e.stage, equalTo("compute"))
  }
}
