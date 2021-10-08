package core.structure

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

internal class FlattenedBlockTest {
  @Test
  fun `flattened (zero layers)`() {
    val expected = Block(repeat = 1, layers = listOf())
    assertThat(blockWithZeroLayers.flatten(), equalTo(expected))
  }

  @Test
  fun `flattened (single layer)`() {
    val expected = Block(repeat = 1, layers = listOf(layer1, layer1, layer1))
    assertThat(blockWithSingleLayer.flatten(), equalTo(expected))
  }

  @Test
  fun `flattened (multiple layers)`() {
    val expected = Block(repeat = 1, layers = listOf(layer1, layer2, layer1, layer2, layer1, layer2))
    assertThat(blockWithMultipleLayers1.flatten(), equalTo(expected))
  }
}

