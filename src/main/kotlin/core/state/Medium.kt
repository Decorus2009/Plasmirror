package core.state

import com.fasterxml.jackson.annotation.JsonProperty
import core.Complex
import core.layers.semiconductor.ConstRefractiveIndexLayer
import core.layers.semiconductor.GaAs
import core.optics.MediumType
import core.optics.PermittivityType

data class Medium(
  val type: MediumType,
  // explicit annotation for property naming:
  // Jackson marshals "nReal" as "nreal" and it can no longer be read from json
  @get:JsonProperty("nReal")
  val nReal: Double,
  @get:JsonProperty("nImaginary")
  val nImaginary: Double
) {
  /**
   * Negative refractive index values are allowed
   */
  fun toLayer() = when (type) {
    MediumType.AIR -> {
      ConstRefractiveIndexLayer(d = Double.POSITIVE_INFINITY, n = Complex.ONE)
    }
    MediumType.GAAS_ADACHI -> {
      GaAs(d = Double.POSITIVE_INFINITY, permittivityType = PermittivityType.ADACHI_SIMPLE)
    }
    MediumType.GAAS_GAUSS -> {
      GaAs(d = Double.POSITIVE_INFINITY, permittivityType = PermittivityType.ADACHI_GAUSSIAN_BROADENING)
    }
    MediumType.CUSTOM -> {
      ConstRefractiveIndexLayer(d = Double.POSITIVE_INFINITY, n = Complex(nReal, nImaginary))
    }
  }
}