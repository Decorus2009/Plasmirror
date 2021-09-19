package core.structure.parser

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
fun String.json(): String {

  return """{"${DescriptionParameters.structure}":[${
    toLowerCase()
      .removeMultiLineComments()
      .removeSingleLineComments()
      // surround val, fun and return with '@' to avoid expr break after spaces deletion
      .replace(
        Regex("\\s*\\b(val|fun|return)\\b\\s+"),
        "${DescriptionParameters.exprLeftKWBoundary}$1${DescriptionParameters.exprRightKWBoundary}"
      )
      // remove all spaces, \n
      .replace(Regex("\\s+"), "")
//      // insert x1 into the beginning if description starts without it
//      .replace(Regex("^([^xX])"), "x1$1")
      // x42 -> repeat:42
      .replace(Regex("([xX])([\\d]+)"), "${DescriptionParameters.repeat}:$2;")
      // add artificial d if not specified: medium: { -> medium: { d: 0,
      .replace(Regex("(${DescriptionParameters.medium}:\\{)"), "$1d:0,")
      /**
       * eps: { some expr } -> "eps": { "expr":"some expr" }
       *
       * "?" in regex is responsible for non-greedy/reluctant evaluation
       * so that it matches right after it meets the first "}" character that closes expression.
       * All the following "}" are skipped (we need to find only expression-related "}").
       * Can't check that this regex is evaluated lazily in Idea checker (need to use online checker)
       */
      .replace(
        Regex("(\\b${DescriptionParameters.eps}\\b):\\{([\\w\\W\\s]*?)}"),
        "\"$1\":{\"${DescriptionParameters.expr}\":\"$2\"}"
      )
      // eps:-3.6E6 -> "eps":"-3.6E6", i.e. real numbers
      .replace(
        Regex("\\b(\\w+)\\b:($realNumberPattern)"),
        "\"$1\":\"$2\""
      )
      /**
       * e.g.:
       *  eps:(13.6E6,-0.1695) -> "eps":"(13.6E6,-0.1695)", i.e. complex numbers
       *  C:(13.6E6,-0.1695) -> "C":"(13.6E6,-0.1695)"
       */
      .replace(
        Regex("\\b(\\w+)\\b:($complexNumberPattern)"),
        "\"$1\":\"$2\""
      )
      /**
       * eps: drude -> "eps": "drude"
       * w0: 1.0 -> "w0": "1.0"
       */
      .replace(Regex("\\b(\\w+)\\b:(\\w+\\.*[0-9]*)"), "\"$1\":\"$2\"")
      // particles: { -> "particles": {
      .replace(Regex("\\b(\\w+)\\b:\\{"), "\"$1\":{")
      .split(";")
      // surround with {} to convert to json node 
      .joinToString(",") { "{$it}" }
      // remove empty trailing nodes
      .replace(",{}", "")
  }]}"""
}

fun String.asArray() = mapper.readTree(this)
  .requireNode(DescriptionParameters.structure)
  .also { require(it.isArray) }
  .filterNot { it.isEmpty }

object DescriptionParameters {
  const val structure = "structure"
  const val definition = "def"
  const val name = "name"
  const val repeat = "repeat"
  const val all = "all"
  const val type = "type"
  const val medium = "medium"
  const val particles = "particles"
  const val exciton = "exciton"
  const val material = "material"
  const val orders = "orders"
  const val oscillators = "oscillators"
  const val latticeFactor = "lattice_factor"
  const val eps = "eps"
  const val epsInf = "eps_inf"
  const val d = "d"
  const val n = "n"
  const val dampingFactor = "df"
  const val cAl = "cal"
  const val cAs = "cas"
  const val w = "w"
  const val w0 = "w0"
  const val g = "g"
  const val g0 = "g0"
  const val wb = "wb"
  const val gb = "gb"
  const val b = "b"
  const val c = "c"
  const val f = "f"
  const val r = "r"
  const val expr = "expr"
  const val external = "external"
  const val exprLeftKWBoundary = "@"
  const val exprRightKWBoundary = "#"
}