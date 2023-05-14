package core.structure

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.*
import org.junit.Test

internal class FlattenedStructureTest {
  @Test
  fun `flatten (zero blocks)`() {
    val expected = Structure(blocks = listOf())
    assertThat(structureWithZeroBlocks.flatten(), CoreMatchers.equalTo(expected))
  }

  @Test
  fun `flatten (single block)`() {
    val expected = Structure(blocks = listOf(
      Block(
        repeat = 1,
        layers = listOf(layer1, layer2, layer1, layer2, layer1, layer2)
      )
    ))
    assertThat(structureWithSingleBlock.flatten(), CoreMatchers.equalTo(expected))
  }

  @Test
  fun `flatten (multiple blocks)`() {
    val expected = Structure(blocks = listOf(
      Block(
        repeat = 1,
        layers = listOf(
          layer1, layer1, layer1,
          layer1, layer2, layer1, layer2, layer1, layer2,
          layer3, layer4, layer3, layer4, layer3, layer4, layer3, layer4
        )
      )
    ))
    assertThat(structureWithMultipleBlocks.flatten(), CoreMatchers.equalTo(expected))
  }
}