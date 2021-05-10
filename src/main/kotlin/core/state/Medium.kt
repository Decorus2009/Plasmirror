package core.state

import com.fasterxml.jackson.annotation.JsonProperty
import core.layer.materials.*
import core.math.Complex
import core.optics.ExternalMediumType
import core.optics.PermittivityModel

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
      GaAs(d = Double.POSITIVE_INFINITY, permittivityModel = PermittivityModel.ADACHI_SIMPLE)
    }
    ExternalMediumType.GAAS_GAUSS -> {
      GaAs(d = Double.POSITIVE_INFINITY, permittivityModel = PermittivityModel.ADACHI_GAUSS)
    }
    ExternalMediumType.GAN -> {
      GaN(d = Double.POSITIVE_INFINITY)
    }
    ExternalMediumType.CUSTOM -> {
      ConstPermittivityLayer(d = Double.POSITIVE_INFINITY, eps = Complex(epsReal, epsImaginary))
    }
  }
}