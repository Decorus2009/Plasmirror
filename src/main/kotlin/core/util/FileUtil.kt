package core.util

import core.Complex
import core.optics.Mode
import core.state.*
import java.io.File
import java.nio.file.Paths
import java.util.*

object KnownPaths {
  val config = "data${sep}internal${sep}state${sep}config.json"
  val permittivitySbCardonaAdachi = "data${sep}internal${sep}interpolations${sep}eps_Sb_Cardona_Adachi.txt"
  val help = "data${sep}help.txt"
}

private val sep: String = File.separator

private const val indentFactor = 2

fun String.requireFile(): File = Paths.get(this).toFile().also {
  if (!it.exists()) {
    error("Missing or inaccessible file $this")
  }
}

fun String.writeTo(path: String) = writeTo(path.requireFile())

fun String.writeTo(file: File) = file.writeText(this)

// TODO get rid of?
//fun readRealDataFrom(path: String) = readFileWithTwoColumns(path)

fun String.importComplexData() = requireFile().importComplexData()

fun File.importComplexData() = ExternalData(name, readThreeColumns())

fun exportFileName() = with(activeState()) {
  StringBuilder().apply {
    val mode = computationState.opticalParams.mode
    val start = computationState.range.start
    val end = computationState.range.end

    append("computation_${mode}_${start}_${end}")
    if (mode == Mode.REFLECTANCE || mode == Mode.TRANSMITTANCE || mode == Mode.ABSORBANCE) {
      append("_${polarization()}-POL_^${String.format(Locale.US, "%04.1f", angle())}_deg")
    }
  }.toString()
}

fun writeComputedDataTo(file: File) {
  val activeState = activeState()
  val computedReal = activeState.computationState.data.yReal
  val computedImaginary = activeState.computationState.data.yImaginary

  val columnSeparator = "\t"

  val wavelengths = activeState.computationState.data.x.toList()
  StringBuilder().apply {
    computedReal.indices.forEach { idx ->
      append(String.format(Locale.US, "%.8f", wavelengths[idx]))
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

private fun readFileWithTwoColumns(file: File): Pair<List<Double>, List<Double>> {
  val tokenizedLines = file.readAndMapEachLineTo {
    safeDouble() to safeDouble()
  }
  return Pair(
    first = tokenizedLines.map { it.first },
    second = tokenizedLines.map { it.second }
  )
}

private fun File.readThreeColumns() = readAndMapEachLineTo {
  Triple(safeDouble(), safeDouble(), safeDouble())
}.let { tokenizedLines ->
  Data(
    x = tokenizedLines.map { it.first }.toMutableList(),
    yReal = tokenizedLines.map { it.second }.toMutableList(),
    yImaginary = tokenizedLines.map { it.third }.toMutableList()
  ).validate(name).normalize()
}

private fun <T> File.readAndMapEachLineTo(mapper: Scanner.() -> T) = readLines()
  .asSequence()
  .filter { line -> !line.isBlank() && line.startsWithDigit() }
  .map { it.replaceCommas() }
  .map { Scanner(it).useLocale(Locale.ENGLISH).mapper() }
  .toList()

private fun <A, B, C, D, E> Triple<A, B, C>.map(firstMapper: (A) -> D, secondMapper: (B, C) -> E) =
  Pair(
    first = firstMapper(first),
    second = secondMapper(second, third)
  )

private fun String.startsWithDigit() = first().isDigit()
private fun String.replaceCommas() = replace(',', '.')
private fun Scanner.safeDouble() = if (hasNextDouble()) nextDouble() else Double.NaN

class ImportedComplexData(val name: String, val data: Pair<List<Double>, List<Complex>>) {
  fun x() = data.first

  fun y() = data.second

  fun yReal() = y().map { it.real }

  fun yImaginary() = y().map { it.imaginary }.let { values ->
    when {
      values.all { it.isNaN() } -> emptyList()
      else -> values
    }
  }
}

fun complexList(yReal: List<Double>, yImaginary: List<Double>) = yReal.zip(yImaginary).map { Complex(it.first, it.second) }
