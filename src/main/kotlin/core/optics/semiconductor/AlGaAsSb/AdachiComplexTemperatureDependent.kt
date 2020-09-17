package core.optics.semiconductor.AlGaAsSb

import core.Complex
import kotlin.math.pow

/**
 * Binary components:
 * AC = AlAs
 * AD = AlSb
 * BC = GaAs
 * BD = GaSb
 */
fun epsAdachiComplexTDependent(w: Double, cAl: Double, cAs: Double, T: Double) =
  eps1(w, cAl, cAs, T) + eps2(w, x, T) + eps3(w, x, T) + eps4(w, x, T)

/**
 * Al(x)Ga(1-x)As(y)Sb(1-y)
 * [w] energy
 * [cAl] Al concentration
 * [cAs] As concentration
 */
private fun eps1(w: Double, cAl: Double, cAs: Double, T: Double): Complex {
  //@formatter:off
  val A        = A(cAl, cAs, T)
  val E0       = E0(cAl, cAs, T)
  val E0Delta0 = E0Delta0(cAl, cAs, T)
  val gamma    = GammaE0Delta0(cAl, cAs, T)
  val common   = Complex.I * gamma + w
  //@formatter:on

  val f = { z: Complex ->
    (Complex.ONE * 2.0 - (Complex.ONE + z).sqrt() - (Complex.ONE - z).sqrt()) / (z * z)
  }

  val summand1 = f(common / E0)
  val summand2 = f(common / E0Delta0) * 0.5 * (E0 / E0Delta0).pow(1.5)
  return (summand1 + summand2) * A / E0.pow(1.5)
}




private fun E0(cAl: Double, cAs: Double, T: Double) = energy(CriticalPoints.E0, cAl, cAs, T)

private fun E0Delta0(cAl: Double, cAs: Double, T: Double) = energy(CriticalPoints.E0Delta0, cAl, cAs, T)

private fun E1(cAl: Double, cAs: Double, T: Double) = energy(CriticalPoints.E1, cAl, cAs, T)

private fun E1Delta1(cAl: Double, cAs: Double, T: Double) = energy(CriticalPoints.E1Delta1, cAl, cAs, T)

private fun E2(cAl: Double, cAs: Double, T: Double) = energy(CriticalPoints.E2, cAl, cAs, T)

private fun EIndirect(cAl: Double, cAs: Double, T: Double) = energy(CriticalPoints.EIndirect, cAl, cAs, T)


private fun A(cAl: Double, cAs: Double, T: Double) = parameter(ModelParameters.A, cAl, cAs, T)

private fun GammaE0Delta0(cAl: Double, cAs: Double, T: Double) = parameter()




// TODO check in debug against manual calculation
/**
 * Basic function for computation of model parameters:
 * A(E0/E0 + delta0), C(E2), D(EIndirect), Gamma(E0/E0 + delta0), Gamma(E1/E1 + delta1), Gamma(E2), Gamma(EIndirect)
 */
private fun parameter(parameter: String, cAl: Double, cAs: Double, T: Double): Double {
  /**
   * This function captures [parameter] and [T] so it's easy to call it for Gamma(E1/E1 + delta1), Gamma(E2)
   */
  fun TDependentGammaFor(binary: String) =
    Table_III.values.getValue(parameter).getValue(binary).gammaAt(T)

  var B_AlAs: Double
  var B_AlSb: Double
  var B_GaAs: Double
  var B_GaSb: Double

  when (parameter) {
    ModelParameters.GammaE1Delta1, ModelParameters.GammaE2 -> {
      B_AlAs = TDependentGammaFor(Binaries.AlAs)
      B_AlSb = TDependentGammaFor(Binaries.AlSb)
      B_GaAs = TDependentGammaFor(Binaries.GaAs)
      B_GaSb = TDependentGammaFor(Binaries.GaSb)
    }
    else -> with(Table_I.values.getValue(parameter)) {
      B_AlAs = valueForAlAs
      B_AlSb = valueForAlSb
      B_GaAs = valueForGaAs
      B_GaSb = valueForGaSb
    }
  }

  return vegardQuaternaryViaBinaries(
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
 * NB: binary bowing parameters (C_A-B and C_C-D in the paper)
 * aren't provided in the paper for all considered transitions (E0 - EIndirect) and are supposed to be == 0.0
 */
private fun energy(
  transition: String,
  cAl: Double,
  cAs: Double,
  T: Double,
  bowingA_B: Double = 0.0,
  bowingC_D: Double = 0.0
): Double {
  fun Map<String, Table_II.Record>.energy() = getValue(transition).energyAt(T)

  return vegardQuaternaryViaBinaries(
    cAl,
    cAs,
    B_AC = Table_II.AlAsValues.energy(),
    B_AD = Table_II.AlSbValues.energy(),
    B_BC = Table_II.GaAsValues.energy(),
    B_BD = Table_II.GaSbValues.energy(),
    bowingA_B = bowingA_B,
    bowingC_D = bowingC_D
  )
}













/**
 * Vegard's law for quaternary alloys
 */
private fun vegardQuaternaryViaBinaries(
  x: Double, y: Double,
  B_AC: Double, B_AD: Double, B_BC: Double, B_BD: Double,
  bowingA_B: Double, bowingC_D: Double
) = x * y * B_AC + x * (1 - y) * B_AD + (1 - x) * y * B_BC + (1 - x) * (1 - y) * B_BD + x * (1 - x) * bowingA_B + y * (1 - y) * bowingC_D

/**
 * Semi-empirical Varshni equation
 */
internal fun varshni(valueAt0K: Double, alpha: Double, beta: Double, T: Double) =
  valueAt0K - alpha * T.pow(2) / (T + beta)



/*
private fun vegardQuaternaryViaTernaries(
  x: Double,
  y: Double,
  T_ABC: Double,
  T_ABD: Double,
  T_ACD: Double,
  T_BCD: Double
): Double {
  val c1 = 1 - x
  val c2 = 1 - y

  val summand1 = x * c1 * (y * T_ABC + c2 * T_ABD)
  val summand2 = y * c2 * (x * T_ACD + c1 * T_BCD)
  return 1.0 / (x * c1 + y * c2) * (summand1 + summand2)
}

/**
 * ABC = AlGaAs
 * AC = AlAs
 * BC = GaAs
 * */
fun T_ABC(x: Double, B_AC: Double, B_BC: Double, bowing: Double) = vegardTernaryViaBinaries(x, B_AC, B_BC, bowing)

/**
 * ABD = AlGaSb
 * AD = AlSb
 * BD = GaSb
 * */
fun T_ABD(x: Double, B_AD: Double, B_BD: Double, bowing: Double) = vegardTernaryViaBinaries(x, B_AD, B_BD, bowing)

/**
 * ACD = AlAsSb
 * AD = AlSb
 * CD = AsSb
 * */
fun T_ACD(y: Double, B_AD: Double, B_CD: Double, bowing: Double) = vegardTernaryViaBinaries(y, B_AD, B_CD, bowing)

/**
 * BCD = GaAsSb
 * BD = GaSb
 * CD = AsSb
 * */
fun T_BCD(y: Double, B_BD: Double, B_CD: Double, bowing: Double) = vegardTernaryViaBinaries(y, B_BD, B_CD, bowing)

private fun vegardTernaryViaBinaries(x: Double, B1: Double, B2: Double, bowing: Double) =
  x * B1 + (1 - x) * B2 + x * (1 - x) * bowing

 */