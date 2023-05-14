package core.structure.description

import core.state.mapper
import core.util.*

/**
 * Maps structure string representation to a json object
 * Ignores: single-line comments, multi-line comments, empty lines
 * Expand each layer description named tokens to single line:
 *
 * x1
 * type: GaAs, d: 5;
 * // comment
 * type: AlGaAs, d: 1000, df: 0.0, cAl: 0.3;
 *
 * /*
 * multiline comment 1
 * multiline comment 2
 * */
 * x20
 * type: spheres_lattice,
 * medium: { material: AlGaAs, eps: Adachi_simple, df: 0.0, cAl: 0.3 },
 * particles: { eps: Drude,      w: 14.6, G: 0.5, epsInf: 1.0 },
 * d: 40, lattice_factor: 8.1       ;
 *
 * ;
 * ---->
 * {"layers":[{"repeat":1},{"layer":"gaas","d":5},{"layer":"algaas","d":1000,"k":0.0,"cal":0.3},{"repeat":20},{"layer":"spheres_lattice","medium":{"material":"algaas","n":"adachi_simple","k":0.0,"cal":0.3},"particles":{"n":"drude","w":14.6,"g":0.5,"epsinf":1.0},"d":40,"lattice_factor":8.1}]}
 *
 * @return structure representation as json object containing array of "layer objects"
 */
fun String.json() = """{"${DescriptionParameters.structure}":[${
  toLowerCase()
    .removeComments()
    .escapeExpressionKeywords()    // note: run it *before* spaces removal below
    .removeSpaces()
    .replaceXWithRepeat()
    .addThicknessNodeToMedium()
    .parseAndQuoteVarRealParams()
    .quoteExpressions()
//    .quoteNumbers()
    .quoteRealNumbers()
    .parseComplexNumbers()
    .quotePairedWords()
    .quoteWordsBeforeCurlyBraces()
    .split(";")
    .joinToString(",") { "{$it}" } // surround with {} to convert to json node 
    .replace(",{}", "")            // remove empty trailing nodes
}]}"""

fun String.asArray() = mapper.readTree(this)
  .requireNode(DescriptionParameters.structure)
  .also { require(it.isArray) }
  .filterNot { it.isEmpty }


/** removes all spaces, \n */
private fun String.removeSpaces() = replace(Regex("\\s+"), "")

/**
 * surround val, fun and return with '@' to avoid expr break after spaces deletion
 */
private fun String.escapeExpressionKeywords() = replace(
  Regex("\\s*\\b(${DescriptionParameters.valExprKw}|${DescriptionParameters.funExprKw}|${DescriptionParameters.returnExprKw})\\b\\s+"),
  "${DescriptionParameters.exprLeftKwBoundary}$1${DescriptionParameters.exprRightKwBoundary}"
)

/** x42 -> repeat:42 */
private fun String.replaceXWithRepeat() = replace(Regex("([xX])([\\d]+)"), "${DescriptionParameters.repeat}:$2;")

/** add artificial d if not specified: medium: { -> medium: { d: 0, */
private fun String.addThicknessNodeToMedium() = replace(Regex("(${DescriptionParameters.medium}:\\{)"), "$1d:0,")

/**
 * eps: { some expr } -> "eps": { "expr":"some expr" }
 *
 * "?" in regex is responsible for non-greedy/reluctant evaluation
 * so that it matches right after it meets the first "}" character that closes expression.
 * All the following "}" are skipped (we need to find only expression-related "}").
 * Can't check that this regex is evaluated lazily in Idea checker (need to use online checker)
 */
private fun String.quoteExpressions() = replace(
  Regex("(\\b${DescriptionParameters.eps}\\b):\\{([\\w\\W\\s]*?)}"),
  "\"$1\":{\"${DescriptionParameters.expr}\":\"$2\"}"
)

private fun String.quoteNumbers() = quoteRealNumbers().quoteComplexNumbers()

/** eps:-3.6E6 -> "eps":"-3.6E6", i.e. real numbers */
private fun String.quoteRealNumbers() = replace(
  Regex("\\b(\\w+)\\b:($realNumberPattern)"),
  "\"$1\":\"$2\""
)

/**
 * e.g.:
 *  eps:(13.6E6,-0.1695) -> "eps":"(13.6E6,-0.1695)", i.e. complex numbers
 *  C:(13.6E6,-0.1695) -> "C":"(13.6E6,-0.1695)"
 */
private fun String.quoteComplexNumbers() = replace(
  Regex("\\b(\\w+)\\b:($complexNumberPattern)"),
  "\"$1\":\"$2\""
)

/**
 * eps:(-3.6E6, 2.4E-2) -> "eps": { "real": "-3.6E6", "imag": "2.4E-2" }

 * it's interesting that imag corresponds to a 4th group
 * e.g. 0-4 groups for "C:(90,-1.561E-2)" are: "C:(90,-1.561E-2)", "С", "90", "null", "-1.561E-2"
 */
private fun String.parseComplexNumbers(): String {
  val realKw = DescriptionParameters.real
  val imagKw = DescriptionParameters.imag

  return replace(
    Regex("\\b(\\w+)\\b:\\(($realNumberPattern),($realNumberPattern)\\)"),
    "\"$1\":{\"$realKw\":\"$2\",\"$imagKw\":\"$4\"}"
  )
}

/**
 * eps: drude -> "eps": "drude"
 * w0: 1.0 -> "w0": "1.0"
 */
private fun String.quotePairedWords() = replace(Regex("\\b(\\w+)\\b:(\\w+\\.*[0-9]*)"), "\"$1\":\"$2\"")

/** particles: { -> "particles": { */
private fun String.quoteWordsBeforeCurlyBraces() = replace(Regex("\\b(\\w+)\\b:\\{"), "\"$1\":{")

/**
 * cAl: var(-3.6E6, 2.4E-2) -> "cAl": { "var": true, "mean": "-3.6E6", "deviation": "2.4E-2" }
 *
 * it's interesting that deviation corresponds to a 4th group
 * e.g. 0-4 groups for "d:var(90,-1.561E-2)" are: "d:var(90,-1.561E-2)", "d", "90", "null", "-1.561E-2"
 */
private fun String.parseAndQuoteVarRealParams(): String {
  val varKw = DescriptionParameters.varExprKw
  val meanKw = DescriptionParameters.mean
  val devKw = DescriptionParameters.deviation

  return replace(
    Regex("\\b(\\w+)\\b:${varKw}\\(($realNumberPattern),($realNumberPattern)\\)"),
    "\"$1\":{\"$varKw\":true,\"$meanKw\":\"$2\",\"$devKw\":\"$4\"}"
  )
}

/**
 * 3 cases:
 *   A:(var(-3.6E6, 2.4E-2), -1.561E-2)
 *   A:(-1.561E-2, var(-3.6E6, 2.4E-2))
 *   A:(var(-3.6E6, 2.4E-2), var(8.125, -1.561E-2))
 */
private fun String.parseAndQuoteComplexVarParams1(): String {
  val varKw = DescriptionParameters.varExprKw
  val meanKw = DescriptionParameters.mean
  val devKw = DescriptionParameters.deviation
  val realKw = DescriptionParameters.real
  val imagKw = DescriptionParameters.imag

  // TODO нужен общий механизм, который выделяет части коплексного числа (группы)
  //  группы отдельно парсятся уже существующими регекспами

  Regex("\\b(\\w+)\\b:\\((${varKw}\\(($realNumberPattern),($realNumberPattern)\\)),($realNumberPattern)\\)")

  return replace(
    Regex("\\b(\\w+)\\b:\\((${varKw}\\(($realNumberPattern),($realNumberPattern)\\)),($realNumberPattern)\\)"),
    "\"$1\":{\"$realKw\":\" {} \"   \"$varKw\":true,\"$meanKw\":\"$2\",\"$devKw\":\"$4\"}"
  )
}