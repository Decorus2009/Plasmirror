package core.structure

import core.structure.layer.mutable.VarParameter

/**
 * A mutable version of [Structure] that holds [MutableBlock]s.
 * This allows blocks to have variable repeat parameters.
 */
data class MutableStructure(val blocks: List<MutableBlock>) {
  companion object {
    fun empty() = MutableStructure(blocks = emptyList())
  }

  /**
   * Returns all variable repeat parameters from all blocks.
   */
  fun variableRepeatParams(): List<VarParameter<Int>> = blocks
    .filter { it.isVariable() }
    .flatMap { it.variableParameters() }

  /**
   * Checks if any block has a variable repeat parameter.
   */
  fun hasVariableRepeat() = blocks.any { it.isVariable() }

  /**
   * Converts to a regular [Structure] by converting each [MutableBlock] to [Block].
   * For blocks with variable repeat, uses the current value of repeat.
   */
  fun toStructure(): Structure = Structure(blocks.map { it.toBlock() })

  /**
   * Transforms this MutableStructure to a Structure with a single [Block] containing all layers.
   * Uses current values of repeat for each block.
   *
   * e.g.:
   * MutableStructure(
   *   listOf(
   *     MutableBlock(repeat = IntConstParameter(2), layers = listOf(A, B)),
   *     MutableBlock(repeat = IntRangeParameter(varValue = 3), layers = listOf(C, D))
   *   )
   * )
   *
   * ->
   *
   * Structure(
   *   listOf(
   *     Block(repeat = 1, layers = listOf(A, B, A, B, C, D, C, D, C, D))
   *   )
   * )
   */
  fun flatten(): Structure {
    val allLayersInAllBlocks = blocks.map { it.flatten().layers }.flatten()

    return when {
      allLayersInAllBlocks.isEmpty() -> Structure.empty()
      else -> {
        val singleBlock = Block(repeat = 1, layers = allLayersInAllBlocks)
        Structure(blocks = listOf(singleBlock))
      }
    }
  }
}
