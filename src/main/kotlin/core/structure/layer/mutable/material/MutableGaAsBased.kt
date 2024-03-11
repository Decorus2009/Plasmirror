package core.structure.layer.mutable.material

import core.math.Complex
import core.optics.AlGaAsPermittivityModel
import core.optics.AlGaAsPermittivityModel.*
import core.optics.material.AlGaAs.AlGaAsDjurisic1999Model
import core.optics.material.AlGaAs.Adachi1985Model
import core.optics.material.AlGaAs.AlGaAsAdachi1985ModelWithTanguy1995ImaginaryPart
import core.optics.material.AlGaAsSb.AlGaAsSbAdachiModelWithTemperatureDependence
import core.optics.material.AlGaAsWithGamma.AlGaAsTanguy1995ManualModel
import core.optics.material.AlGaAsWithGamma.AlGaAsTanguy1995Model
import core.optics.material.AlGaAsWithGamma.AlGaAsTanguy1999Model
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
  private val gamma: VarParameter<Double>? = null,
  private val matrixElement: VarParameter<Double>? = null,
  private val gParam: VarParameter<Double>? = null,
  private val infraredPermittivity: VarParameter<Double>? = null,
  private val permittivityModel: AlGaAsPermittivityModel
) : AbstractMutableLayer(d) {

  override fun permittivity(wl: Double, temperature: Double): Complex {
    val w = wl.toEnergy()

    return when (permittivityModel) {
      ADACHI_SIMPLE -> {
        check(dampingFactor != null) { "Gamma parameter 'df' is required for AlGaAs or GaAs layer with Adachi based models" }

        Adachi1985Model.permittivityWithScaledImaginaryPart(
          w,
          cAl.requireValue(),
          dampingFactor.requireValue()
        )
      }

      ADACHI_1989 -> TODO("ADACHI_1989 model not implemented")
      ADACHI_1992 -> TODO("ADACHI_1989 model not implemented")

      ADACHI_GAUSS -> {
        AlGaAsDjurisic1999Model.permittivity(
          w,
          cAl.requireValue()
        )
      }

      ADACHI_MOD_GAUSS -> {
        check(dampingFactor != null) { "Gamma parameter 'df' is required for AlGaAs or GaAs layer with Adachi based models" }

        AlGaAsDjurisic1999Model.permittivityWithScaledImaginaryPart(
          w,
          cAl.requireValue(),
          dampingFactor.requireValue()
        )
      }

      ADACHI_T -> {
        AlGaAsSbAdachiModelWithTemperatureDependence(
          w,
          cAl.requireValue(),
          cAs = 1.0,
          T = temperature
        ).permittivity()
      }

      TANGUY_1995 -> {
        check(gamma != null) { "Gamma parameter 'G' is required for AlGaAs or GaAs layer with Tanguy models" }

        return AlGaAsTanguy1995Model(
          cAl.requireValue(),
          gamma.requireValue()
        ).permittivity(w)
      }

      TANGUY_1999 -> {
        check(gamma != null) { "Gamma parameter 'G' is required for AlGaAs or GaAs layer with Tanguy models" }
        check(matrixElement != null) { "Matrix element parameter 'matr_el' is required for AlGaAs or GaAs layer with Tanguy models" }
        check(infraredPermittivity != null) { "Infrared permittivity parameter 'eps_infra' is required for AlGaAs or GaAs layer with Tanguy models" }
        check(gParam != null) { "g parameter 'g_param' is required for AlGaAs or GaAs layer with Tanguy99 model" }

        return AlGaAsTanguy1999Model(
          cAl.requireValue(),
          gamma.requireValue(),
          gParam.requireValue(),
          matrixElement.requireValue(),
          infraredPermittivity.requireValue()
        ).permittivity(w)
      }

      ADACHI_SIMPLE_TANGUY_1995 -> {
        check(gamma != null) { "Gamma parameter 'G' is required for AlGaAs or GaAs layer with Tanguy models" }

        AlGaAsAdachi1985ModelWithTanguy1995ImaginaryPart(
          cAl.requireValue(),
          gamma.requireValue()
        ).permittivity(w)
      }

      TANGUY_95_MANUAL -> {
        check(gamma != null) { "Gamma parameter 'G' is required for AlGaAs or GaAs layer with Tanguy models" }
        check(matrixElement != null) { "Matrix element parameter 'matr_el' is required for AlGaAs or GaAs layer with Tanguy models" }
        check(infraredPermittivity != null) { "Infrared permittivity parameter 'eps_infra' is required for AlGaAs or GaAs layer with Tanguy models" }

        AlGaAsTanguy1995ManualModel(
          cAl.requireValue(),
          gamma.requireValue(),
          matrixElement.requireValue(),
          infraredPermittivity.requireValue()
        ).permittivity(w)      }
    }
  }
}

data class MutableGaAs(
  override val d: VarParameter<Double>,
  val dampingFactor: VarParameter<Double>?,
  private val gamma: VarParameter<Double>? = null,
  private val matrixElement: VarParameter<Double>? = null,
  private val gParam: VarParameter<Double>? = null,
  private val infraredPermittivity: VarParameter<Double>? = null,
  val permittivityModel: AlGaAsPermittivityModel
) : MutableAlGaAsBase(
  d = d,
  dampingFactor = dampingFactor,
  cAl = DoubleConstParameter.ZERO_CONST,
  gamma = gamma,
  matrixElement = matrixElement,
  gParam = gParam,
  infraredPermittivity = infraredPermittivity,
  permittivityModel = permittivityModel
) {

  override fun variableParameters() = listOfNotNull(
    d,
    dampingFactor,
    gamma,
    matrixElement,
    gParam,
    infraredPermittivity
  )
}

data class MutableAlGaAs(
  override val d: VarParameter<Double>,
  val dampingFactor: VarParameter<Double>?,
  val cAl: VarParameter<Double>,
  private val gamma: VarParameter<Double>? = null,
  private val matrixElement: VarParameter<Double>? = null,
  private val gParam: VarParameter<Double>? = null,
  private val infraredPermittivity: VarParameter<Double>? = null,
  val permittivityModel: AlGaAsPermittivityModel
) : MutableAlGaAsBase(
  d = d,
  dampingFactor = dampingFactor,
  cAl = cAl,
  gamma = gamma,
  matrixElement = matrixElement,
  gParam = gParam,
  infraredPermittivity = infraredPermittivity,
  permittivityModel = permittivityModel
) {

  override fun variableParameters() = listOfNotNull(
    d,
    dampingFactor,
    cAl,
    gamma,
    matrixElement,
    gParam,
    infraredPermittivity
  )
}