package core.structure

import core.structure.layer.ILayer
import core.structure.layer.mutable.VarParameter

/**
 * Represents a list of layers with a variable repeat descriptor.
 * Unlike [Block], [repeat] can be a variable parameter (e.g., IntRangeParameter).
 */
data class MutableBlock(val repeat: VarParameter<Int>, val layers: List<ILayer>) {

  /**
   * Returns a list of variable parameters from this block.
   * Currently only repeat can be variable in a block.
   */
  fun variableParameters(): List<VarParameter<Int>> = when {
    repeat.isVariable -> listOf(repeat)
    else -> emptyList()
  }

  fun isVariable() = repeat.isVariable

  /**
   * Transforms this MutableBlock to a Block with the current value of [repeat].
   * [repeat.requireValue()] is used to get the current value.
   *
   * e.g.:
   * MutableBlock(repeat = IntRangeParameter(varValue = 3), layers = listOf(A, B))
   * -> Block(repeat = 3, layers = listOf(A, B))
   */
  fun toBlock(): Block = Block(repeat = repeat.requireValue(), layers = layers)

  /**
   * Transforms this MutableBlock to a Block with [Block.repeat] == 1
   * and [Block.layers] == [Block.layers] * current repeat value
   * (a physical representation of a block with a full list of layers)
   *
   * e.g.:
   * MutableBlock(repeat = IntRangeParameter(varValue = 3), layers = listOf(A, B))
   * -> Block(repeat = 1, layers = listOf(A, B, A, B, A, B))
   */
  fun flatten(): Block {
    val repeatValue = repeat.requireValue()
    val allLayers = mutableListOf<ILayer>()
    repeat(repeatValue) { allLayers.addAll(layers) }

    return Block(repeat = 1, layers = allLayers)
  }
}
