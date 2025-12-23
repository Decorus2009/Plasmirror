package core.structure

import com.fasterxml.jackson.databind.JsonNode
import core.state.mapper
import core.structure.description.*
import core.structure.layer.ILayer
import core.structure.parser.presets.*
import core.util.*

fun String.buildStructure() = buildStructure { layersBlockBuilder() }

/**
 * Builds a regular Structure from mutable layers.
 * Note: This loses information about variable repeat parameters.
 * Use [buildMutableStructureWithBlocks] if you need to preserve repeat range information.
 */
fun String.buildMutableStructure() = buildStructure { mutableLayersBlockBuilder() }

/**
 * Builds a MutableStructure that preserves information about variable repeat parameters.
 * Use this when you need to iterate over repeat range values.
 */
fun String.buildMutableStructureWithBlocks(): MutableStructure = json().asArray()
  .extractDefinitions()
  .buildMutableBlocks { mutableBlockBuilder() }
  .let { MutableStructure(it) }

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

/**
 * Builds MutableBlocks that can have variable repeat parameters.
 */
private fun List<JsonNode>.buildMutableBlocks(blockBuilder: List<JsonNode>.() -> MutableBlock): List<MutableBlock> {
  val nodes = when {
    !first().isRepeatDescriptor() -> listOf(repeatDescriptorNode()) + this
    else -> this
  }

  return nodes.adjacentPositionsOfRepeatDescriptors()
    .map { (position, nextPosition) ->
      nodes.slice(position until nextPosition).blockBuilder()
    }
    // exclude blocks with 0 repeats - for variable repeat, check if it's a constant with value 0
    .filterNot { block ->
      val repeatParam = block.repeat
      !repeatParam.isVariable && repeatParam.requireValue() == 0
    }
}

private fun List<JsonNode>.layersBlockBuilder() = block(::layer)

private fun List<JsonNode>.mutableLayersBlockBuilder() = block(::mutableLayer)

/**
 * Creates a Block. For repeat, handles both simple integer and range object.
 * When repeat is a range object (e.g. x:range(10,50,5)), uses the start value.
 */
private fun List<JsonNode>.block(layerBuilder: (JsonNode) -> ILayer) = Block(
  repeat = first().extractRepeatValue(),
  layers = subList(1, size).map { layerBuilder(it) }
)

/**
 * Extracts repeat value from a JsonNode that can be either:
 * - A simple integer: {"repeat": "24"}
 * - A range object: {"repeat": {"range": true, "start": "10", "end": "50", "step": "5"}}
 *
 * For range objects, returns the start value.
 */
private fun JsonNode.extractRepeatValue(): Int {
  val repeatNode = requireNode(DescriptionParameters.repeat)
  return when {
    repeatNode.isIntRangeParameter() -> repeatNode.requireInt(DescriptionParameters.start)
    else -> repeatNode.requireInt()
  }.also { check(it >= 0) { "repeat value must be non-negative" } }
}

/**
 * Creates a MutableBlock with a variable repeat parameter.
 */
private fun List<JsonNode>.mutableBlockBuilder() = MutableBlock(
  repeat = first().requireNode(DescriptionParameters.repeat).requireIntVarParameter(),
  layers = subList(1, size).map { mutableLayer(it) }
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
