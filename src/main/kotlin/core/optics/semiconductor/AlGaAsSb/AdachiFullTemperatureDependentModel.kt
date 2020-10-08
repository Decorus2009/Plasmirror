package core.optics.semiconductor.AlGaAsSb

import core.Complex
import core.optics.toRefractiveIndex
import kotlin.math.pow

/**
 * It's a class, not a function to avoid using [cAl], [cAs] and [temperature] as arguments in all the functions
 * of computation stack below. It's much more convenient to keep this values within an internal state of the class instance,
 * however initialized each time the permittivity computation is needed
 */
class AdachiFullTemperatureDependentModel(
  private val w: Double,
  private val cAl: Double,
  private val cAs: Double,
  private val temperature: Double
) {
  fun refractiveIndex() = permittivity().toRefractiveIndex()

  fun permittivity() = eps1() + eps2() + eps3() + eps4()

  private fun eps1(): Complex {
    //@formatter:off
    val A        = A()
    val E0       = E0()
    val E0Delta0 = E0Delta0()
    val gamma    = GammaE0Delta0()

    val common   = Complex.I * gamma + w
    //@formatter:on

    val f = { z: Complex ->
      (Complex.ONE * 2.0 - (Complex.ONE + z).sqrt() - (Complex.ONE - z).sqrt()) / (z * z)
    }

    val summand1 = f(common / E0)
    val summand2 = f(common / E0Delta0) * 0.5 * (E0 / E0Delta0).pow(1.5)

    return (summand1 + summand2) * A / E0.pow(1.5)
  }

  private fun eps2(): Complex {
    //@formatter:off
    val gamma    = GammaE1Delta1()
    val E1       = E1()
    val Delta1   = Delta1()
    val E1Delta1 = E1Delta1()
    val a_lc     = a_lc()

    val common0 = Complex.I * gamma + w
    val common1 = (common0 / E1      ).pow(2.0)
    val common2 = (common0 / E1Delta1).pow(2.0)
    //@formatter:on

    val B1 = 44.0 * (E1 + 1.0 / 3.0 * Delta1) / (a_lc * E1.pow(2))
    val B2 = 44.0 * (E1 + 2.0 / 3.0 * Delta1) / (a_lc * E1Delta1.pow(2))

    val ln1 = (Complex.ONE - common1).log()
    val ln2 = (Complex.ONE - common2).log()

    return -Complex.ONE * B1 / common1 * ln1 - Complex.ONE * B2 / common2 * ln2
  }

  private fun eps3(): Complex {
    //@formatter:off
    val E2    = E2()
    val C     = C()
    val gamma = GammaE2()
    //@formatter:on

    val common = Complex(E2.pow(2))
    val numerator = common * C
    val denominator = common - w.pow(2) - Complex.I * w * gamma

    return numerator / denominator
  }

  private fun eps4(): Complex {
    //@formatter:off
    val D         = Complex(D())
    val EIndirect = Complex(EIndirect())
    val Ec        = E1() // "Ec is a high-energy cutoff, assumed equal to E1"
    val gamma     = GammaEIndirect()
    //@formatter:on

    val common0 = Complex.I * gamma + w
    val common1 = EIndirect / common0

    val ln1 = (Complex.ONE * Ec / EIndirect).log()
    val ln2 = ((common0 + Ec) / (common0 + EIndirect)).log()
    val ln3 = ((common0 - Ec) / (common0 - EIndirect)).log()

    val summand1 = -Complex.ONE * common1.pow(2.0) * ln1
    val summand2 = Complex.ONE / 2.0 * (Complex.ONE + common1).pow(2.0) * ln2
    val summand3 = Complex.ONE / 2.0 * (Complex.ONE - common1).pow(2.0) * ln3

    return D * 2.0 / Math.PI * (summand1 + summand2 + summand3)
  }


  /** -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- critical point energies -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- **/
  //@formatter:off
  private fun E0()        = energy(CriticalPoints.E0)
  private fun E0Delta0()  = energy(CriticalPoints.E0Delta0)
  private fun E1()        = energy(CriticalPoints.E1)
  private fun E1Delta1()  = energy(CriticalPoints.E1Delta1)
  private fun Delta1()    = energy(CriticalPoints.Delta1)
  private fun E2()        = energy(CriticalPoints.E2)
  private fun EIndirect() = energy(CriticalPoints.EIndirect)
  /** -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- **/

  /** -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_ model parameters -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- **/
  private fun A()              = parameter(ModelParameters.A)
  private fun C()              = parameter(ModelParameters.C)
  private fun D()              = parameter(ModelParameters.D)
  private fun GammaE0Delta0()  = parameter(ModelParameters.GammaE0Delta0)
  private fun GammaE1Delta1()  = parameter(ModelParameters.GammaE1Delta1)
  private fun GammaE2()        = parameter(ModelParameters.GammaE2)
  private fun GammaEIndirect() = parameter(ModelParameters.GammaEIndirect)
  private fun a_lc()           = parameter(ModelParameters.aLattice)
  //@formatter:on
  /** -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- **/


  /**
   * Basic function for computation of model parameters:
   *   A(E0/E0 + Delta0)
   *   C(E2)
   *   D(EIndirect)
   *   Gamma(E0/E0 + Delta0)
   *   Gamma(E1/E1 + Delta1)
   *   Gamma(E2)
   *   Gamma(EIndirect)
   *   a_lc
   */
  private fun parameter(parameter: String): Double {
    /**
     * Temperature-dependent gamma for E1/E1 + Delta1 and E2 critical points.
     */
    fun gamma(binary: String) = Table_III.values.getValue(parameter).getValue(binary).gammaAt(temperature)

    /**
     * Temperature-dependent lattice constant
     */
    fun latticeConstant(binary: String) = Table_IV.values.getValue(binary).latticeConstantAt(temperature)

    var B_AlAs: Double
    var B_AlSb: Double
    var B_GaAs: Double
    var B_GaSb: Double

    when (parameter) {
      ModelParameters.GammaE1Delta1, ModelParameters.GammaE2 -> {
        B_AlAs = gamma(Binaries.AlAs)
        B_AlSb = gamma(Binaries.AlSb)
        B_GaAs = gamma(Binaries.GaAs)
        B_GaSb = gamma(Binaries.GaSb)
      }
      ModelParameters.aLattice -> {
        B_AlAs = latticeConstant(Binaries.AlAs)
        B_AlSb = latticeConstant(Binaries.AlSb)
        B_GaAs = latticeConstant(Binaries.GaAs)
        B_GaSb = latticeConstant(Binaries.GaSb)
      }
      else -> with(Table_I.values.getValue(parameter)) {
        B_AlAs = AlAsValue
        B_AlSb = AlSbValue
        B_GaAs = GaAsValue
        B_GaSb = GaSbValue
      }
    }

    return vegard(
      x = cAl,
      y = cAs,
      B_AC = B_AlAs,
      B_AD = B_AlSb,
      B_BC = B_GaAs,
      B_BD = B_GaSb,
      bowingA_B = Table_VI.values.getValue(parameter).bowingA_B,
      bowingC_D = Table_VI.values.getValue(parameter).bowingC_D
    )
  }

  /**
   * Basic function for computation of critical point energies:
   *   E0
   *   E0 + Delta0
   *   E1
   *   Delta1 (NB: not present in table, computed using E1 and E1 + Delta1)
   *   E1 + Delta1
   *   E2
   *   EIndirect
   */
  private fun energy(transition: String): Double {
    fun Map<String, Table_II.Record>.energy() = getValue(transition).energyAt(temperature)

    return vegard(
      cAl,
      cAs,
      B_AC = Table_II.AlAsValues.energy(),
      B_AD = Table_II.AlSbValues.energy(),
      B_BC = Table_II.GaAsValues.energy(),
      B_BD = Table_II.GaSbValues.energy(),
      bowingA_B = Table_V.values.getValue(transition).bowingA_B(cAs),
      bowingC_D = Table_V.values.getValue(transition).bowingC_D(cAl)
    )
  }
}

/**
 * Vegard's law for quaternary alloys using binary components
 */
private fun vegard(
  x: Double,
  y: Double,
  B_AC: Double,
  B_AD: Double,
  B_BC: Double,
  B_BD: Double,
  bowingA_B: Double,
  bowingC_D: Double
) = x * y * B_AC + x * (1 - y) * B_AD + (1 - x) * y * B_BC + (1 - x) * (1 - y) * B_BD + x * (1 - x) * bowingA_B + y * (1 - y) * bowingC_D


