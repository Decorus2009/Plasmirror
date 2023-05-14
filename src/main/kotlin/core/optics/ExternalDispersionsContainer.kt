package core.optics

import core.math.interpolateComplex
import core.state.ExternalDispersionPathDescriptor
import core.state.config
import core.state.data.ExternalData
import core.util.*
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import java.io.File

data class ExternalDispersion(
  val data: ExternalData,
  val isPermittivity: Boolean,
  val polynomialSplines: Pair<PolynomialSplineFunction, PolynomialSplineFunction?>,
  val xMin: Double,
  val xMax: Double
)

object ExternalDispersionsContainer {
  // keys are file names without extensions
  val externalDispersions = mutableMapOf<String, ExternalDispersion>()

  /**
   * [isPermittivity] is false if one imports refractive index dispersion
   */
  fun File.importExternalDispersion(isPermittivity: Boolean) {
    println("Separator: ${sep}")

    // copy a file to "internal" directory in order to be protected from the deletion of an original one
    val newPath = copy("${KnownPaths.externalDispersionsDir}$sep$name")
    val name = name.removeExtension()

    config.commonData.externalDispersions[name.toLowerCase()] = ExternalDispersionPathDescriptor(
      isPermittivity,
      newPath.toString()
    )

    interpolateExternalDispersion(name, isPermittivity)
  }

  fun File.interpolateExternalDispersion(name: String, isPermittivity: Boolean) = with(importMaybeComplexData()) {
    externalDispersions[name.toLowerCase()] = ExternalDispersion(
      this@with,
      isPermittivity,
      polynomialSplines = interpolateComplex(x(), y()),
      xMin = x().minOrNull()!!,
      xMax = x().maxOrNull()!!
    )
  }

  fun removeDispersion(name: String) {
    externalDispersions.remove(name)
    config.commonData.removeExternalDispersion(name)
  }
}


