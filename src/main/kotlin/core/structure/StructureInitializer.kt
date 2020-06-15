package core.structure

import core.*
import core.validators.*

/**
 * Validates and builds structure description representation from the string
 */
// TODO FIX: should I pass textDescription to unbound StructureInitializer from StructureDescriptionStorage?
// TODO FIX: eliminate Validation result. Refactor then if yes?
// TODO FIX: throw StructureDescriptionException up to [ControlsController.initialize]
fun initStructure(): ValidationResult = try {
  val tokenizedLines = StructureDescriptionStorage.textDescription.toLines().tokenize()
  StructureValidator().validate(tokenizedLines)
//  tokenizedLines.validate()
  State.structure = tokenizedLines.toStructure()
  ValidationResult.SUCCESS
} catch (e: StructureDescriptionException) {
  alert(
    headerText = "Structure description error",
    contentText = e.message ?: ""
  )
  ValidationResult.FAILURE
}

/**
 * Maps structure string representation to the list of lines
 * such that each line represents description of a layer or period.
 * Ignore: single-line comments, multi-line comments, empty lines
 * Expand each layer description named tokens to single line:
 *
 * type = 7-2, d = 10, x = 0.31,
 * wPlasma = 7.38, gammaPlasma = 0.18,
 * f = 0.0017
 * ---->
 * type = 7-2, d = 10, x = 0.31, wPlasma = 7.38, gammaPlasma = 0.18, f = 0.0017
 *
 * NB: (?s) activates Pattern.DOTALL notation.
 * "In dotall mode, the expression . matches any character, including a line terminator.
 * By default this expression does not match line terminators."
 *
 * @return structure representation as prepared lines
 */
private fun String.toLines() = toLowerCase()
  /** exclude multi-line comments */
  .replace(Regex("(?s)/\\*.*\\*/"), "")
  /** exclude single-line comments */
  .replace(Regex("\\s*[/]{2,}.*"), "")
  /**
   * replace all new line characters by whitespaces
   * (the case when the structure description of some layer occupied several lines)
   */
  .replace(Regex("\\R*"), "")
  /** remove all whitespaces */
  .replace(Regex("\\s*"), "")
  /** put period descriptions to separate lines */
  .replace(Regex("([x][0-9]+)"), "\n\$1\n")
  /** each line will start with the keyword "type" */
  .replace(Regex("(type)"), "\n\$1")
  .lines()
  .map { it.trim() }
  .filter { it.isNotBlank() }

/**
 * Tokenizes each line of, i.e. removes names of layer parameters ("type=", "d=", "k=", "x=")
 *
 * [this] structure representation as lines
 * @return list of tokenized lines (each tokenized line is a list of tokens)
 */
private fun List<String>.tokenize() = map { line ->
  line.split(",").map { token -> token.replace(Regex(".+=+"), "") }
}

/**
 * @return [Structure] object using the tokenized data for layers
 */
private fun TokenizedLines.toStructure() = StructureBuilder.build(structureDescription = toStructureDescription())

/**
 * @return [StructureDescription] object using the tokenized data for layers
 * [adjacentPositionsOfRepeatDescriptors] serve as bounds
 * when slicing [TokenizedLines] into chunks to be converted to block descriptions
 */
private fun TokenizedLines.toStructureDescription() =
  adjacentPositionsOfRepeatDescriptors()
    .map { (position, nextPosition) ->
      slice(position until nextPosition).toBlockDescription()
    }
    .toStructureDescription()

@JvmName("BlockDescriptionsToStructureDescription")
private fun List<BlockDescription>.toStructureDescription() = StructureDescription(blockDescriptions = this)

private fun TokenizedLines.toBlockDescription() = BlockDescription(
  repeat = first().repeatValue(),
  layerDescriptions = remaining().map { it.toLayerDescription() }
)

private fun List<String>.toLayerDescription() = LayerDescription(
  type = first(),
  description = remaining()
)

/**
 * @return view of elements of the list except first
 */
private fun <T> List<T>.remaining() = subList(1, size)

/**
 * @return String representation of a number in repeat descriptor (e.g. ["x123"] -> "123")
 */
private fun List<String>.repeatValue() = first().substring(1)

/**
 * @return positions of repeat descriptors in list of tokenized lines
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
private fun TokenizedLines.repeatDescriptorsPositions() =
  mapIndexed { idx, line -> if (line.isRepeatDescription()) idx else -1 }.filterNot { it == -1 }

/**
 * @return pairs of adjacent positions of repeat descriptors. The last position is coupled with a size of [TokenizedLines]
 * e.g. [0, 2, 5] -> [(0, 2), (2, 5), (5, 8)], 8 is the size of tokenized lines list
 *
 * see the usage in [toStructureDescription]
 */
private fun TokenizedLines.adjacentPositionsOfRepeatDescriptors() = with(repeatDescriptorsPositions()) {
  mapIndexed { index: Int, position: Int ->
    val nextPosition = when (position) {
      last() -> this@adjacentPositionsOfRepeatDescriptors.size
      else -> this@with.elementAt(index + 1)
    }
    position to nextPosition
  }
}