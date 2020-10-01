package core.state

import com.fasterxml.jackson.annotation.JsonProperty
import core.Complex
import core.layers.semiconductor.ConstRefractiveIndexLayer
import core.layers.semiconductor.GaAs
import core.optics.ExternalMediumType
import core.optics.PermittivityModel

data class Medium(
  val type: ExternalMediumType,
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
    ExternalMediumType.AIR -> {
      ConstRefractiveIndexLayer(d = Double.POSITIVE_INFINITY, n = Complex.ONE)
    }
    ExternalMediumType.GAAS_ADACHI -> {
      GaAs(d = Double.POSITIVE_INFINITY, permittivityModel = PermittivityModel.ADACHI_SIMPLE)
    }
    ExternalMediumType.GAAS_GAUSS -> {
      GaAs(d = Double.POSITIVE_INFINITY, permittivityModel = PermittivityModel.ADACHI_FULL_GAUSS)
    }
    ExternalMediumType.CUSTOM -> {
      ConstRefractiveIndexLayer(d = Double.POSITIVE_INFINITY, n = Complex(nReal, nImaginary))
    }
  }
}