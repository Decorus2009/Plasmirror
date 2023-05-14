package core.structure

import com.fasterxml.jackson.databind.JsonNode
import core.structure.layer.*
import core.state.mapper
import core.structure.description.*
import core.structure.parser.presets.*
import core.util.*

fun String.buildStructure() = buildStructure { layersBlockBuilder() }

fun String.buildMutableStructure() = buildStructure { mutableLayersBlockBuilder() }

fun JsonNode.isRepeatDescriptor() = size() == 1 && has("repeat")


private fun String.buildStructure(blockBuilder: List<JsonNode>.() -> Block) = json().asArray()
  .extractDefinitions()
  .buildBlocks { blockBuilder() }
  .buildStructure()

private fun List<Block>.buildStructure() = Structure(this)

/**
 * Builds structure of a list of blocks or of a single block with no repeat descriptors specified.
 * The active state is currently being built and not available yet,
 * so there's no way to check the mode and validate the necessity of presence of repeat descriptors
 *
 * [adjacentPositionsOfRepeatDescriptors] serve as bounds
 * when slicing into chunks to be converted to block descriptions
 */
private fun List<JsonNode>.buildBlocks(blockBuilder: List<JsonNode>.() -> Block): List<Block> {
  val nodes = when {
    // no repeat descriptor is found, insert an artificial node before the node with a single layer description
    !first().isRepeatDescriptor() -> listOf(repeatDescriptorNode()) + this
    else -> this
  }

  return nodes.adjacentPositionsOfRepeatDescriptors()
    .map { (position, nextPosition) ->
      nodes.slice(position until nextPosition).blockBuilder()
    }
    // exclude blocks with 0 repeats (e.g. a user in structure description prints x0 to exclude a block from computation
    .filterNot { it.repeat == 0 }
}

private fun List<JsonNode>.layersBlockBuilder() = block { node -> layer(node) }

private fun List<JsonNode>.mutableLayersBlockBuilder() = block { node -> mutableLayer(node) }

private fun List<JsonNode>.block(layerBuilder: (JsonNode) -> ILayer) = Block(
  repeat = first().requireNonNegativeInt(DescriptionParameters.repeat),
  layers = subList(1, size).map { layerBuilder(it) }
)

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
private fun List<JsonNode>.repeatDescriptorPositions() = this
  .mapIndexed { index, node -> if (node.isRepeatDescriptor()) index else -1 }
  .filterNot { it == -1 }

private fun repeatDescriptorNode() = mapper.readTree("""{"repeat":"1"}""")
