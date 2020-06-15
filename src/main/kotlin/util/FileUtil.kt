package util

import core.Complex_
import core.State
import core.optics.Mode
import org.json.JSONObject
import java.io.File
import java.nio.file.Paths
import java.util.*

val sep: String = File.separator

private const val indentFactor = 2

fun requireFile(path: String): File = Paths.get(path).toFile().also {
  if (!it.exists()) {
    error("Missing or inaccessible file $path")
  }
}

fun File.toJsonObject() = JSONObject(readText())

fun String.writeTo(file: File) = file.writeText(this)

fun JSONObject.writeTo(file: File) = toString(indentFactor).writeTo(file)

fun readRealDataFrom(path: String) = readFileWithTwoColumns(path)

fun readComplexDataFrom(path: String) = readFileWithThreeColumns(path)
  .map(
    firstMapper = { x -> x },
    secondMapper = { y1, y2 ->
      require(y1.size == y2.size)
      y1.zip(y2).map { Complex_(it.first, it.second) }
    }
  )

fun exportFileName() = StringBuilder().apply {
  append("computation_${State.mode}_${State.wavelengthStart}_${State.wavelengthEnd}")
  if (State.mode == Mode.REFLECTANCE || State.mode == Mode.TRANSMITTANCE || State.mode == Mode.ABSORBANCE) {
    append("_${State.polarization}-POL_^${String.format(Locale.US, "%04.1f", State.angle)}_deg")
  }
}.toString()

fun writeComputedDataTo(file: File) {
  fun List<Complex_>.real() = map { it.real }
  fun List<Complex_>.imaginary() = map { it.imaginary }

  val computedReal = when (State.mode) {
    Mode.REFLECTANCE -> State.reflectance
    Mode.TRANSMITTANCE -> State.transmittance
    Mode.ABSORBANCE -> State.absorbance
    Mode.PERMITTIVITY -> State.permittivity.real()
    Mode.REFRACTIVE_INDEX -> State.refractiveIndex.real()
    Mode.EXTINCTION_COEFFICIENT -> State.extinctionCoefficient
    Mode.SCATTERING_COEFFICIENT -> State.scatteringCoefficient
  }

  val computedImaginary = when (State.mode) {
    Mode.PERMITTIVITY -> State.permittivity.imaginary()
    Mode.REFRACTIVE_INDEX -> State.refractiveIndex.imaginary()
    else -> emptyList()
  }

  val columnSeparator = "\t"

  StringBuilder().apply {
    computedReal.indices.forEach { idx ->
      append(String.format(Locale.US, "%.8f", State.wavelength[idx]))
      append(columnSeparator)
      append(String.format(Locale.US, "%.32f", computedReal[idx]))

      if (computedImaginary.isNotEmpty()) {
        append(columnSeparator)
        append(String.format(Locale.US, "%.32f", computedImaginary[idx]))
      }
      append(System.lineSeparator())
    }
  }.toString().writeTo(file)
}

private fun readFileWithTwoColumns(path: String): Pair<List<Double>, List<Double>> {
  val tokenizedLines = path.readAndMapEachLineTo {
    doubleOrZero() to doubleOrZero()
  }
  return Pair(
    first = tokenizedLines.map { it.first },
    second = tokenizedLines.map { it.second }
  )
}

private fun readFileWithThreeColumns(path: String): Triple<List<Double>, List<Double>, List<Double>> {
  val tokenizedLines = path.readAndMapEachLineTo {
    Triple(doubleOrZero(), doubleOrZero(), doubleOrZero())
  }
  return Triple(
    first = tokenizedLines.map { it.first },
    second = tokenizedLines.map { it.second },
    third = tokenizedLines.map { it.third }
  )
}

private fun <T> String.readAndMapEachLineTo(mapper: Scanner.() -> T) = requireFile(this)
  .readLines()
  .asSequence()
  .filter { line -> !line.isBlankOrEmpty() && line.startsWithDigit() }
  .map { it.replaceCommas() }
  .map { Scanner(it).useLocale(Locale.ENGLISH).mapper() }
  .toList()

private fun <A, B, C, D, E> Triple<A, B, C>.map(firstMapper: (A) -> D, secondMapper: (B, C) -> E) =
  Pair(
    first = firstMapper(first),
    second = secondMapper(second, third)
  )
