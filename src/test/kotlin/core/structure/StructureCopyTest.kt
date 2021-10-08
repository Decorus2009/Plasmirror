package core.structure

import core.layer.mutable.DoubleVarParameter
import core.layer.mutable.DoubleVarParameter.Companion.constant
import core.layer.mutable.DoubleVarParameter.Companion.variable
import core.layer.mutable.material.MutableGaAs
import core.optics.AdachiBasedPermittivityModel
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class StructureCopyTest {
  @Test
  fun `flatten (zero blocks)`() {
    val layer = MutableGaAs(
      d = variable(),
      dampingFactor = constant(1.0),
      permittivityModel = AdachiBasedPermittivityModel.ADACHI_SIMPLE
    )
    val copy = layer.copy()

    assertThat(layer, equalTo(copy))

    layer.d.setValue(5.0)
    println(copy.d === layer.d)
    assertThat(layer, equalTo(copy))
  }
}