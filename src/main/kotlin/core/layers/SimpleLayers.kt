package core.layers

import core.math.*
import core.optics.*
import core.optics.semiconductor.AlGaAs.AdachiModelWithGaussianBroadening
import core.optics.semiconductor.AlGaAs.AdachiSimpleModel
import core.optics.semiconductor.AlGaAsSb.AdachiModelWithTemperatureDependence
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
  private val permittivityModel: PermittivityModel
) : Layer {
  override fun permittivity(wl: Double, temperature: Double): Complex {
    val w = wl.toEnergy()
    return when (permittivityModel) {
      PermittivityModel.ADACHI_SIMPLE -> {
        AdachiSimpleModel.permittivityWithScaledImaginaryPart(w, cAl, dampingFactor)
      }
      PermittivityModel.ADACHI_GAUSS -> {
        AdachiModelWithGaussianBroadening.permittivity(w, cAl)
      }
      PermittivityModel.ADACHI_MOD_GAUSS -> {
        AdachiModelWithGaussianBroadening.permittivityWithScaledImaginaryPart(w, cAl, dampingFactor)
      }
      PermittivityModel.ADACHI_T -> {
        AdachiModelWithTemperatureDependence(w, cAl, cAs = 1.0, temperature = temperature).permittivity()
      }
    }
  }
}

class GaAs(
  d: Double,
  dampingFactor: Double = 0.0,
  cAl: Double = 0.0,
  /** default value for external media initialization in [core.state.Medium.toLayer] */
  permittivityModel: PermittivityModel
) : AlGaAsBase(d, dampingFactor, cAl, permittivityModel)

class AlGaAs(
  d: Double,
  dampingFactor: Double,
  cAl: Double,
  permittivityModel: PermittivityModel
) : AlGaAsBase(d, dampingFactor, cAl, permittivityModel)

class AlGaAsSb(
  override val d: Double,
  private val cAl: Double,
  private val cAs: Double
) : Layer {
  override fun permittivity(wl: Double, temperature: Double) = AlGaAsSb.permittivity(wl, cAl, cAs, temperature)
}

class ConstPermittivityLayer(
  override val d: Double,
  val eps: Complex
) : Layer {
  /** [temperature] is unused but required */
  override fun permittivity(wl: Double, temperature: Double) = eps
}

class ExpressionBasedPermittivityLayer(
  override val d: Double,
  epsExpr: String
) : Layer {
  private val expressionEvaluator = ExpressionEvaluator(epsExpr)

  init {
    expressionEvaluator.prepare()
  }

  override fun permittivity(wl: Double, temperature: Double) =
    // TODO think of .let { Complex(it.yReal, it.yImaginary ?: 0.0) } -> toComplex
    expressionEvaluator.compute(x = wl).let { Complex(it.yReal, it.yImaginary ?: 0.0) }
}