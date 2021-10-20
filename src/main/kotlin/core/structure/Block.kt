package core.structure

import core.structure.layer.ILayer

//interface Block {
//  fun layers(): List<ILayer>
//
//  fun repeat(): Int
//}
//data class MutableBlock(val layers: List<AbstractMutableLayer>) : IBlock {
//  override fun layers() = layers
//
//  override fun repeat() = 1
//}

/**
 * Represents a list of layers with repeat descriptor.
 * [repeat] is a number of repetitions of a given list of layers
 */
data class Block(val repeat: Int, val layers: List<ILayer>) : Copyable<Block> {

  /**
   * Transforms this Block to a Block with [Block.repeat] == 1 and [Block.layers] == [Block.layers] * [Block.repeat]
   * (a physical representation of a block with a full list of layers)
   *
   * e.g.:
   * Block(repeat = 3, layers = listOf(A, B)) -> Block(repeat = 1, layers = listOf(A, B, A, B, A, B))
   */
  fun flatten(): Block {
    val allLayers = mutableListOf<ILayer>()
    repeat(repeat) { allLayers.addAll(layers) }

    return Block(repeat = 1, layers = allLayers)
  }

  override fun deepCopy() = Block(repeat, layers.map { it.deepCopy() })
}
