package core.structure

import com.fasterxml.jackson.databind.JsonNode
import core.layer.composite.Orders
import core.layer.material.excitonic.Exciton
import core.layer.particles.*
import core.optics.AdachiBasedPermittivityModel
import core.optics.ExternalDispersionsContainer
import core.optics.particles.LorentzOscillator
import core.structure.builder.userDefinitions
import core.structure.parser.*
import core.util.*
import core.validators.fail


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
    ParticleType.SB -> SbParticle(r)
    ParticleType.BI_ORTHOGONAL -> BiParticle(r, BiParticlePermittivityType.ORTHOGONAL)
    ParticleType.BI_PARALLEL -> BiParticle(r, BiParticlePermittivityType.PARALLEL)
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

private fun JsonNode.maybePermittivityExpression() = this
  .requireNode(DescriptionParameters.eps)
  /** [DescriptionParameters.expr] node is artificially added to a structure description if expression is detected */
  .requireTextOrNull(DescriptionParameters.expr)

private fun JsonNode.maybeExternalDispersion(): String? {
  val maybeDispersion = this
    .requireNode(DescriptionParameters.eps)
    /** [DescriptionParameters.external] node is artificially added to a structure description if external dispersion is detected */
    .requireTextOrNull(DescriptionParameters.external)
    ?: return null

  return when (maybeDispersion) {
    in ExternalDispersionsContainer.externalDispersions -> maybeDispersion
    else -> null
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
fun JsonNode.requireOscillators() = requireNode(DescriptionParameters.oscillators)
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
