package core.state

import core.validators.StateException
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith

internal class MediumValidationTest {

  @Test
  fun `valid description returns the medium`() {
    assertThat(validatedMedium("material: GaN", "Left medium"), equalTo(Medium("material: GaN")))
  }

  @Test
  fun `blank description is valid (defaults to air)`() {
    // must not throw
    assertThat(validatedMedium("   ", "Left medium"), equalTo(Medium("   ")))
  }

  @Test
  fun `single-layer rule violation is wrapped into StateException with the side header`() {
    val e = assertFailsWith<StateException> { validatedMedium("x2\nmaterial: GaN", "Left medium") }
    assertThat(e.headerMessage, equalTo("Left medium error"))
  }

  @Test
  fun `unknown material is wrapped into StateException with the side header`() {
    val e = assertFailsWith<StateException> { validatedMedium("material: Unobtainium", "Right medium") }
    assertThat(e.headerMessage, equalTo("Right medium error"))
  }

  @Test
  fun `malformed text is wrapped into StateException, not a raw parse error`() {
    // Regression guard: malformed input previously threw JsonParseException straight through,
    // bypassing the medium error dialog.
    val e = assertFailsWith<StateException> { validatedMedium("}{ not valid", "Right medium") }
    assertThat(e.headerMessage, equalTo("Right medium error"))
  }
}
