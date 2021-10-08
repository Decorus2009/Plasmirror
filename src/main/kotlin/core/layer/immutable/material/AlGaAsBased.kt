package core.layer.immutable.material

import core.layer.ILayer
import core.layer.immutable.AbstractLayer
import core.math.Complex
import core.optics.AdachiBasedPermittivityModel
import core.optics.material.AlGaAs.AlGaAsAdachiModelWithGaussianBroadening
import core.optics.material.AlGaAs.AlGaAsAdachiSimpleModel
import core.optics.material.AlGaAsSb.AlGaAsSbAdachiModelWithTemperatureDependence
import core.optics.toEnergy

/**
 * Base class for AlGaAs-based layers (excluding AlGaAsSb).
 * [dampingFactor] - coefficient between the imaginary and real part of permittivity:
 * im(eps) = [dampingFactor] * re(eps).
 *
 * Optical properties of AlGaAs-based layers are computed via permittivity models.
 * So computation of [n] is provided with 2 steps (see [permittivity]):
 *   1. compute the permittivity obtained from models (maybe with modification of im(eps) part via [dampingFactor]
 *   2. convert it to refractive index
 */
abstract class AlGaAsBase(
  override val d: Double,
  private val dampingFactor: Double,
  private val cAl: Double,
  private val permittivityModel: AdachiBasedPermittivityModel
) : AbstractLayer(d) {

  override fun permittivity(wl: Double, temperature: Double): Complex {
    val w = wl.toEnergy()
    return when (permittivityModel) {
      AdachiBasedPermittivityModel.ADACHI_SIMPLE -> {
        AlGaAsAdachiSimpleModel.permittivityWithScaledImaginaryPart(w, cAl, dampingFactor)
      }
      AdachiBasedPermittivityModel.ADACHI_GAUSS -> {
        AlGaAsAdachiModelWithGaussianBroadening.permittivity(w, cAl)
      }
      AdachiBasedPermittivityModel.ADACHI_MOD_GAUSS -> {
        AlGaAsAdachiModelWithGaussianBroadening.permittivityWithScaledImaginaryPart(w, cAl, dampingFactor)
      }
      AdachiBasedPermittivityModel.ADACHI_T -> {
        AlGaAsSbAdachiModelWithTemperatureDependence(w, cAl, cAs = 1.0, T = temperature).permittivity()
      }
    }
  }
}

data class GaAs(
  override val d: Double,
  val dampingFactor: Double = 0.0, // default value is used for external media initialization in [core.state.Medium.toLayer]
  val permittivityModel: AdachiBasedPermittivityModel
) : AlGaAsBase(d, dampingFactor, cAl = 0.0, permittivityModel) {

  override fun copy() = GaAs(d, dampingFactor, permittivityModel)
}

data class AlGaAs(
  override val d: Double,
  val dampingFactor: Double,
  val cAl: Double,
  val permittivityModel: AdachiBasedPermittivityModel
) : AlGaAsBase(d, dampingFactor, cAl, permittivityModel) {

  override fun copy() = AlGaAs(d, dampingFactor, cAl, permittivityModel)
}