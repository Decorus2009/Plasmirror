package core.structure.layer.mutable.material

import core.math.Complex
import core.optics.AlGaAsPermittivityModel
import core.optics.material.AlGaAs.AlGaAsAdachiModelWithGaussianBroadening
import core.optics.material.AlGaAs.AlGaAsAdachiSimpleModel
import core.optics.material.AlGaAsSb.AlGaAsSbAdachiModelWithTemperatureDependence
import core.optics.material.AlGaAsWithGamma.Tanguy95Model
import core.optics.toEnergy
import core.structure.layer.mutable.AbstractMutableLayer
import core.structure.layer.mutable.DoubleConstParameter
import core.structure.layer.mutable.VarParameter

/**
 * Same as [core.layer.immutable.material.AlGaAsBase] but with mutability ability (used in randomization computations)
 */
abstract class MutableAlGaAsBase(
  override val d: VarParameter<Double>,
  private val dampingFactor: VarParameter<Double>?,
  private val cAl: VarParameter<Double>,
  private val permittivityModel: AlGaAsPermittivityModel
) : AbstractMutableLayer(d) {

  override fun permittivity(wl: Double, temperature: Double): Complex {
    val w = wl.toEnergy()

    return when (permittivityModel) {
      AlGaAsPermittivityModel.ADACHI_SIMPLE -> {
        check(dampingFactor != null) { "Gamma parameter 'df' is required for AlGaAs or GaAs layer with Adachi based models" }

        AlGaAsAdachiSimpleModel.permittivityWithScaledImaginaryPart(w, cAl.requireValue(), dampingFactor.requireValue())
      }

      AlGaAsPermittivityModel.ADACHI_GAUSS -> {
        AlGaAsAdachiModelWithGaussianBroadening.permittivity(w, cAl.requireValue())
      }

      AlGaAsPermittivityModel.ADACHI_MOD_GAUSS -> {
        check(dampingFactor != null) { "Gamma parameter 'df' is required for AlGaAs or GaAs layer with Adachi based models" }

        AlGaAsAdachiModelWithGaussianBroadening.permittivityWithScaledImaginaryPart(w, cAl.requireValue(), dampingFactor.requireValue())
      }

      AlGaAsPermittivityModel.ADACHI_T -> {
        AlGaAsSbAdachiModelWithTemperatureDependence(w, cAl.requireValue(), cAs = 1.0, T = temperature).permittivity()
      }

      AlGaAsPermittivityModel.TANGUY_95 -> {
        TODO("TANGUY_95 model is not yet supported for mutable layers")
      }
    }
  }
}

data class MutableGaAs(
  override val d: VarParameter<Double>,
  val dampingFactor: VarParameter<Double>?,
  val permittivityModel: AlGaAsPermittivityModel
) : MutableAlGaAsBase(d, dampingFactor, cAl = DoubleConstParameter.ZERO_CONST, permittivityModel) {

  override fun variableParameters() = listOf(d, dampingFactor).filterNotNull()
}

data class MutableAlGaAs(
  override val d: VarParameter<Double>,
  val dampingFactor: VarParameter<Double>?,
  val cAl: VarParameter<Double>,
  val permittivityModel: AlGaAsPermittivityModel
) : MutableAlGaAsBase(d, dampingFactor, cAl, permittivityModel) {

  override fun variableParameters() = listOf(d, dampingFactor, cAl).filterNotNull()
}