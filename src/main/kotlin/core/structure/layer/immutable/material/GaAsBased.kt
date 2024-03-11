package core.structure.layer.immutable.material

import core.math.Complex
import core.optics.AlGaAsPermittivityModel
import core.optics.AlGaAsPermittivityModel.*
import core.optics.material.AlGaAs.*
import core.optics.material.AlGaAsSb.AlGaAsSbAdachiModelWithTemperatureDependence
import core.optics.material.AlGaAsWithGamma.AlGaAsTanguy1995ManualModel
import core.optics.material.AlGaAsWithGamma.AlGaAsTanguy1995Model
import core.optics.material.AlGaAsWithGamma.AlGaAsTanguy1999Model
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
  private val gamma: Double? = null,
  private val epsInf: Double? = null,
  private val matrixElement: Double? = null, // TODO temporary passed from front
  private val gParam: Double? = null,
  private val infraredPermittivity: Double? = null,
  private val permittivityModel: AlGaAsPermittivityModel,
) : AbstractLayer(d) {

  override fun permittivity(wl: Double, temperature: Double): Complex {
    val w = wl.toEnergy()

    return when (permittivityModel) {
      ADACHI_SIMPLE -> {
        check(dampingFactor != null) { "Damping factor parameter 'df' is required for AlGaAs or GaAs layer with Adachi based models" }

        Adachi1985Model.permittivityWithScaledImaginaryPart(w, cAl, dampingFactor)
      }
      ADACHI_1989 -> {
        check(gamma != null) { "Gamma parameter 'G' is required for AlGaAs or GaAs layer with Adachi based models" }

        Adachi1989Model(cAl, gamma).permittivity(w)
      }
      ADACHI_1992 -> {
        check(gamma != null) { "Gamma parameter 'G' is required for AlGaAs or GaAs layer with Adachi_1992 model" }
        check(epsInf != null) { "Eps infinity parameter 'eps_inf' is required for AlGaAs or GaAs layer with Adachi_1992 model" }

        Adachi1992BookModel(cAl, gamma, epsInf).permittivity(w)
      }
      ADACHI_GAUSS -> {
        AlGaAsDjurisic1999Model.permittivity(w, cAl)
      }
      ADACHI_MOD_GAUSS -> {
        check(dampingFactor != null) { "Damping factor parameter 'df' is required for AlGaAs or GaAs layer with Adachi based models" }

        AlGaAsDjurisic1999Model.permittivityWithScaledImaginaryPart(w, cAl, dampingFactor)
      }
      ADACHI_T -> {
        AlGaAsSbAdachiModelWithTemperatureDependence(w, cAl, cAs = 1.0, T = temperature).permittivity()
      }

      TANGUY_1995 -> {
        check(gamma != null) { "Gamma parameter 'G' is required for AlGaAs or GaAs layer with Tanguy models" }

        return AlGaAsTanguy1995Model(cAl, gamma).permittivity(w)
      }
      TANGUY_1999 -> {
        check(gamma != null) { "Gamma parameter 'G' is required for AlGaAs or GaAs layer with Tanguy models" }
        check(matrixElement != null) { "Matrix element parameter 'matr_el' is required for AlGaAs or GaAs layer with Tanguy models" }
        check(infraredPermittivity != null) { "Infrared permittivity parameter 'eps_infra' is required for AlGaAs or GaAs layer with Tanguy models" }
        check(gParam != null) { "g parameter 'g_param' is required for AlGaAs or GaAs layer with Tanguy99 model" }

        return AlGaAsTanguy1999Model(cAl, gamma, gParam, matrixElement, infraredPermittivity).permittivity(w)
      }

      ADACHI_SIMPLE_TANGUY_1995 -> {
        check(gamma != null) { "Gamma parameter 'G' is required for AlGaAs or GaAs layer with Tanguy models" }

        AlGaAsAdachi1985ModelWithTanguy1995ImaginaryPart(cAl, gamma).permittivity(w)
      }

      TANGUY_95_MANUAL -> {
        check(gamma != null) { "Gamma parameter 'G' is required for AlGaAs or GaAs layer with Tanguy models" }
        check(matrixElement != null) { "Matrix element parameter 'matr_el' is required for AlGaAs or GaAs layer with Tanguy models" }
        check(infraredPermittivity != null) { "Infrared permittivity parameter 'eps_infra' is required for AlGaAs or GaAs layer with Tanguy models" }

        AlGaAsTanguy1995ManualModel(cAl, gamma, matrixElement, infraredPermittivity).permittivity(w)
      }
    }
  }
}

data class GaAs(
  override val d: Double,
  val dampingFactor: Double = 0.0, // default value is used for external media initialization in [core.state.Medium.toLayer]
  val g: Double? = null,
  val epsInf: Double? = null,
  val matrixElement: Double? = null, // TODO temporary passed from front
  val gParam: Double? = null,
  val infraredPermittivity: Double? = null,
  val permittivityModel: AlGaAsPermittivityModel
) : AlGaAsBase(
  d = d,
  dampingFactor = dampingFactor,
  cAl = 0.0,
  gamma = g,
  epsInf = epsInf,
  matrixElement = matrixElement,
  gParam = gParam,
  infraredPermittivity = infraredPermittivity,
  permittivityModel = permittivityModel
)

data class AlGaAs(
  override val d: Double,
  val dampingFactor: Double?,
  val cAl: Double,
  val g: Double? = null,
  val epsInf: Double? = null,
  val matrixElement: Double? = null, // TODO temporary passed from front
  val gParam: Double? = null,
  val infraredPermittivity: Double? = null,
  val permittivityModel: AlGaAsPermittivityModel
) : AlGaAsBase(
  d = d,
  dampingFactor = dampingFactor,
  cAl = cAl,
  gamma = g,
  epsInf = epsInf,
  matrixElement = matrixElement,
  gParam = gParam,
  infraredPermittivity = infraredPermittivity,
  permittivityModel = permittivityModel
)
