package core.structure

data class Structure(val blocks: List<Block>) : DeepCopyable<Structure> {
  companion object {
    fun empty() = Structure(blocks = emptyList())
  }

  /**
   * Transforms this Structure to a Structure with a single [Block] containing all layers of all blocks of this Structure
   * (a physical representation of a Structure with a full list of layers)
   *
   * e.g.:
   * Structure(
   *   listOf(
   *     Block(repeat = 2, layers = listOf(A, B)),
   *     Block(repeat = 3, layers = listOf(C, D))
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
      allLayersInAllBlocks.isEmpty() -> {
        empty()
      }
      else -> {
        val singleBlock = Block(repeat = 1, layers = allLayersInAllBlocks)
        Structure(blocks = listOf(singleBlock))
      }
    }
  }

  override fun deepCopy() = Structure(blocks.map { it.deepCopy() })
}



