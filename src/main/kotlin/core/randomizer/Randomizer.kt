package core.randomizer

import core.layer.ILayer
import core.structure.Structure

class Randomizer(structure: Structure) {
  private val flattenedStructure: Structure = structure.flatten()

  fun randomizeVars() {

  }

  fun scanVarParams() {
//    flattenedStructure.allLayers().
  }
}

/**
 * [this] structure is required to be flattened
 */
private fun Structure.allLayers(): List<ILayer> {
  require(blocks.size == 1)
  require(blocks.first().repeat == 1)

  return blocks.first().layers
}