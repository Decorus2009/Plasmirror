package core.structure

import com.fasterxml.jackson.databind.JsonNode
import core.layers.*
import core.layers.composite.*
import core.layers.excitonic.Exciton
import core.layers.excitonic.Excitonic
import core.layers.particle.*
import core.optics.PermittivityModel
import core.optics.particles.LorentzOscillator
import core.util.*
import ui.controllers.structureDescriptionController

/**
 * Structure is a sequence of blocks
 */
class Structure(val blocks: List<Block>)

/**
 * Block is a sequence of layers with repeat descriptor
 * [repeat] number of repetitions of a given sequence of layers
 */
class Block(val repeat: Int, val layers: List<Layer>)

fun List<JsonNode>.toBlock() = Block(
  repeat = first().requireNonNegativeInt(DescriptionParameters.repeat),
  layers = remaining().map { it.toLayer() }
)

/**
 * See possible layer configurations in the big comment block below
 *
 * In case if [d] param is visually missing (e.g. in medium: { ... } node), it's actually added artificially before in
 * [StructureInitializer.json]
 * */
private fun JsonNode.toLayer(): Layer {
  val layerType = requireLayerType()
  val d = requireNonNegativeDouble(DescriptionParameters.d)

  return when (layerType) {
    LayerType.GAAS -> GaAs(
      d = d,
      dampingFactor = requireDouble(DescriptionParameters.dampingFactor),
      permittivityModel = requirePermittivityModelFor(layerType)
    )
    LayerType.ALGAAS -> AlGaAs(
      d = d,
      dampingFactor = requireDouble(DescriptionParameters.dampingFactor),
      cAl = requireNonNegativeDouble(DescriptionParameters.cAl),
      permittivityModel = requirePermittivityModelFor(layerType)
    )
    LayerType.ALGAASSB -> AlGaAsSb(
      d = d,
      cAl = requireNonNegativeDouble(DescriptionParameters.cAl),
      cAs = requireNonNegativeDouble(DescriptionParameters.cAs)
    )
    LayerType.CUSTOM -> {
      // CUSTOM layer type may contain eps as a number (e.g. eps: 3.6 or eps: (3.6, -0.1)) or as an expression
      when (val maybeExpr = maybeEpsExpression()) {
        null -> ConstPermittivityLayer(
          d = d,
          eps = requireComplex(DescriptionParameters.eps)
        )
        else -> ExpressionBasedPermittivityLayer(
          d = d,
          epsExpr = maybeExpr
        )
      }
    }
    LayerType.EXCITONIC -> Excitonic(
      d = d,
      medium = requireMedium().toLayer(),
      exciton = requireExciton()
    )
    LayerType.EFF_MEDIUM -> EffectiveMedium(
      d = d,
      medium = requireMedium().toLayer(),
      particle = requireParticlesFor(layerType),
      f = requireNonNegativeDouble(DescriptionParameters.f)
    )
    LayerType.SPHERES_LATTICE -> SpheresLattice(
      d = d,
      medium = requireMedium().toLayer(),
      particle = requireParticlesFor(layerType),
      latticeFactor = requireNonNegativeDouble(DescriptionParameters.latticeFactor)
    )
    LayerType.MIE -> Mie(
      d = d,
      medium = requireMedium().toLayer(),
      particle = requireParticlesFor(layerType),
      f = requireNonNegativeDouble(DescriptionParameters.f),
      orders = requireOrders()
    )
  }
}

/** "material" and "type" keywords are interchangeable */
private fun JsonNode.requireLayerType(): LayerType {
  requireTextOrNull(DescriptionParameters.type)?.let { return@requireLayerType it.requireLayerType() }
  requireTextOrNull(DescriptionParameters.material)?.let { return@requireLayerType it.requireLayerType() }
  error("Missing \"type\" or \"material\" parameter")
}

private fun String.requireLayerType(): LayerType {
  check(LayerType.values().map { it.name }.contains(toUpperCase())) {
    "Unknown type \"${findWrongLayerTypeInDescriptionText()}\""
  }
  return LayerType.valueOf(toUpperCase())
}

// finds properly capitalized wrong layer type in actual (i.e. visible to a user) structure description text
// nb: [this] is of lower case
private fun String.findWrongLayerTypeInDescriptionText(): String {
  val wrongLayerType = this
  val actualStructureDescriptionText = structureDescriptionController().structureDescriptionCodeArea.text
  val startPos = actualStructureDescriptionText.replace(" ", "").toLowerCase().indexOf(wrongLayerType)

  return actualStructureDescriptionText.substring(startPos, startPos + wrongLayerType.length)
}

private fun JsonNode.requirePermittivityModelFor(layerType: LayerType): PermittivityModel {
  val maybeModel = requireText(DescriptionParameters.eps).toUpperCase()

  check(PermittivityModel.values().map { it.name }.contains(maybeModel)) {
    "Unknown permittivity model"
  }
  return PermittivityModel.valueOf(maybeModel).also { it.checkIsAllowedFor(layerType) }
}

private fun PermittivityModel.checkIsAllowedFor(layerType: LayerType) {
  check(layerType in listOf(
    LayerType.GAAS,
    LayerType.ALGAAS
  ) && this in PermittivityModel.values()) {
    "Layers must correspond to their permittivity models specified in \"n\" parameter"
  }
}

private fun JsonNode.requireParticlesPermittivityModel(): ParticlesPermittivityModel {
  val maybeModel =
    requireTextOrNull(DescriptionParameters.type)
      ?: requireTextOrNull(DescriptionParameters.material)
      ?: fail("Particles type should to be described via \"type\" or \"material\" keyword")

  check(ParticlesPermittivityModel.values().map { it.name }.contains(maybeModel.toUpperCase())) {
    "Unknown particles permittivity model \"$this\""
  }
  return ParticlesPermittivityModel.valueOf(maybeModel.toUpperCase())
}

private fun JsonNode.requireMedium() = requireNode(DescriptionParameters.medium).also {
  check(it.requireLayerType() in listOf(
    LayerType.GAAS,
    LayerType.ALGAAS,
    LayerType.ALGAASSB,
    LayerType.CUSTOM
  )) {
    "Medium material/type can be only GaAs, AlGaAs, AlGaAsSb or custom"
  }
}

private fun JsonNode.requireExciton() = requireNode(DescriptionParameters.exciton).run {
  Exciton(
    w0 = requireNonNegativeDouble(DescriptionParameters.w0),
    G0 = requireDouble(DescriptionParameters.g0),
    G = requireDouble(DescriptionParameters.g),
    wb = requireDouble(DescriptionParameters.wb),
    Gb = requireDouble(DescriptionParameters.gb),
    B = requireDouble(DescriptionParameters.b),
    C = requireComplex(DescriptionParameters.c),
  )
}

private fun JsonNode.requireParticlesFor(layerType: LayerType) = requireNode(DescriptionParameters.particles).run {
  val r = when (layerType) {
    LayerType.MIE -> requireNonNegativeDouble(DescriptionParameters.r)
    // "r" parameter can be provided only for Mie layer type
    else -> requirePositiveDoubleOrNull(DescriptionParameters.r)?.let {
      error("Particle radius should be provided only for Mie layer type")
    }
  }
  when (requireParticlesPermittivityModel()) {
    ParticlesPermittivityModel.DRUDE -> DrudeParticle(
      r = r,
      wPl = requireNonNegativeDouble(DescriptionParameters.w),
      g = requireDouble(DescriptionParameters.g),
      epsInf = requireDouble(DescriptionParameters.epsInf)
    )
    ParticlesPermittivityModel.DRUDE_LORENTZ -> DrudeLorentzParticle(
      // Drude params
      r = r,
      wPl = requireNonNegativeDouble(DescriptionParameters.w),
      g = requireDouble(DescriptionParameters.g),
      epsInf = requireDouble(DescriptionParameters.epsInf),
      oscillators = requireOscillators()
    )
    ParticlesPermittivityModel.CUSTOM -> {
      when (val maybeExpr = maybeEpsExpression()) {
        null -> ConstPermittivityParticle(
          eps = requireComplex(DescriptionParameters.eps)
        )
        else -> ExpressionBasedPermittivityParticle(
          epsExpr = maybeExpr
        )
      }
    }
    ParticlesPermittivityModel.SB -> SbParticle(r)
  }
}

private fun JsonNode.requireOrders() = requirePositiveIntOrNull(DescriptionParameters.orders)?.let {
  when (it) {
    1 -> Orders.ONE
    2 -> Orders.TWO
    else -> error("Available orders for Mie layer type: \"1\", \"2\" or \"all\"")
  }
} ?: run {
  requireText(DescriptionParameters.orders).let {
    when (it) {
      DescriptionParameters.all -> Orders.ALL
      else -> error("Available orders for Mie layer type: \"1\", \"2\" or \"all\"")
    }
  }
}

private fun JsonNode.maybeEpsExpression() =
  requireNode(DescriptionParameters.eps).requireTextOrNull(DescriptionParameters.expr)

/**
 * Reads a part of user-provided structure description:
 * oscillators: {
 *   1: { w: 1, f: 2, g: 3 },
 *   2: { w: 4, f: 5, g: 6 }
 * }
 *
 * converted to json:
 * "oscillators": {
 *   "1": {"w": "1", "f": "2", "g": "3"},
 *   "2": {"w": "4", "f": "5", "g": "6"}
 * }
 *
 * Iterates through json-map entries, sorts them by keys and returns a list of [LorentzOscillator] objects.
 * So, it's a user's responsibility to specify meaningful successive orders starting with 1.
 * In [core.optics.particles.DrudeLorentzModel] only list of oscillators is used
 * without any information about user-provided orders
 */
private fun JsonNode.requireOscillators() = requireNode(DescriptionParameters.oscillators)
  .fields()
  .asSequence()
  .map { oscillator: MutableMap.MutableEntry<String, JsonNode> ->
    val order = oscillator.key
    val params = oscillator.value
    order to LorentzOscillator(
      f_i = params.requireDouble(DescriptionParameters.f),
      g_i = params.requireDouble(DescriptionParameters.g),
      w_i = params.requireDouble(DescriptionParameters.w)
    )
  }
  .sortedBy { it.first }
  .map { it.second }
  .toList()


private enum class LayerType {
  GAAS,
  ALGAAS,
  ALGAASSB,
  CUSTOM,
  EXCITONIC,
  EFF_MEDIUM,
  MIE, // TODO remove "layer: mie" from computation if activeState().mode() is Scattering or Extinction
  SPHERES_LATTICE
}

object DescriptionParameters {
  const val structure = "structure"
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
  const val exprLeftKWBoundary = "@"
  const val exprRightKWBoundary = "#"
}

// TODO maybe irrelevant (see help)
/**
 * Possible layer descriptions according to `when` switch in the code above
 * (not all the layers might have been included):
 *
 * layer: GaAs, n: Adachi_simple, d: 5;
 *
 *
 * layer: AlGaAs, n: Adachi_simple, d: 5, df: 0.0, cAl: 0.3;
 *
 *
 * layer: AlGaAsSb, d: 5, cAl: 0.3, cAs: 0.02;
 *
 *
 * layer: const_n, n: 3.6, d: 5;
 * layer: const_n, n: (3.6, 0.1), d: 5;
 *
 *
 * layer: excitonic,
 * medium: { material: GaAs, n: adachi_simple, df: 0.0 },
 * exciton: { w0: 1.5, G0: 0.0005, G: 0.1 },
 * d: 12;
 *
 *
 * layer: eff_medium,
 * medium: { material: AlGaAs, n: Adachi_simple, df: 0.0, cAl: 0.3 },
 * particles: { n: Drude, w: 14.6, G: 0.5, epsInf: 1.0 },
 *
 * // or
 *
 * particles: { n: Drude-Lorentz, w: 14.6, G: 0.5, epsInf: 1.0,
 *   oscillators: {
 *     1: { w: 1, f: 2, g: 3 },
 *     2: { w: 4, f: 5, g: 6 }
 *   }
 * },
 * d: 1000, f: 0.01;
 *
 * layer: eff_medium,
 * medium: { layer: AlGaAsSb, n: Adachi_T, cAl: 0.3, cSb: 0.01 },
 * particles: { n: Drude, w: 14.6, G: 0.5, epsInf: 1.0 },
 * d: 1000, f: 0.01;
 *
 *
 * layer: mie,
 * orders: 1,
 * f = 0.01,
 * medium: { material: AlGaAs, n: Adachi_simple, df: 0.0, cAl: 0.3 },
 * particles: { n: Drude, r: 10.6, w: 14.6, G: 0.5, epsInf: 1.0 };
 *
 *
 * layer: spheres_lattice,
 * medium: { material: AlGaAs, n: Adachi_simple, df: 0.0, cAl: 0.3 },
 * particles: { n: Drude, w: 14.6, G: 0.5, epsInf: 1.0 },
 * d: 40, lattice_factor: 8.1;
 *
 *
 *
 * material: custom, d: 90, n: {
 *   val w0 = 1.5
 *   val gamma = 0.1
 *   return gamma / ((x - w0)^2 + gamma^2)
 * };
 *
material: custom, d: 90, n: {
fun f(x) = sin(x)
val offset = 1.0
return f(x) * offset
};


FAILS
x1
layer: excitonic,
medium: { material: GaAs, n: adachi_simple, df: 0.0 },
exciton: { w0: 1.7, G0: 0.0005, G: 0.01 },
d: 10;
material: custom, d: 90, n: {
fun f(x) = sin(x * 0.1) * 0.5
fun g(x) = cos(x * 0.1) * 0.5
return (f(x), g(x))
};

material: custom, d: 90, n: {
fun f(x) = sin(x * 0.1) * 0.5
fun g(x) = cos(x * 0.1) * 0.5
return (f(x), g(x))
};
 */


