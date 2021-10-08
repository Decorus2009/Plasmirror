package core.state

import com.fasterxml.jackson.annotation.JsonProperty
import core.layer.immutable.material.*
import core.math.Complex
import core.optics.AdachiBasedPermittivityModel
import core.optics.ExternalMediumType

data class Medium(
  val type: ExternalMediumType,
  // explicit annotation for property naming:
  // Jackson marshals "nReal" as "nreal" and it can no longer be read from json
  @get:JsonProperty("epsReal")
  val epsReal: Double,
  @get:JsonProperty("epsImaginary")
  val epsImaginary: Double
) {
  /**
   * Negative refractive index values are allowed
   */
  fun toLayer() = when (type) {
    ExternalMediumType.AIR -> {
      ConstPermittivityLayer(d = Double.POSITIVE_INFINITY, eps = Complex.ONE)
    }
    ExternalMediumType.GAAS_ADACHI -> {
      GaAs(d = Double.POSITIVE_INFINITY, permittivityModel = AdachiBasedPermittivityModel.ADACHI_SIMPLE)
    }
    ExternalMediumType.GAAS_GAUSS -> {
      GaAs(d = Double.POSITIVE_INFINITY, permittivityModel = AdachiBasedPermittivityModel.ADACHI_GAUSS)
    }
    ExternalMediumType.GAN -> {
      GaN(d = Double.POSITIVE_INFINITY)
    }
    ExternalMediumType.CUSTOM -> {
      ConstPermittivityLayer(d = Double.POSITIVE_INFINITY, eps = Complex(epsReal, epsImaginary))
    }
  }
}