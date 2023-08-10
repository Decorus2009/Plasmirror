//package core.structure.layer.immutable.material
//
//import core.math.Complex
//import core.optics.material.AlGaAsWithGamma.Tanguy95Model
//import core.optics.toEnergy
//import core.structure.layer.immutable.AbstractLayer
//
//class AlGaAsWithGamma(
//  override val d: Double,
//  val cAl: Double,
//  val g: Double, // gamma,
//  val permittivityModel: AlGaAsWithGammaPermittivityModel
//) : AbstractLayer(d) {
//  override fun permittivity(wl: Double, temperature: Double): Complex {
//    when (permittivityModel) {
//      AlGaAsWithGammaPermittivityModel.TANGUY_95 -> {
//        val w = wl.toEnergy()
//
//        return Tanguy95Model.permittivity(w, cAl, g)
//      }
//    }
//  }
//}
