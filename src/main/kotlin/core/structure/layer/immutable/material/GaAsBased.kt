package core.structure.layer.immutable.material

import core.math.Complex
import core.optics.AlGaAsPermittivityModel
import core.optics.material.AlGaAs.*
import core.optics.material.AlGaAsSb.AlGaAsSbAdachiModelWithTemperatureDependence
import core.optics.material.AlGaAsWithGamma.Tanguy95Model
import core.optics.toEnergy
import core.structure.layer.immutable.AbstractLayer

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
  private val dampingFactor: Double?,
  private val cAl: Double,
  private val G: Double? = null,
  private val matrixElement: Double? = null, // TODO temporary passed from front
  private val permittivityModel: AlGaAsPermittivityModel,
) : AbstractLayer(d) {

  override fun permittivity(wl: Double, temperature: Double): Complex {
    val w = wl.toEnergy()

    return when (permittivityModel) {
      AlGaAsPermittivityModel.ADACHI_SIMPLE -> {
        check(dampingFactor != null) { "Gamma parameter 'df' is required for AlGaAs or GaAs layer with Adachi based models" }

        AlGaAsAdachiSimpleModel.permittivityWithScaledImaginaryPart(w, cAl, dampingFactor)
      }
      AlGaAsPermittivityModel.ADACHI_GAUSS -> {
        AlGaAsAdachiModelWithGaussianBroadening.permittivity(w, cAl)
      }
      AlGaAsPermittivityModel.ADACHI_MOD_GAUSS -> {
        check(dampingFactor != null) { "Gamma parameter 'df' is required for AlGaAs or GaAs layer with Adachi based models" }

        AlGaAsAdachiModelWithGaussianBroadening.permittivityWithScaledImaginaryPart(w, cAl, dampingFactor)
      }
      AlGaAsPermittivityModel.ADACHI_T -> {
        AlGaAsSbAdachiModelWithTemperatureDependence(w, cAl, cAs = 1.0, T = temperature).permittivity()
      }
      AlGaAsPermittivityModel.TANGUY_95 -> {
        check(G != null) { "Gamma parameter 'G' is required for AlGaAs or GaAs layer with Tanguy models" }
        check(matrixElement != null) { "Gamma parameter 'matr_el' is required for AlGaAs or GaAs layer with Tanguy models" }

        return (Tanguy95Model(cAl, G, matrixElement).permittivity(w) * Complex.of(1E-17))
          .also { println("$wl\t${it.real}\t${it.imaginary}") }
      }
    }
  }
}

data class GaAs(
  override val d: Double,
  val dampingFactor: Double = 0.0, // default value is used for external media initialization in [core.state.Medium.toLayer]
  val g: Double? = null,
  val matrixElement: Double? = null, // TODO temporary passed from front
  val permittivityModel: AlGaAsPermittivityModel
) : AlGaAsBase(d, dampingFactor, cAl = 0.0, g, matrixElement, permittivityModel)

data class AlGaAs(
  override val d: Double,
  val dampingFactor: Double?,
  val cAl: Double,
  val g: Double? = null,
  val matrixElement: Double? = null, // TODO temporary passed from front
  val permittivityModel: AlGaAsPermittivityModel
) : AlGaAsBase(d, dampingFactor, cAl, g, matrixElement, permittivityModel)
