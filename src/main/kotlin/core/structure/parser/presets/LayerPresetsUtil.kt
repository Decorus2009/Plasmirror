package core.structure.parser.presets

import com.fasterxml.jackson.databind.JsonNode
import core.optics.AdachiBasedPermittivityModel
import core.optics.particles.LorentzOscillator
import core.structure.description.DescriptionParameters
import core.structure.layer.immutable.AbstractLayer
import core.structure.layer.immutable.composite.Orders
import core.structure.layer.immutable.material.excitonic.Exciton
import core.structure.layer.immutable.particles.*
import core.structure.layer.mutable.AbstractMutableLayer
import core.structure.layer.mutable.material.excitonic.MutableExciton
import core.structure.layer.mutable.particles.*
import core.structure.parser.*
import core.util.*
import core.validators.fail

val userDefinitions = mutableMapOf<String, JsonNode>()

/**
 * See possible layer configurations in the help
 *
 * In case if [DescriptionParameters.d] param is visually missing (e.g. in medium: { ... } node),
 * it has been added artificially earlier during [StructureParserUtil.json] call
 * */
fun layer(layerNode: JsonNode): AbstractLayer = with(layerNode) {
  val layerType = requireLayerType()
  val d = requireNonNegativeDouble(DescriptionParameters.d)  // TODO PLSMR-0002 VarParameter candidate

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

fun mutableLayer(layerNode: JsonNode): AbstractMutableLayer = with(layerNode) {
  val layerType = requireLayerType()
  val d = requireNonNegativeDoubleVarParameter(DescriptionParameters.d)

  return when (layerType) {
    is LayerType.Material.GaAs -> mutableGaAs(d, layerType)
    is LayerType.Material.AlGaAs -> mutableAlGaAs(d, layerType)
    is LayerType.Material.AlGaAsSb -> mutableAlGaAsSb(d)
    is LayerType.Material.GaN -> mutableGaN(d)
    is LayerType.Material.AlGaN -> mutableAlGaN(d)
    is LayerType.Material.Custom -> mutableCustomLayer(d)
    is LayerType.UserDefined -> mutableUserDefinedLayer()
    is LayerType.Composite.Excitonic -> mutableExcitonic(d)
    is LayerType.Composite.Mie -> mutableMie(d, layerType)


    else -> throw IllegalArgumentException("Unsupported layer type: $layerType")
  }
}

/**
 * Layer might be represented via
 * 1. predefined material (such as GaAs, or custom)
 * 2. user-defined material (which is *different* from custom material)
 */
fun JsonNode.requireLayerType(): LayerType {
  val maybeMaterial = requireTextOrNullUpperCase(DescriptionParameters.material)

  when {
    maybeMaterial != null && maybeMaterial in userDefinitions.keys -> {
      return LayerType.UserDefined(name = maybeMaterial)
    }

    maybeMaterial != null && maybeMaterial in predefinedMaterialNames -> {
      return when (maybeMaterial) {
        GAAS -> LayerType.Material.GaAs
        ALGAAS -> LayerType.Material.AlGaAs
        ALGAASSB -> LayerType.Material.AlGaAsSb
        GAN -> LayerType.Material.GaN
        ALGAN -> LayerType.Material.AlGaN
        CUSTOM -> LayerType.Material.Custom
        else -> fail("Unknown material parameter \"$maybeMaterial\"")
      }
    }
  }

  val maybeType = requireTextOrNullUpperCase(DescriptionParameters.type)

  when {
    maybeType != null && maybeType in userDefinitions.keys -> {
      return LayerType.UserDefined(name = maybeType)
    }

    maybeType != null && maybeType in predefinedCompositeNames -> {
      return when (maybeType) {
        EXCITONIC -> LayerType.Composite.Excitonic
        EFF_MEDIUM -> LayerType.Composite.EffectiveMedium
        MIE -> LayerType.Composite.Mie
        SPHERES_LATTICE -> LayerType.Composite.SpheresLattice
        else -> fail("Unknown \"type\" parameter \"$maybeType\"")
      }
    }
  }

  fail("Missing or unknown \"material\" or \"type\" parameter. Check syntax or definitions if any")
}

fun JsonNode.requireParticlesFor(layerType: LayerType) = requireNode(DescriptionParameters.particles).run {
  val r = when (layerType) {
    is LayerType.Composite.Mie -> requireNonNegativeDouble(DescriptionParameters.r)
    // "r" parameter can be provided only for Mie layer type
    else -> requirePositiveDoubleOrNull(DescriptionParameters.r)?.let {
      fail("Particle radius should be provided only for Mie layer type")
    }
  }

  val maybeParticlesType = requireTextOrNullUpperCase(DescriptionParameters.material)
    ?: fail("Particles type should be described via \"material\" keyword")

  val particleType = try {
    ParticleType.valueOf(maybeParticlesType)
  } catch (ex: IllegalArgumentException) {
    fail("Unknown particles type \"$maybeParticlesType\"")
  }

  when (particleType) {
    ParticleType.DRUDE -> DrudeParticle(r)
    ParticleType.DRUDE_LORENTZ -> DrudeLorentzParticle(r)
    ParticleType.CUSTOM -> customParticle(r)
    ParticleType.SB_CARDONA -> SbParticle(r, SbParticlePermittivityType.CARDONA_ADACHI)
    ParticleType.SB_PALLIK -> SbParticle(r, SbParticlePermittivityType.PALLIK)
    ParticleType.BI_CARDONA_ADACHI_ORTHOGONAL -> BiParticle(r, BiParticlePermittivityType.CARDONA_ADACHI_ORTHOGONAL)
    ParticleType.BI_CARDONA_ADACHI_PARALLEL -> BiParticle(r, BiParticlePermittivityType.CARDONA_ADACHI_PARALLEL)
//    ParticleType.BI_WERNER_EXPERIMENT -> TODO()
//    ParticleType.BI_WERNER_DFT_CALCULATIONS -> TODO()
  }
}

fun JsonNode.requireAdachiBasedPermittivityModelFor(layerType: LayerType): AdachiBasedPermittivityModel {
  val maybeModelName = requireTextUpperCase(DescriptionParameters.eps)
  val permittivityModelNames = AdachiBasedPermittivityModel.values().map { it.name }
  val isLayerTypeAllowed = layerType is LayerType.Material.GaAs || layerType is LayerType.Material.AlGaAs

  check(maybeModelName in permittivityModelNames && isLayerTypeAllowed) {
    "Unknown permittivity model \"$maybeModelName\""
  }

  return AdachiBasedPermittivityModel.valueOf(maybeModelName)
}


//fun JsonNode.requireParticlesPermittivityModel(): ParticlesPermittivityModel {
//  val maybeModelName = requireTextOrNullUpperCase(DescriptionParameters.material) ?: fail("Particles type should be described via \"material\" keyword")
//
//  check(maybeModelName in ParticlesPermittivityModel.values().map { it.name }) {
//    "Unknown particles permittivity model \"$this\""
//  }
//
//  return ParticlesPermittivityModel.valueOf(maybeModelName)
//}

//fun JsonNode.requireParticlesFor(layerType: LayerType) = requireNode(DescriptionParameters.particles).run {
//  val r = when (layerType) {
//    is LayerType.Composite.Mie -> requireNonNegativeDouble(DescriptionParameters.r)
//    // "r" parameter can be provided only for Mie layer type
//    else -> requirePositiveDoubleOrNull(DescriptionParameters.r)?.let {
//      fail("Particle radius should be provided only for Mie layer type")
//    }
//  }
//
//  when (requireParticlesPermittivityModel()) {
//    ParticlesPermittivityModel.DRUDE -> DrudeParticle(
//      r = r,
//      wPl = requireNonNegativeDouble(DescriptionParameters.w),
//      g = requireDouble(DescriptionParameters.g),
//      epsInf = requireDouble(DescriptionParameters.epsInf)
//    )
//    ParticlesPermittivityModel.DRUDE_LORENTZ -> DrudeLorentzParticle(
//      r = r,
//      wPl = requireNonNegativeDouble(DescriptionParameters.w),
//      g = requireDouble(DescriptionParameters.g),
//      epsInf = requireDouble(DescriptionParameters.epsInf),
//      oscillators = requireOscillators()
//    )
//    ParticlesPermittivityModel.CUSTOM -> {
//      when (val maybeExpr = maybePermittivityExpression()) {
//        null -> ConstPermittivityParticle(
//          eps = requireComplex(DescriptionParameters.eps)
//        )
//        else -> PermittivityExpressionBasedParticle(
//          epsExpr = maybeExpr
//        )
//      }
//    }
//    ParticlesPermittivityModel.SB -> SbParticle(r)
//    ParticlesPermittivityModel.BI_ORTHOGONAL -> BiParticle(r, BiParticlePermittivityType.ORTHOGONAL)
//    ParticlesPermittivityModel.BI_PARALLEL -> BiParticle(r, BiParticlePermittivityType.PARALLEL)
//  }
//}

fun JsonNode.requireOrders(): Orders {
  val maybeNumericOrders = requirePositiveIntOrNull(DescriptionParameters.orders)
  val failMessage = "Available orders for Mie layer type: \"1\", \"2\" or \"all\""

  if (maybeNumericOrders != null) {
    return when (maybeNumericOrders) {
      1 -> Orders.ONE
      2 -> Orders.TWO
      else -> fail(failMessage)
    }
  }

  return requireText(DescriptionParameters.orders).let {
    when (it) {
      DescriptionParameters.all -> Orders.ALL
      else -> fail(failMessage)
    }
  }
}

fun JsonNode.requireExciton() = requireNode(DescriptionParameters.exciton).run {
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

fun JsonNode.requireMutableExciton() = requireNode(DescriptionParameters.exciton).run {
  MutableExciton(
    w0 = requireNonNegativeDoubleVarParameter(DescriptionParameters.w0),
    G0 = requireDoubleVarParameter(DescriptionParameters.g0),
    G = requireDoubleVarParameter(DescriptionParameters.g),
    wb = requireDoubleVarParameter(DescriptionParameters.wb),
    Gb = requireDoubleVarParameter(DescriptionParameters.gb),
    B = requireDoubleVarParameter(DescriptionParameters.b),
    C = requireComplexVarParameter(DescriptionParameters.c),
  )
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
fun JsonNode.requireDrudeLorentzOscillators() = requireNode(DescriptionParameters.oscillators)
  .fields()
  .asSequence()
  .map { oscillator ->
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

fun JsonNode.requireParticles(layerType: LayerType): AbstractParticle = requireNode(DescriptionParameters.particles).run {
  val r = when (layerType) {
    is LayerType.Composite.Mie -> requireNonNegativeDouble(DescriptionParameters.r)
    // "r" parameter can be provided only for Mie layer type
    else -> requirePositiveDoubleOrNull(DescriptionParameters.r)?.let {
      fail("Particle radius should be provided only for Mie layer type")
    }
  }

  when (particleType()) {
    ParticleType.DRUDE -> DrudeParticle(r)
    ParticleType.DRUDE_LORENTZ -> DrudeLorentzParticle(r)
    ParticleType.CUSTOM -> customParticle(r)
    ParticleType.SB_CARDONA -> SbParticle(r, SbParticlePermittivityType.CARDONA_ADACHI)
    ParticleType.SB_PALLIK -> SbParticle(r, SbParticlePermittivityType.PALLIK)
    ParticleType.BI_CARDONA_ADACHI_ORTHOGONAL -> BiParticle(r, BiParticlePermittivityType.CARDONA_ADACHI_ORTHOGONAL)
    ParticleType.BI_CARDONA_ADACHI_PARALLEL -> BiParticle(r, BiParticlePermittivityType.CARDONA_ADACHI_PARALLEL)
  }
}

fun JsonNode.requireDrudeLorentzMutableOscillators() = requireNode(DescriptionParameters.oscillators)
  .fields()
  .asSequence()
  .map { oscillator ->
    val order = oscillator.key
    val params = oscillator.value

    order to MutableLorentzOscillator(
      f_i = params.requireDoubleVarParameter(DescriptionParameters.f),
      g_i = params.requireDoubleVarParameter(DescriptionParameters.g),
      w_i = params.requireDoubleVarParameter(DescriptionParameters.w)
    )
  }
  .sortedBy { it.first }
  .map { it.second }
  .toList()

fun JsonNode.requireMutableParticles(layerType: LayerType): AbstractMutableParticle = requireNode(DescriptionParameters.particles).run {
  val r = when (layerType) {
    is LayerType.Composite.Mie -> requireNonNegativeDoubleVarParameter(DescriptionParameters.r)
    // "r" parameter can be provided only for Mie layer type
    else -> requirePositiveDoubleVarParameterOrNull(DescriptionParameters.r)?.let {
      fail("Particle radius should be provided only for Mie layer type")
    }
  }

  when (particleType()) {
    ParticleType.DRUDE -> mutableDrudeParticle(r)
    ParticleType.DRUDE_LORENTZ -> mutableDrudeLorentzParticle(r)
    ParticleType.CUSTOM -> mutableCustomParticle(r)
    ParticleType.SB_CARDONA -> MutableSbParticle(r, SbParticlePermittivityType.CARDONA_ADACHI)
    ParticleType.SB_PALLIK -> MutableSbParticle(r, SbParticlePermittivityType.PALLIK)
    ParticleType.BI_CARDONA_ADACHI_ORTHOGONAL -> MutableBiParticle(r, BiParticlePermittivityType.CARDONA_ADACHI_ORTHOGONAL)
    ParticleType.BI_CARDONA_ADACHI_PARALLEL -> MutableBiParticle(r, BiParticlePermittivityType.CARDONA_ADACHI_PARALLEL)
  }
}

private fun JsonNode.particleType(): ParticleType {
  val maybeParticlesType = requireTextOrNullUpperCase(DescriptionParameters.material)
    ?: fail("Particles type should be described via \"material\" keyword")

  return try {
    ParticleType.valueOf(maybeParticlesType)
  } catch (ex: IllegalArgumentException) {
    fail("Unknown particles type \"$maybeParticlesType\"")
  }
}

/**
 * Finds, processes and filters out all definition nodes from resulting list
 */
fun List<JsonNode>.extractDefinitions(): List<JsonNode> {
  userDefinitions.clear()

  return filter {
    val definitionNode = it.requireNodeOrNull(DescriptionParameters.definition)

    if (definitionNode != null) {
      userDefinitions[definitionNode.requireTextUpperCase(DescriptionParameters.name)] = definitionNode
    }

    definitionNode == null
  }
}