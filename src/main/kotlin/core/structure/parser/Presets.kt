package core.structure

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import core.layer.Layer
import core.layer.composite.*
import core.layer.material.*
import core.layer.material.excitonic.Excitonic
import core.layer.particles.*
import core.optics.ExternalDispersionsContainer
import core.structure.builder.toLayer
import core.structure.builder.userDefinitions
import core.structure.parser.*
import core.util.*
import core.validators.fail

fun JsonNode.GaAs(d: Double, layerType: LayerType) = GaAs(
  d = d,
  dampingFactor = requireDouble(DescriptionParameters.dampingFactor),
  permittivityModel = requireAdachiBasedPermittivityModelFor(layerType)
)

fun JsonNode.AlGaAs(d: Double, layerType: LayerType) = AlGaAs(
  d = d,
  dampingFactor = requireDouble(DescriptionParameters.dampingFactor),
  cAl = requireNonNegativeDouble(DescriptionParameters.cAl),
  permittivityModel = requireAdachiBasedPermittivityModelFor(layerType)
)

fun JsonNode.AlGaAsSb(d: Double) = AlGaAsSb(
  d = d,
  cAl = requireNonNegativeDouble(DescriptionParameters.cAl),
  cAs = requireNonNegativeDouble(DescriptionParameters.cAs)
)

fun GaN(d: Double) = core.layer.material.GaN(
  d = d
)

fun JsonNode.AlGaN(d: Double) = AlGaN(
  d = d,
  cAl = requireNonNegativeDouble(DescriptionParameters.cAl)
)

fun JsonNode.customLayer(d: Double): Layer {
  val epsNode = requireNode(DescriptionParameters.eps)

  return when (val type = epsNode.permittivityType()) {
    is PermittivityType.Number -> ConstPermittivityLayer(
      d = d,
      eps = type.numberValue
    )
    is PermittivityType.ExternalDispersion -> ExternalPermittivityDispersionBasedLayer(
      d = d,
      permittivityDispersion = ExternalDispersionsContainer.externalDispersions[type.dispersionName]!!
    )
    is PermittivityType.Expression -> PermittivityExpressionBasedLayer(
      d = d,
      epsExpr = type.exprText
    )
  }
}

fun JsonNode.userDefinedLayer(): Layer {
  val maybeMaterial = requireTextOrNullUpperCase(DescriptionParameters.material)
  val maybeType = requireTextOrNullUpperCase(DescriptionParameters.type)
  val key = maybeMaterial ?: maybeType ?: fail("Material or type should be specified for a layer")

  val definitionNode = userDefinitions[key] ?: fail("Unknown material or type definition: $key")

  /*
  put definition node into the current one,
  values of fields in this node become overridden by those in definition node.
  This is is convenient for the replacements similar to:

  material: custom_GaN ->

  material: custom,
  eps: {
    fun f(q)=5.1529+(92842.09/(q*q-86436))
    return (f(x), 0)
  }

  Here "material" field value "custom_GaN" is replaced with "custom"
  so that the further recursive call of [toLayer()] was successful
  */
  (this as ObjectNode).setAll<ObjectNode>((definitionNode as ObjectNode))

  return toLayer()
}

fun JsonNode.excitonic(d: Double) = Excitonic(
  d = d,
  medium = requireNode(DescriptionParameters.medium).toLayer(),
  exciton = requireExciton()
)

fun JsonNode.effectiveMedium(d: Double, layerType: LayerType) = EffectiveMedium(
  d = d,
  medium = requireNode(DescriptionParameters.medium).toLayer(),
  particles = requireParticlesFor(layerType),
  f = requireNonNegativeDouble(DescriptionParameters.f)
)

fun JsonNode.spheresLattice(d: Double, layerType: LayerType) = SpheresLattice(
  d = d,
  medium = requireNode(DescriptionParameters.medium).toLayer(),
  particles = requireParticlesFor(layerType),
  latticeFactor = requireNonNegativeDouble(DescriptionParameters.latticeFactor)
)

fun JsonNode.mie(d: Double, layerType: LayerType) = Mie(
  d = d,
  medium = requireNode(DescriptionParameters.medium).toLayer(),
  particles = requireParticlesFor(layerType),
  f = requireNonNegativeDouble(DescriptionParameters.f),
  orders = requireOrders()
)


fun JsonNode.DrudeParticle(r: Double?) = DrudeParticle(
  r = r,
  wPl = requireNonNegativeDouble(DescriptionParameters.w),
  g = requireDouble(DescriptionParameters.g),
  epsInf = requireDouble(DescriptionParameters.epsInf)
)

fun JsonNode.DrudeLorentzParticle(r: Double?) = DrudeLorentzParticle(
  r = r,
  wPl = requireNonNegativeDouble(DescriptionParameters.w),
  g = requireDouble(DescriptionParameters.g),
  epsInf = requireDouble(DescriptionParameters.epsInf),
  oscillators = requireOscillators()
)

fun JsonNode.customParticle(r: Double?): Particle {
  val epsNode = requireNode(DescriptionParameters.eps)

  return when (val type = epsNode.permittivityType()) {
    is PermittivityType.Number -> ConstPermittivityParticle(
      r = r,
      eps = type.numberValue
    )
    is PermittivityType.ExternalDispersion -> ExternalPermittivityDispersionBasedParticle(
      r = r,
      permittivityDispersion = ExternalDispersionsContainer.externalDispersions[type.dispersionName]!!
    )
    is PermittivityType.Expression -> PermittivityExpressionBasedParticle(
      r = r,
      epsExpr = type.exprText
    )
  }
}