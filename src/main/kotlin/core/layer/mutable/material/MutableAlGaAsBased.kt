package core.layer.mutable.material

import core.layer.mutable.AbstractMutableLayer
import core.layer.mutable.DoubleVarParameter
import core.math.Complex
import core.optics.AdachiBasedPermittivityModel
import core.optics.material.AlGaAs.AlGaAsAdachiModelWithGaussianBroadening
import core.optics.material.AlGaAs.AlGaAsAdachiSimpleModel
import core.optics.material.AlGaAsSb.AlGaAsSbAdachiModelWithTemperatureDependence
import core.optics.toEnergy
import core.validators.fail

/**
 * Same as [core.layer.immutable.material.AlGaAsBase] but with mutability ability (used in randomization computations)
 *
 */
abstract class MutableAlGaAsBase(
  override val d: DoubleVarParameter,
  private val dampingFactor: DoubleVarParameter,
  private val cAl: DoubleVarParameter,
  private val permittivityModel: AdachiBasedPermittivityModel
) : AbstractMutableLayer(d) {

  override fun permittivity(wl: Double, temperature: Double): Complex {
    val w = wl.toEnergy()

    return when (permittivityModel) {
      AdachiBasedPermittivityModel.ADACHI_SIMPLE -> {
        AlGaAsAdachiSimpleModel.permittivityWithScaledImaginaryPart(w, cAl.requireValue(), dampingFactor.requireValue())
      }
      AdachiBasedPermittivityModel.ADACHI_GAUSS -> {
        AlGaAsAdachiModelWithGaussianBroadening.permittivity(w, cAl.requireValue())
      }
      AdachiBasedPermittivityModel.ADACHI_MOD_GAUSS -> {
        AlGaAsAdachiModelWithGaussianBroadening.permittivityWithScaledImaginaryPart(w, cAl.requireValue(), dampingFactor.requireValue())
      }
      AdachiBasedPermittivityModel.ADACHI_T -> {
        AlGaAsSbAdachiModelWithTemperatureDependence(w, cAl.requireValue(), cAs = 1.0, T = temperature).permittivity()
      }
    }
  }
}

data class MutableGaAs(
  override val d: DoubleVarParameter,
  val dampingFactor: DoubleVarParameter,
  val permittivityModel: AdachiBasedPermittivityModel
) : MutableAlGaAsBase(d, dampingFactor, cAl = DoubleVarParameter.ZERO, permittivityModel) {

  override fun variableParameter() =
    requireDoubleVarParameter(d, dampingFactor, layerName = "GaAs")

  override fun copy() = MutableGaAs(
    d = d.copy(),
    dampingFactor = dampingFactor.copy(),
    permittivityModel
  )
}

data class MutableAlGaAs(
  override val d: DoubleVarParameter,
  val dampingFactor: DoubleVarParameter,
  val cAl: DoubleVarParameter,
  val permittivityModel: AdachiBasedPermittivityModel
) : MutableAlGaAsBase(d, dampingFactor, cAl, permittivityModel) {

  override fun variableParameter(): DoubleVarParameter =
    requireDoubleVarParameter(d, dampingFactor, cAl, layerName = "AlGaAs")

  override fun copy() = MutableAlGaAs(
    d = d.copy(),
    dampingFactor = dampingFactor.copy(),
    cAl = cAl.copy(),
    permittivityModel
  )
}

// TODO test
// TODO move
private fun requireDoubleVarParameter(vararg args: DoubleVarParameter, layerName: String): DoubleVarParameter {
  val varCandidates = args.toList()

  if (varCandidates.hasMultipleVars()) {
    fail("AlGaAs layer has multiple variable parameters specified. Only one \"var\" is allowed")
  }

  return varCandidates.first { it.isVariable }
}

// TODO move
private fun List<DoubleVarParameter>.hasMultipleVars() = count { it.isVariable } > 1