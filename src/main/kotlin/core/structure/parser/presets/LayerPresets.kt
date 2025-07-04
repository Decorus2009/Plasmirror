package core.structure.parser.presets

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import core.optics.ExternalDispersionsContainer
import core.structure.description.DescriptionParameters
import core.structure.layer.immutable.AbstractLayer
import core.structure.layer.immutable.composite.EffectiveMedium
import core.structure.layer.immutable.composite.Mie
import core.structure.layer.immutable.composite.SpheresLattice
import core.structure.layer.immutable.material.*
import core.structure.layer.immutable.material.excitonic.Excitonic
import core.structure.layer.immutable.particles.*
import core.structure.parser.LayerType
import core.structure.parser.PermittivityType
import core.structure.parser.permittivityType
import core.util.*
import core.validators.fail

fun JsonNode.GaAs(d: Double, layerType: LayerType) = GaAs(
  d = d,
  dampingFactor = requireDouble(DescriptionParameters.dampingFactor),
  g = requireDoubleOrNull(DescriptionParameters.g),                           // optional parameter for Tanguy models
  epsInf = requireDoubleOrNull(DescriptionParameters.epsInf),                 // optional parameter for Adachi1988,1992 models
  matrixElement = requireDoubleOrNull(DescriptionParameters.matrixElement),   // optional parameter for Tanguy models
  gParam = requireDoubleOrNull(DescriptionParameters.gParam),                 // optional parameter for Tanguy models
  infraredPermittivity = requireDoubleOrNull(DescriptionParameters.epsInfra), // optional parameter for Tanguy models
  permittivityModel = requireBasedPermittivityModelFor(layerType)
)

fun JsonNode.AlGaAs(d: Double, layerType: LayerType) = AlGaAs(
  d = d,
  dampingFactor = requireDoubleOrNull(DescriptionParameters.dampingFactor),
  cAl = requireNonNegativeDouble(DescriptionParameters.cAl),
  g = requireDoubleOrNull(DescriptionParameters.g),                           // optional parameter for Tanguy models
  epsInf = requireDoubleOrNull(DescriptionParameters.epsInf),                 // optional parameter for Adachi1988,1992 models
  matrixElement = requireDoubleOrNull(DescriptionParameters.matrixElement),   // optional parameter for Tanguy models
  gParam = requireDoubleOrNull(DescriptionParameters.gParam),                 // optional parameter for Tanguy models
  infraredPermittivity = requireDoubleOrNull(DescriptionParameters.epsInfra), // optional parameter for Tanguy models
  permittivityModel = requireBasedPermittivityModelFor(layerType)
)

fun JsonNode.AlGaAsSb(d: Double) = AlGaAsSb(
  d = d,
  cAl = requireNonNegativeDouble(DescriptionParameters.cAl),
  cAs = requireNonNegativeDouble(DescriptionParameters.cAs)
)

fun GaN(d: Double) = core.structure.layer.immutable.material.GaN(
  d = d
)

fun JsonNode.AlGaN(d: Double) = AlGaN(
  d = d,
  cAl = requireNonNegativeDouble(DescriptionParameters.cAl)
)

fun JsonNode.customLayer(d: Double): AbstractLayer {
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

    is PermittivityType.CustomModel -> KnownCustomModelBasedLayer(
      d = d,
      modelName = type.modelName,
      m_e = requireDoubleOrNull(DescriptionParameters.m_e),                       // optional parameter for Tanguy models,
      m_hh = requireDoubleOrNull(DescriptionParameters.m_hh),                     // optional parameter for Tanguy models
      excitonRydberg = requireDoubleOrNull(DescriptionParameters.excitonRydberg), // optional parameter for Tanguy models
      Eg = requireDoubleOrNull(DescriptionParameters.Eg),                         // optional parameter for Tanguy models
      matrixElement = requireDoubleOrNull(DescriptionParameters.matrixElement),   // optional parameter for Tanguy models
      infraredPermittivity = requireDoubleOrNull(DescriptionParameters.epsInfra),
      gamma = requireDoubleOrNull(DescriptionParameters.g),
    )

    is PermittivityType.Expression -> PermittivityExpressionBasedLayer(
      d = d,
      epsExpr = type.exprText
    )

  }
}

fun JsonNode.userDefinedLayer(): AbstractLayer {
  val maybeMaterial = requireTextOrNullUpperCase(DescriptionParameters.material)
  val maybeType = requireTextOrNullUpperCase(DescriptionParameters.type)
  val key = maybeMaterial ?: maybeType ?: fail("Material or type should be specified for a layer")

  val definitionNode = userDefinitions[key] ?: fail("Unknown material or type definition: $key")

  /**
  put definition node into the current one,
  values of fields in this node become overridden by those in definition node.
  This is convenient for replacements similar to:

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

  return layer(this)
}

fun JsonNode.excitonic(d: Double) = Excitonic(
  d = d,
  medium = layer(requireNode(DescriptionParameters.medium)),
  exciton = requireExciton()
)

fun JsonNode.effectiveMedium(d: Double, layerType: LayerType) = EffectiveMedium(
  d = d,
  medium = layer(requireNode(DescriptionParameters.medium)),
  particles = requireParticles(layerType),
  f = requireNonNegativeDouble(DescriptionParameters.f)
)

fun JsonNode.spheresLattice(d: Double, layerType: LayerType) = SpheresLattice(
  d = d,
  medium = layer(requireNode(DescriptionParameters.medium)),
  particles = requireParticles(layerType),
  latticeFactor = requireNonNegativeDouble(DescriptionParameters.latticeFactor)
)

fun JsonNode.mie(d: Double, layerType: LayerType) = Mie(
  d = d,
  medium = layer(requireNode(DescriptionParameters.medium)),
  particles = requireParticles(layerType),
  f = requireNonNegativeDouble(DescriptionParameters.f),
  orders = requireOrders(),
  includeMediumAbsorption = requireBooleanOrNull(DescriptionParameters.includeMediumAbsorption) ?: false
//    .also { println("include_medium_absorption: ${it}") }?: false
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
  oscillators = requireDrudeLorentzOscillators()
)

fun JsonNode.customParticle(r: Double?): AbstractParticle {
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

    is PermittivityType.CustomModel -> {
      TODO("Custom permittivity model for particles is not implemented")
    }
  }
}