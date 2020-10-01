package core.optics.semiconductor.AlGaAs

import core.Complex
import core.optics.PermittivityModel
import core.optics.PermittivityModel.*
import core.optics.semiconductor.AlGaAsSb.temperatureDependent.AdachiFullTemperatureDependent
import core.optics.toEnergy

object AlGaAs {
  fun permittivity(wl: Double, k: Double, cAl: Double, temperature: Double, permittivityModel: PermittivityModel): Complex {
    val w = wl.toEnergy()
    return when (permittivityModel) {
      ADACHI_SIMPLE -> {
        epsAdachiSimple(w, cAl).let { Complex(it.real, it.real * k) }
      }
      ADACHI_FULL_T -> {
        AdachiFullTemperatureDependent(w, cAl, cAs = 0.0, temperature = temperature).compute()
      }
      ADACHI_FULL_GAUSS -> {
        AdachiFullWithGaussianBroadening(w, cAl).compute()
      }
      ADACHI_FULL_GAUSS_MOD -> {
        AdachiFullWithGaussianBroadening(w, cAl).compute().let { eps ->
          Complex(
            eps.real,
            if (w >= AdachiFullWithGaussianBroadening.E0(cAl)) eps.imaginary else eps.real * k
          )
        }
      }
    }
  }
}

// TODO get rid of
///** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
///**
// * OpticalConstants (Gauss-Adachi)
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
// */
//private val GaussAdachiIntersections = mutableMapOf<Double, Double>()
//
///**
// * If w < intersection energy, returns eps and n computed by the Adachi'85 approximation
// * (with imaginary part computed by the Gauss approximation)
// * Else returns eps computed using the Gauss approximation
// *
// * Finds intersection point for both approaches
// * using appropriate small energy range (including E0) and a fixed energy precision
// *
// * For Re(eps):
// * Adachi describes experimental data for real part (only) of eps better (why?)
// * than full approach using Gaussian broadening below E0.
// *
// * Lorentz broadening is even worse.
// * At the very critical point E0 Adachi approach is not applicable.
// * The idea is to staple two curves for eps below E0, Adachi and Gauss, at the point of their intersection (eV).
// *
// * It was found that the intersection point is located within the energy range (1.4 : 1.8) eV for x[0.0 : 0.5]
// * The intersection point is always lower than E0. But at the much lower energies there are another intersections.
// * We don't need to consider them. There will be only Adachi-based computation of permittivity.
// */
//private fun findIntersection(x: Double) {
//  println("Finding intersection for $x")
//  val w = arrayListOf<Double>()
//  val epsGauss = arrayListOf<Complex>()
//  val nGauss = arrayListOf<Complex>()
//  val epsAdachi = arrayListOf<Complex>()
//  val nAdachi = arrayListOf<Complex>()
//
//  /* compute in range [from; to] */
//  val from = 1.4
//  val to = 1.8
//  val step = 0.001
//  var wTmp = from
//  while (wTmp <= to) {
//    wTmp = wTmp.round() // 12.000000000001 -> 12.0 && 13.99999999999 -> 14.0
//    w.add(wTmp)
//    epsGauss.add(epsAdachiGaussianBroadening(wTmp, x))
//    nGauss.add(epsAdachiGaussianBroadening(wTmp, x).toRefractiveIndex())
//    epsAdachi.add(epsAdachiSimple(wTmp, x))
//    nAdachi.add(epsAdachiSimple(wTmp, x).toRefractiveIndex())
//    wTmp += step
//  }
//  /**
//   * Class to keep difference of nGauss.real and nAdachi at w
//   */
//  data class Diff(val w: Double, val diff: Double)
//
//  /**
//   * Energy range within which the only one closest to the E0 intersection is found
//   */
//  val upperBound = E0(x)
//  val nGaussReal = w.indices.map { nGauss[it].real }
//  val nAdachiReal = w.indices.map { nAdachi[it].real }
//  GaussAdachiIntersections.putIfAbsent(
//    x,
//    w.indices
//      .map { Diff(w[it], diff = abs(nGaussReal[it] - nAdachiReal[it])) }
//      .filter { it.w > 1.4 && it.w < upperBound }.minBy { it.diff }!!.w
//  )
//}
//
//private fun epsGaussAdachi(w: Double, x: Double): Complex {
//  if (GaussAdachiIntersections[x] == null) {
//    findIntersection(x)
//  }
//  return if (w < GaussAdachiIntersections[x]!!) {
//    Complex(epsAdachiSimple(w, x).real, epsAdachiGaussianBroadening(w, x).imaginary)
//  } else {
//    epsAdachiGaussianBroadening(w, x)
//  }
//}
