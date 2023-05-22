package core.structure.parser.presets

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import core.optics.ExternalDispersionsContainer
import core.structure.description.DescriptionParameters
import core.structure.layer.immutable.composite.*
import core.structure.layer.immutable.particles.*
import core.structure.layer.mutable.*
import core.structure.layer.mutable.composite.*
import core.structure.layer.mutable.material.*
import core.structure.layer.mutable.material.excitonic.MutableExcitonic
import core.structure.layer.mutable.particles.*
import core.structure.parser.*
import core.util.*
import core.validators.fail

fun JsonNode.mutableGaAs(d: VarParameter<Double>, layerType: LayerType) = MutableGaAs(
  d = d,
  dampingFactor = requireDoubleVarParameter(DescriptionParameters.dampingFactor),
  permittivityModel = requireAdachiBasedPermittivityModelFor(layerType)
)

fun JsonNode.mutableAlGaAs(d: VarParameter<Double>, layerType: LayerType) = MutableAlGaAs(
  d = d,
  dampingFactor = requireDoubleVarParameter(DescriptionParameters.dampingFactor),
  cAl = requireNonNegativeDoubleVarParameter(DescriptionParameters.cAl),
  permittivityModel = requireAdachiBasedPermittivityModelFor(layerType)
)

fun JsonNode.mutableAlGaAsSb(d: VarParameter<Double>) = MutableAlGaAsSb(
  d = d,
  cAl = requireNonNegativeDoubleVarParameter(DescriptionParameters.cAl),
  cAs = requireNonNegativeDoubleVarParameter(DescriptionParameters.cAs)
)

fun mutableGaN(d: VarParameter<Double>) = MutableGaN(
  d = d
)

fun JsonNode.mutableAlGaN(d: VarParameter<Double>) = MutableAlGaN(
  d = d,
  cAl = requireNonNegativeDoubleVarParameter(DescriptionParameters.cAl)
)

fun JsonNode.mutableCustomLayer(d: VarParameter<Double>): AbstractMutableLayer {
  val epsNode = requireNode(DescriptionParameters.eps)

  return when (val type = epsNode.permittivityType()) {
    is PermittivityType.Number -> MutableConstPermittivityLayer(
      d = d,
      eps = ComplexConstParameter.constant(type.numberValue)
    )

    is PermittivityType.ExternalDispersion -> TODO("Plasmirror-6")
    is PermittivityType.Expression -> MutablePermittivityExpressionBasedLayer(
      d = d,
      epsExpr = type.exprText
    )
  }
}

/**
 * This is a crutch because it's required to return [AbstractMutableLayer],
 * whereas implementation is copied from [core.structure.parser.presets.LayerPresetsKt.userDefinedLayer]
 *
 * Need to fix it within
 * https://github.com/Decorus2009/Plasmirror/issues/6
 */
fun JsonNode.mutableUserDefinedLayer(): AbstractMutableLayer {
  val maybeMaterial = requireTextOrNullUpperCase(DescriptionParameters.material)
  val maybeType = requireTextOrNullUpperCase(DescriptionParameters.type)
  val key = maybeMaterial ?: maybeType ?: fail("Material or type should be specified for a layer")

  val definitionNode = userDefinitions[key] ?: fail("Unknown material or type definition: $key")

  (this as ObjectNode).setAll<ObjectNode>((definitionNode as ObjectNode))

  return mutableLayer(this)
}

fun JsonNode.mutableExcitonic(d: VarParameter<Double>) = MutableExcitonic(
  d = d,
  medium = mutableLayer(requireNode(DescriptionParameters.medium)),
  mutableExciton = requireMutableExciton()
)

fun JsonNode.mutableEffectiveMedium(d: VarParameter<Double>, layerType: LayerType) = MutableEffectiveMedium(
  d = d,
  medium = mutableLayer(requireNode(DescriptionParameters.medium)),
  particles = requireMutableParticles(layerType),
  f = requireNonNegativeDoubleVarParameter(DescriptionParameters.f)
)

fun JsonNode.mutableSpheresLattice(d: VarParameter<Double>, layerType: LayerType) = MutableSpheresLattice(
  d = d,
  medium = mutableLayer(requireNode(DescriptionParameters.medium)),
  particles = requireMutableParticles(layerType),
  latticeFactor = requireNonNegativeDoubleVarParameter(DescriptionParameters.latticeFactor)
)

fun JsonNode.mutableMie(d: VarParameter<Double>, layerType: LayerType) = MutableMie(
  d = d,
  medium = mutableLayer(requireNode(DescriptionParameters.medium)),
  particles = requireMutableParticles(layerType),
  f = requireNonNegativeDoubleVarParameter(DescriptionParameters.f),
  orders = requireOrders()
)

fun JsonNode.mutableDrudeParticle(r: VarParameter<Double>?) = MutableDrudeParticle(
  r = r,
  wPl = requireNonNegativeDoubleVarParameter(DescriptionParameters.w),
  g = requireDoubleVarParameter(DescriptionParameters.g),
  epsInf = requireDoubleVarParameter(DescriptionParameters.epsInf)
)

// TODO need to generalize LorentzOscillator and MutableLorentzOscillator, not that easy
fun JsonNode.mutableDrudeLorentzParticle(r: VarParameter<Double>?) = MutableDrudeLorentzParticle(
  r = r,
  wPl = requireDoubleVarParameter(DescriptionParameters.w),
  g = requireDoubleVarParameter(DescriptionParameters.g),
  epsInf = requireDoubleVarParameter(DescriptionParameters.epsInf),
  oscillators = requireDrudeLorentzMutableOscillators()
)

fun JsonNode.mutableCustomParticle(r: VarParameter<Double>?): AbstractMutableParticle {
  val epsNode = requireNode(DescriptionParameters.eps)

  return when (val type = epsNode.permittivityType()) {
    is PermittivityType.Number -> MutableConstPermittivityParticle(
      r = r,
      eps = ComplexConstParameter.constant(type.numberValue)
    )

    is PermittivityType.ExternalDispersion -> MutableExternalPermittivityDispersionBasedParticle(
      r = r,
      permittivityDispersion = ExternalDispersionsContainer.externalDispersions[type.dispersionName]!!
    )

    is PermittivityType.Expression -> MutablePermittivityExpressionBasedParticle(
      r = r,
      epsExpr = type.exprText
    )
  }
}
