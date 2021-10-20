package core.structure.parser.presets

import com.fasterxml.jackson.databind.JsonNode
import core.structure.layer.mutable.DoubleVarParameter
import core.structure.layer.mutable.material.MutableAlGaAs
import core.structure.layer.mutable.material.MutableGaAs
import core.structure.description.DescriptionParameters
import core.structure.parser.*
import core.util.*

fun JsonNode.mutableGaAs(d: DoubleVarParameter, layerType: LayerType) = MutableGaAs(
  d = d,
  dampingFactor = requireDoubleVarParameter(DescriptionParameters.dampingFactor),
  permittivityModel = requireAdachiBasedPermittivityModel(layerType)
)

fun JsonNode.mutableAlGaAs(d: DoubleVarParameter, layerType: LayerType) = MutableAlGaAs(
  d = d,
  dampingFactor = requireDoubleVarParameter(DescriptionParameters.dampingFactor),
  cAl = requireNonNegativeDoubleVarParameter(DescriptionParameters.cAl),
  permittivityModel = requireAdachiBasedPermittivityModel(layerType)
)


//TODO PLSMR-0002 not implemented
/*
fun JsonNode.mutableAlGaAsSb(d: Double) = core.structure.layer.immutable.material.AlGaAsSb(
  d = d,
  cAl = requireNonNegativeDouble(DescriptionParameters.cAl),
  cAs = requireNonNegativeDouble(DescriptionParameters.cAs)
)

fun mutableGaN(d: Double) = core.structure.layer.immutable.material.GaN(
  d = d
)

fun JsonNode.mutableAlGaN(d: Double) = core.structure.layer.immutable.material.AlGaN(
  d = d,
  cAl = requireNonNegativeDouble(DescriptionParameters.cAl)
)

fun JsonNode.mutableCustomLayer(d: Double): ILayer {
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

fun JsonNode.mutableUserDefinedLayer(): ILayer {
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

fun JsonNode.mutableExcitonic(d: Double) = Excitonic(
  d = d,
  medium = requireNode(DescriptionParameters.medium).toLayer(),
  exciton = requireExciton()
)

fun JsonNode.mutableEffectiveMedium(d: Double, layerType: LayerType) = EffectiveMedium(
  d = d,
  medium = requireNode(DescriptionParameters.medium).toLayer(),
  particles = requireParticles(layerType),
  f = requireNonNegativeDouble(DescriptionParameters.f)
)

fun JsonNode.mutableSpheresLattice(d: Double, layerType: LayerType) = SpheresLattice(
  d = d,
  medium = requireNode(DescriptionParameters.medium).toLayer(),
  particles = requireParticles(layerType),
  latticeFactor = requireNonNegativeDouble(DescriptionParameters.latticeFactor)
)

fun JsonNode.mutableMie(d: Double, layerType: LayerType) = Mie(
  d = d,
  medium = requireNode(DescriptionParameters.medium).toLayer(),
  particles = requireParticles(layerType),
  f = requireNonNegativeDouble(DescriptionParameters.f),
  orders = requireOrders()
)


fun JsonNode.mutableDrudeParticle(r: Double?) = DrudeParticle(
  r = r,
  wPl = requireNonNegativeDouble(DescriptionParameters.w),
  g = requireDouble(DescriptionParameters.g),
  epsInf = requireDouble(DescriptionParameters.epsInf)
)

fun JsonNode.mutableDrudeLorentzParticle(r: Double?) = DrudeLorentzParticle(
  r = r,
  wPl = requireNonNegativeDouble(DescriptionParameters.w),
  g = requireDouble(DescriptionParameters.g),
  epsInf = requireDouble(DescriptionParameters.epsInf),
  oscillators = requireDrudeLorentzOscillators()
)

fun JsonNode.mutablecustomParticle(r: Double?): IParticle {
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
*/
