package core.structure.layer.mutable.material

import core.math.Complex
import core.optics.AdachiBasedPermittivityModel
import core.optics.material.AlGaAs.AlGaAsAdachiModelWithGaussianBroadening
import core.optics.material.AlGaAs.AlGaAsAdachiSimpleModel
import core.optics.material.AlGaAsSb.AlGaAsSbAdachiModelWithTemperatureDependence
import core.optics.toEnergy
import core.structure.layer.mutable.AbstractMutableLayer
import core.structure.layer.mutable.DoubleVarParameter

/**
 * Same as [core.layer.immutable.material.AlGaAsBase] but with mutability ability (used in randomization computations)
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
) : MutableAlGaAsBase(d, dampingFactor, cAl = DoubleVarParameter.ZERO_CONST, permittivityModel) {

  override fun variableParameters() = listOf(d, dampingFactor)
}

data class MutableAlGaAs(
  override val d: DoubleVarParameter,
  val dampingFactor: DoubleVarParameter,
  val cAl: DoubleVarParameter,
  val permittivityModel: AdachiBasedPermittivityModel
) : MutableAlGaAsBase(d, dampingFactor, cAl, permittivityModel) {

  override fun variableParameters() = listOf(d, dampingFactor, cAl)
}