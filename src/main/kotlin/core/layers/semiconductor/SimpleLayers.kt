package core.layers.semiconductor

import core.Complex
import core.TransferMatrix
import core.optics.*
import core.optics.semiconductor.AlGaAs.AdachiFullWithGaussianBroadeningModel
import core.optics.semiconductor.AlGaAs.AdachiSimpleModel
import core.optics.semiconductor.AlGaAsSb.AdachiFullTemperatureDependentModel
import core.optics.semiconductor.AlGaAsSb.AlGaAsSb
import java.lang.Math.PI

/**
 * Abstract layer without excitons
 *
 * [d] thickness
 * [n] refractive index
 * [matrix] transfer matrix
 */
interface Layer {
  val d: Double

  fun permittivity(wl: Double, temperature: Double): Complex

  fun n(wl: Double, temperature: Double) = permittivity(wl, temperature).toRefractiveIndex()

  fun extinctionCoefficient(wl: Double, temperature: Double) = n(wl, temperature).toExtinctionCoefficientAt(wl)

  /**
   * @return transfer matrix for a layer without excitons
   * polarization is unused
   */
  fun matrix(wl: Double, pol: Polarization, angle: Double, temperature: Double) = TransferMatrix().apply {
    val n = n(wl, temperature)
    val cos = cosThetaInLayer(n, wl, angle, temperature)
    var phi = Complex(2.0 * PI * d / wl) * n * cos
    if (phi.imaginary < 0.0) {
      phi *= -1.0
    }
    this[0, 0] = Complex((phi * Complex.I).exp())
    this[1, 1] = Complex((phi * Complex.I * -1.0).exp())
    setAntiDiagonal(Complex(Complex.ZERO))
  }
}

/**
 * Base class for AlGaAs-based layers (excluding AlGaAsSb).
 * [kToN] argument is the coefficient between the imaginary and real parts of refractive index.
 *
 * Optical properties of AlGaAs-based layers are computed via permittivity models.
 * So computation of [n] is provided with 3 steps (see [refractiveIndex]):
 *   1. compute the "original" (initial) permittivity obtained from models
 *   2. convert it to refractive index
 *   3. modify imaginary part of refractive index with accordance to permittivity model and specified [kToN] value
 *
 * Computation of [permittivity] is then provided using the refractive index with modified imaginary part
 *
 * Such a strange double transfer of computation is caused by the semantics of [kToN] parameter which influences
 * the imaginary part of *refractive index* whereas we compute optical properties using permittivity models first
 */
abstract class AlGaAsBase(
  override val d: Double,
  private val kToN: Double,
  private val cAl: Double,
  private val permittivityModel: PermittivityModel
) : Layer {
  override fun n(wl: Double, temperature: Double) = refractiveIndex(wl, kToN, cAl, temperature, permittivityModel)

  override fun permittivity(wl: Double, temperature: Double) = n(wl, temperature).pow(2.0)
}

open class GaAs(
  d: Double,
  kToN: Double = 0.0,
  cAl: Double = 0.0,
  /** default value for external media initialization in [core.state.Medium.toLayer] */
  permittivityModel: PermittivityModel
) : AlGaAsBase(d, kToN, cAl, permittivityModel)

/**
 * [kToN] for Adachi computation n = (Re(n); Im(n) = k * Re(n)), see [refractiveIndex]
 */
open class AlGaAs(
  d: Double,
  kToN: Double,
  cAl: Double,
  permittivityModel: PermittivityModel
) : AlGaAsBase(d, kToN, cAl, permittivityModel)

open class ConstRefractiveIndexLayer(
  override val d: Double,
  val n: Complex
) : Layer {
  /** [temperature] is unused but required */
  override fun permittivity(wl: Double, temperature: Double) = n * n
}

class AlGaAsSb(
  override val d: Double,
  private val cAl: Double,
  private val cAs: Double
) : Layer {
  override fun permittivity(wl: Double, temperature: Double) = AlGaAsSb.permittivity(wl, cAl, cAs, temperature)
}

/**
 * Refractive index of Al(x)Ga(1-x)As alloy with user defined imaginary part (via [kToN] coefficient)
 * for certain permittivity models (see the `when` branch below)
 */
private fun refractiveIndex(
  wl: Double,
  kToN: Double,
  cAl: Double,
  temperature: Double,
  permittivityModel: PermittivityModel
): Complex {
  val w = wl.toEnergy()
  return when (permittivityModel) {
    PermittivityModel.ADACHI_GAUSS -> {
      AdachiFullWithGaussianBroadeningModel(w, cAl).refractiveIndex()
    }
    PermittivityModel.ADACHI_T -> {
      AdachiFullTemperatureDependentModel(w, cAl, cAs = 1.0, temperature = temperature).refractiveIndex()
    }
    PermittivityModel.ADACHI_SIMPLE -> {
      AdachiSimpleModel.refractiveIndex(w, cAl).let { refInd ->
        Complex(
          refInd.real,
          refInd.real * kToN
        )
      }
    }
    PermittivityModel.ADACHI_GAUSS_MOD -> {
      AdachiFullWithGaussianBroadeningModel(w, cAl).refractiveIndex().let { refInd ->
        Complex(
          refInd.real,
          if (w >= AdachiFullWithGaussianBroadeningModel.E0(cAl)) refInd.imaginary else refInd.real * kToN
        )
      }
    }
  }
}