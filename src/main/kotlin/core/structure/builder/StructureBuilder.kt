package core.structure.builder

import com.fasterxml.jackson.databind.JsonNode
import core.layer.*
import core.layer.composite.*
import core.layer.particles.*
import core.optics.*
import core.state.mapper
import core.structure.*
import core.structure.parser.*
import core.util.*

val userDefinitions = mutableMapOf<String, JsonNode>()

data class Structure(val blocks: List<Block>) {
  companion object {
    fun empty() = Structure(blocks = emptyList())
  }
}

/**
 * Block is a sequence of layers with repeat descriptor
 * [repeat] number of repetitions of a given sequence of layers
 */
data class Block(
  val repeat: Int,
  val layers: List<Layer>
)

fun String.buildStructure() = json().asArray()
  .extractDefinitions()
  .buildStructure()

/**
 * Builds structure of a list of blocks or of a single block with no repeat descriptors specified.
 * The active state is currently being built and not available yet,
 * so there's no way to check the mode and validate the necessity of presence of repeat descriptors
 *
 * [adjacentPositionsOfRepeatDescriptors] serve as bounds
 * when slicing into chunks to be converted to block descriptions
 */
private fun List<JsonNode>.buildStructure(): Structure {
  val nodes = when {
    // no repeat descriptor is found, insert an artificial node before the node with a single layer description
    !first().isRepeatDescriptor() -> listOf(repeatDescriptorNode()) + this
    else -> this
  }
  val blocks = nodes.adjacentPositionsOfRepeatDescriptors()
    .map { (position, nextPosition) ->
      nodes.slice(position until nextPosition).toBlock()
    }
    // exclude blocks with 0 repeats (e.g. a user in structure description prints x0 to exclude a block from computation
    .filterNot { it.repeat == 0 }

  return Structure(blocks)
}

private fun List<JsonNode>.toBlock() = Block(
  repeat = first().requireNonNegativeInt(DescriptionParameters.repeat),
  layers = subList(1, size).map { it.toLayer() }
)

/**
 * See possible layer configurations in the help
 *
 * In case if [DescriptionParameters.d] param is visually missing (e.g. in medium: { ... } node),
 * it has been added artificially earlier during [StructureParserUtil.json] call
 * */
fun JsonNode.toLayer(): Layer {
  val layerType = requireLayerType()
  val d = requireNonNegativeDouble(DescriptionParameters.d)

  return when (layerType) {
    is LayerType.Material.GaAs -> GaAs(d, layerType)
    is LayerType.Material.AlGaAs -> AlGaAs(d, layerType)
    is LayerType.Material.AlGaAsSb -> AlGaAsSb(d)
    is LayerType.Material.GaN -> GaN(d)
    is LayerType.Material.AlGaN -> AlGaN(d)
    is LayerType.Material.Custom -> customLayer(d)
    is LayerType.UserDefined -> userDefinedLayer()
    is LayerType.Composite.Excitonic -> excitonic(d)
    is LayerType.Composite.EffectiveMedium -> effectiveMedium(d, layerType)
    is LayerType.Composite.SpheresLattice -> spheresLattice(d, layerType)
    is LayerType.Composite.Mie -> mie(d, layerType)
  }
}

/**
 * @return pairs of adjacent positions of repeat descriptors. The last position is coupled with a size of [this]
 * e.g. [0, 2, 5] -> [(0, 2), (2, 5), (5, 8)], 8 is the size of tokenized lines list
 */
private fun List<JsonNode>.adjacentPositionsOfRepeatDescriptors() = with(repeatDescriptorPositions()) {
  mapIndexed { index: Int, position: Int ->
    val nextPosition = when (position) {
      last() -> this@adjacentPositionsOfRepeatDescriptors.size
      else -> this@with.elementAt(index + 1)
    }
    position to nextPosition
  }
}

/**
 * @return positions of repeat descriptors
 * e.g.
 *
 * 0: x10           <-- repeat descriptor
 * 1: type = x, ...
 *
 * 2: x24           <-- repeat descriptor
 * 3: type = x, ...
 * 4: type = y, ...
 *
 * 5: x100          <-- repeat descriptor
 * 6: type = y, ...
 * 7: type = z, ...
 *
 * returns [0, 2, 5]
 */
private fun List<JsonNode>.repeatDescriptorPositions() =
  mapIndexed { idx, node -> if (node.isRepeatDescriptor()) idx else -1 }.filterNot { it == -1 }

private fun repeatDescriptorNode() = mapper.readTree("""{"repeat":"1"}""")

fun JsonNode.isRepeatDescriptor() = size() == 1 && has("repeat")

