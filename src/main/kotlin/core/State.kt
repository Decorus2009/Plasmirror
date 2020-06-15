package core

import core.layers.semiconductor.Layer
import core.optics.Polarization
import core.optics.Mode
import core.structure.Structure
import core.validators.*
import core.validators.ValidationResult.FAILURE
import core.validators.ValidationResult.SUCCESS
import rootController

object State {
  var wavelengthStart: Double = 600.0
  var wavelengthEnd: Double = 1000.0
  var wavelengthStep: Double = 1.0
  var wavelengthCurrent = wavelengthStart

  lateinit var mode: Mode

  lateinit var leftMediumLayer: Layer
  lateinit var rightMediumLayer: Layer

  lateinit var polarization: Polarization
  var angle: Double = 0.0

  lateinit var structure: Structure
  lateinit var mirror: Mirror

  var wavelength = mutableListOf<Double>()
  val reflectance = mutableListOf<Double>()
  val transmittance = mutableListOf<Double>()
  val absorbance = mutableListOf<Double>()
  val permittivity = mutableListOf<Complex_>()
  val refractiveIndex = mutableListOf<Complex_>()
  val extinctionCoefficient = mutableListOf<Double>()
  val scatteringCoefficient = mutableListOf<Double>()

  fun init(): ValidationResult {
    saveToStorages()
    return when (initState()) {
      SUCCESS -> {
        clear()
        buildMirror()
        SUCCESS
      }
      FAILURE -> FAILURE
    }
  }

  fun compute() = (0 until wavelength.size).forEach {
    wavelengthCurrent = wavelength[it]

    with(mirror) {
      when (mode) {
        Mode.REFLECTANCE -> reflectance += reflectance()
        Mode.TRANSMITTANCE -> transmittance += transmittance()
        Mode.ABSORBANCE -> absorbance += absorbance()
        Mode.PERMITTIVITY -> permittivity += permittivity()
        Mode.REFRACTIVE_INDEX -> refractiveIndex += refractiveIndex()
        Mode.EXTINCTION_COEFFICIENT -> {
          extinctionCoefficient += extinctionCoefficient()
//          wavelength.forEach { print("$it\t") }
//          println()
        }
        Mode.SCATTERING_COEFFICIENT -> scatteringCoefficient += scatteringCoefficient()
      }
    }
  }

  private fun clear() {
    fun <T> clear(vararg lists: MutableList<out T>) = lists.forEach { it.clear() }

    clear(reflectance, transmittance, absorbance, extinctionCoefficient, scatteringCoefficient)
    clear(permittivity, refractiveIndex)
  }

  private fun buildMirror() {
    mirror = Mirror(structure, leftMediumLayer, rightMediumLayer)
  }

  private fun saveToStorages() = rootController.mainController.save()
}