package core.structure

import com.fasterxml.jackson.databind.JsonNode
import core.state.mapper
import core.util.requireNode

fun String.toStructure() = json().asArray().toStructure()

/**
 * Maps structure string representation to a json object
 * Ignores: single-line comments, multi-line comments, empty lines
 * Expand each layer description named tokens to single line:
 *
 * x1
 * layer: GaAs, d: 5;
 * // comment
 * layer: AlGaAs, d: 1000, k/n: 0.0, cAl: 0.3;
 *
 * /*
 * multiline comment 1
 * multiline comment 2
 * */
 * x20
 * layer: spheres_lattice,
 * medium: { material: AlGaAs, n: Adachi_simple, k/n: 0.0, cAl: 0.3 },
 * particles: { n: Drude,      w: 14.6, G: 0.5, epsInf: 1.0 },
 * d: 40, lattice_factor: 8.1       ;
 *
 * ;
 * ---->
 * {"layers":[{"repeat":1},{"layer":"gaas","d":5},{"layer":"algaas","d":1000,"k":0.0,"cal":0.3},{"repeat":20},{"layer":"spheres_lattice","medium":{"material":"algaas","n":"adachi_simple","k":0.0,"cal":0.3},"particles":{"n":"drude","w":14.6,"g":0.5,"epsinf":1.0},"d":40,"lattice_factor":8.1}]}

 *
 * @return structure representation as json object containing array of "layer objects"
 */
fun String.json() = """{"${DescriptionParameters.structure}":[${toLowerCase()
  .replace(Regex("(?s)/\\*.*\\*/"), "")                                    // exclude multi-line comments
  .replace(Regex("\\s*[/]{2,}.*"), "")                                     // exclude single-line comments
  .replace(Regex("\\s+"), "")                                              // remove all spaces, \n
  .replace(Regex("^([^xX])"), "x1$1")                                      // insert x1 into the beginning if description starts without it
  .replace(Regex("([xX])([\\d]+)"), "${DescriptionParameters.repeat}:$2;") // x42 -> repeat:42
  // add artificial d param required for description parsing: medium: { -> medium: { d: 0,
  .replace(Regex("(${DescriptionParameters.medium}:\\{)"), "${DescriptionParameters.medium}:{d:0,")
  .replace(Regex("n:\\("), "\"n\":\"(")                                    // n:( -> "n":"(    for (n:(3.6,0.1),)
  .replace(Regex("k/n"), "k_to_n")                                         // k/n -> "k_to_n"
  .replace(Regex("\\),"), ")\",")                                          // ), -> )",    for (n:(3.6,0.1),)
  .replace(Regex("(\\w+):(\\w+\\.*[0-9]*)"), "\"$1\":\"$2\"")              // n: drude -> "n": "drude"
  .replace(Regex("(\\w+):\\{"), "\"$1\":{")                                // particles: { -> "particles": {
  .split(";")
  .joinToString(",") { "{$it}" }                                           // surround with {} to convert to json node 
  .replace(",{}", "")                                                      // remove empty trailing nodes
}]}"""

fun String.asArray() = mapper.readTree(this)
  .requireNode(DescriptionParameters.structure)
  .also { require(it.isArray) }
  .map { it }

/**
 * Builds structure of a list of blocks or of a single block with no repeat descriptors specified.
 * The active state is currently being built and not available yet,
 * so there's no way to check the mode and validate the necessity of presence of repeat descriptors
 *
 * [adjacentPositionsOfRepeatDescriptors] serve as bounds
 * when slicing into chunks to be converted to block descriptions
 */
fun List<JsonNode>.toStructure(): Structure {
  val nodes = when {
    // no repeat descriptor is found, insert an artificial node before the node with a single layer description
    !first().isRepeatDescription() -> listOf(repeatDescriptorNode()) + this
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

/**
 * @return pairs of adjacent positions of repeat descriptors. The last position is coupled with a size of [this]
 * e.g. [0, 2, 5] -> [(0, 2), (2, 5), (5, 8)], 8 is the size of tokenized lines list
 */
private fun List<JsonNode>.adjacentPositionsOfRepeatDescriptors() = with(repeatDescriptorsPositions()) {
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
private fun List<JsonNode>.repeatDescriptorsPositions() =
  mapIndexed { idx, node -> if (node.isRepeatDescription()) idx else -1 }.filterNot { it == -1 }

private fun repeatDescriptorNode() = mapper.readTree("""{"repeat":"1"}""")

fun JsonNode.isRepeatDescription() = size() == 1 && has("repeat")

/**
 * @return view of elements of the list except first
 */
fun <T> List<T>.remaining() = subList(1, size)