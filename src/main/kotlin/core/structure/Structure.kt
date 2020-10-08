package core.structure

import com.fasterxml.jackson.databind.JsonNode
import core.layers.composite.*
import core.layers.particles.*
import core.layers.semiconductor.*
import core.optics.PermittivityModel
import core.optics.particles.LorentzOscillator
import core.util.*
import core.validators.alert

/**
 * Structure is a sequence of blocks
 */
class Structure(val blocks: List<Block>)

fun List<Block>.toStructure() = runCatching {
  Structure(this)
}.getOrElse { ex ->
  alert(header = "Structure description error", content = ex.message ?: "")
  throw ex
}

/**
 * Block is a sequence of layers with repeat descriptor
 * [repeat] number of repetitions of a given sequence of layers
 */
class Block(val repeat: Int, val layers: List<Layer>)

fun List<JsonNode>.toBlock() = Block(
  repeat = first().requirePositiveInt(DescriptionParameters.repeat),
  layers = remaining().map { it.toLayer() }
)

/** See possible layer configurations in the big comment block below */
private fun JsonNode.toLayer(): Layer {
  val layerType = requireLayerType()
  val d = requirePositiveDouble(DescriptionParameters.d)

  return when (layerType) {
    LayerType.GAAS -> GaAs(
      d = d,
      kToN = requireDouble(DescriptionParameters.kToN),
      permittivityModel = requirePermittivityModelFor(layerType)
    )
    LayerType.ALGAAS -> AlGaAs(
      d = d,
      kToN = requireDouble(DescriptionParameters.kToN),
      cAl = requirePositiveDouble(DescriptionParameters.cAl),
      permittivityModel = requirePermittivityModelFor(layerType)
    )
    LayerType.ALGAASSB -> AlGaAsSb(
      d = d,
      cAl = requirePositiveDouble(DescriptionParameters.cAl),
      cAs = requirePositiveDouble(DescriptionParameters.cAs)
    )
    LayerType.CONST_N -> ConstRefractiveIndexLayer(
      d = d,
      n = requireComplex(DescriptionParameters.n)
    )
    LayerType.GAAS_X -> GaAsExcitonic(
      d = d,
      kToN = requireDouble(DescriptionParameters.kToN),
      w0 = requirePositiveDouble(DescriptionParameters.w0),
      G0 = requireDouble(DescriptionParameters.g0),
      G = requireDouble(DescriptionParameters.g),
      permittivityModel = requirePermittivityModelFor(layerType)
    )
    LayerType.ALGAAS_X -> AlGaAsExcitonic(
      d = d,
      kToN = requireDouble(DescriptionParameters.kToN),
      cAl = requirePositiveDouble(DescriptionParameters.cAl),
      w0 = requirePositiveDouble(DescriptionParameters.w0),
      G0 = requireDouble(DescriptionParameters.g0),
      G = requireDouble(DescriptionParameters.g),
      permittivityModel = requirePermittivityModelFor(layerType)
    )
    LayerType.CONST_N_X -> ConstRefractiveIndexLayerExcitonic(
      d = d,
      n = requireComplex(DescriptionParameters.n),
      w0 = requirePositiveDouble(DescriptionParameters.w0),
      G0 = requireDouble(DescriptionParameters.g0),
      G = requireDouble(DescriptionParameters.g)
    )
    LayerType.EFF_MEDIUM -> EffectiveMedium(
      d = d,
      medium = requireMedium().toLayer(),
      particles = requireParticlesFor(layerType),
      f = requirePositiveDouble(DescriptionParameters.f)
    )
    LayerType.MIE -> Mie(
      d = d,
      medium = requireMedium().toLayer(),
      particles = requireParticlesFor(layerType),
      f = requirePositiveDouble(DescriptionParameters.f),
      orders = requireOrders()
    )
    LayerType.SPHERES_LATTICE -> SpheresLattice(
      d = d,
      medium = requireMedium().toLayer(),
      particles = requireParticlesFor(layerType),
      latticeFactor = requirePositiveDouble(DescriptionParameters.latticeFactor)
    )
  }
}

/** "material" or "layer" keywords are interchangeable */
private fun JsonNode.requireLayerType(): LayerType {
  requireTextOrNull(DescriptionParameters.layer)?.let { return@requireLayerType it.requireLayerType() }
  requireTextOrNull(DescriptionParameters.material)?.let { return@requireLayerType it.requireLayerType() }
  error("Missing \"layer\" or \"material\" parameter")
}

private fun String.requireLayerType(): LayerType {
  check(LayerType.values().map { it.name }.contains(this.toUpperCase())) {
    "Unknown layer \"$this\""
  }
  return LayerType.valueOf(this.toUpperCase())
}

private fun JsonNode.requirePermittivityModelFor(layerType: LayerType): PermittivityModel {
  val maybeModel = requireText(DescriptionParameters.n).toUpperCase()

  check(PermittivityModel.values().map { it.name }.contains(maybeModel)) {
    "Unknown permittivity model in \"$this\""
  }
  return PermittivityModel.valueOf(maybeModel).also { it.checkIsAllowedFor(layerType) }
}

private fun PermittivityModel.checkIsAllowedFor(layerType: LayerType) {
  check(layerType in listOf(
    LayerType.GAAS,
    LayerType.ALGAAS,
    LayerType.GAAS_X,
    LayerType.ALGAAS_X
  ) && this in PermittivityModel.values()) {
    "Layers must correspond to their permittivity models specified in \"n\" parameter"
  }
}

private fun JsonNode.requireParticlesPermittivityModel(): ParticlesPermittivityModel {
  val maybeModel = requireText(DescriptionParameters.n).toUpperCase()

  check(ParticlesPermittivityModel.values().map { it.name }.contains(maybeModel)) {
    "Unknown particles permittivity model \"$this\""
  }
  return ParticlesPermittivityModel.valueOf(maybeModel)
}

private fun JsonNode.requireMedium() = requireNode(DescriptionParameters.medium).also {
  check(it.requireLayerType() in listOf(
    LayerType.GAAS,
    LayerType.ALGAAS,
    LayerType.ALGAASSB,
    LayerType.CONST_N
  )) {
    "Medium material/layer can be only GaAs, AlGaAs, AlGaAsSb or const_n"
  }
}

private fun JsonNode.requireParticlesFor(layerType: LayerType) = requireNode(DescriptionParameters.particles).run {
  val r = when (layerType) {
    LayerType.MIE -> requirePositiveDouble(DescriptionParameters.r)
    // "r" parameter can be provided only for Mie layer
    else -> requirePositiveDoubleOrNull(DescriptionParameters.r)?.let {
      error("Particle radius should be provided only for Mie layer")
    }
  }
  when (requireParticlesPermittivityModel()) {
    ParticlesPermittivityModel.DRUDE -> DrudeParticles(
      r = r,
      wPl = requirePositiveDouble(DescriptionParameters.w),
      g = requireDouble(DescriptionParameters.g),
      epsInf = requireDouble(DescriptionParameters.epsInf)
    )
    ParticlesPermittivityModel.DRUDE_LORENTZ -> DrudeLorentzParticles(
      // Drude params
      r = r,
      wPl = requirePositiveDouble(DescriptionParameters.w),
      g = requireDouble(DescriptionParameters.g),
      epsInf = requireDouble(DescriptionParameters.epsInf),
      oscillators = requireOscillators()
    )
    ParticlesPermittivityModel.SB -> SbParticles(r)
  }
}

private fun JsonNode.requireOrders() = requirePositiveIntOrNull(DescriptionParameters.orders)?.let {
  when (it) {
    1 -> Orders.ONE
    2 -> Orders.TWO
    else -> error("Available orders for Mie layer: \"1\", \"2\" or \"all\"")
  }
} ?: run {
  requireText(DescriptionParameters.orders).let {
    when (it) {
      DescriptionParameters.all -> Orders.ALL
      else -> error("Available orders for Mie layer: \"1\", \"2\" or \"all\"")
    }
  }
}

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


enum class LayerType {
  GAAS,
  ALGAAS,
  ALGAASSB,
  CONST_N,
  GAAS_X,
  ALGAAS_X,
  CONST_N_X,
  EFF_MEDIUM,
  MIE, // TODO remove "layer: mie" from computation if activeState().mode() is Scattering or Extinction
  SPHERES_LATTICE
}

object DescriptionParameters {
  const val structure = "structure"
  const val repeat = "repeat"
  const val all = "all"
  const val layer = "layer"
  const val medium = "medium"
  const val particles = "particles"
  const val material = "material"
  const val orders = "orders"
  const val oscillators = "oscillators"
  const val latticeFactor = "lattice_factor"
  const val epsInf = "epsinf"
  const val d = "d"
  const val n = "n"
  const val kToN = "k_to_n"
  const val cAl = "cal"
  const val cAs = "cas"
  const val w = "w"
  const val w0 = "w0"
  const val g = "g"
  const val g0 = "g0"
  const val f = "f"
  const val r = "r"
}

/**
 * Possible layer descriptions according to `when` switch in the code above
 * (not all the layers might have been included):
 *
 * layer: GaAs, n: Adachi_simple, d: 5;
 *
 *
 * layer: AlGaAs, n: Adachi_simple, d: 5, k/n: 0.0, cAl: 0.3;
 *
 *
 * layer: AlGaAsSb, d: 5, cAl: 0.3, cAs: 0.02;
 *
 *
 * layer: const_n, n: 3.6, d: 5;
 * layer: const_n, n: (3.6, 0.1), d: 5;
 *
 *
 * layer: GaAs_X, n: Adachi_simple, d: 5, w0: 1.52, G0: 0.005, G: 0.5;
 *
 *
 * layer: GaAs_X, n: Adachi_simple, d: 5, k/n: 0.0, cAl: 0.3, w0: 1.52, G0: 0.005, G: 0.5;
 *
 *
 * layer: const_n, n: 3.6, d: 5, w0: 1.52, G0: 0.005, G: 0.5;
 * layer: const_n, n: (3.6, 0.1), d: 5, w0: 1.52, G0: 0.005, G: 0.5;
 *
 *
 * layer: eff_medium,
 * medium: { material: AlGaAs, n: Adachi_simple, k/n: 0.0, cAl: 0.3 },
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
 * medium: { material: AlGaAs, n: Adachi_simple, k/n: 0.0, cAl: 0.3 },
 * particles: { n: Drude, r: 10.6, w: 14.6, G: 0.5, epsInf: 1.0 };
 *
 *
 * layer: spheres_lattice,
 * medium: { material: AlGaAs, n: Adachi_simple, k/n: 0.0, cAl: 0.3 },
 * particles: { n: Drude, w: 14.6, G: 0.5, epsInf: 1.0 },
 * d: 40, lattice_factor: 8.1;
 */