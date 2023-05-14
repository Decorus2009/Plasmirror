package core.util

import core.optics.Mode
import core.state.State
import core.state.activeState
import core.state.data.Data
import core.state.data.ExternalData
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.*
import java.util.*


object KnownPaths {
  private val internalDir = "data${sep}internal"

  val config = "$internalDir${sep}state${sep}config.json"
  val permittivitySbCardonaAdachi = "$internalDir${sep}interpolations${sep}eps_Sb_Cardona_Adachi.txt"
  val permittivitySbPallik = "$internalDir${sep}interpolations${sep}eps_Sb_Pallik.txt"
  val permittivitySbAbInitioC05 = "$internalDir${sep}interpolations${sep}eps_Sb_Pallik.txt"
  val permittivityCardonaAdachiBiOrthogonalAdachi = "$internalDir${sep}interpolations${sep}eps_Bi_E_orthogonal_c_axis_Adachi.txt"
  val permittivityCardonaAdachiBiParallelAdachi = "$internalDir${sep}interpolations${sep}eps_Bi_E_parallel_c_axis_Adachi.txt"
  val permittivityBiWernerExperiment = "$internalDir${sep}interpolations${sep}eps_Bi_Werner_2009_experiment_refractive_index_info.txt"
  val permittivityBiWernerDFTCalculations = "$internalDir${sep}interpolations${sep}eps_Bi_Werner_2009_DFT_calculations_refractive_index_info.txt"

  val help = "data${sep}help.txt"

  val importDir = "data${sep}for_import"
  val exportDir = "data${sep}for_export"
  val externalDispersionsDir = "$internalDir${sep}external_dispersions"
  val randomizationsExportDir = "data${sep}for_export${sep}randomizations"
}

val sep: String
  get() {
    val os = System.getProperty("os.name")
    if ("Windows 10" == os) {
      return File.separator + File.separator
    }
    return File.separator
  }

fun String.requireFile() = File(this).also {
  if (!it.exists()) {
    error("Missing or inaccessible file $this")
  }
}

fun String.writeTo(path: String) = writeTo(path.requireFile())

fun String.writeTo(file: File) = file.writeText(this)

fun String.importMaybeComplexData() = requireFile().importMaybeComplexData()

fun File.importMaybeComplexData() = ExternalData(name, readTwoOrThreeColumns())

// TODO activeState
fun State.writeComputedDataTo(file: File) {
  val computedReal = computationData().yReal
  val computedImaginary = computationData().yImaginary

  val columnSeparator = "\t"

  val wavelengths = computationData().x.toList()
  StringBuilder().apply {
    computedReal.indices.forEach { index ->
      append(String.format(Locale.US, "%.8f", wavelengths[index]))
      append(columnSeparator)
      append(String.format(Locale.US, "%.32f", computedReal[index]))

      if (computedImaginary.isNotEmpty()) {
        append(columnSeparator)
        append(String.format(Locale.US, "%.32f", computedImaginary[index]))
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

private fun File.readTwoOrThreeColumns() = readAndMapEachLineTo {
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
  .filter { line -> line.isNotBlank() && line.startsWithDigit() }
  .map { it.replaceCommas() }
  .map { Scanner(it).useLocale(Locale.ROOT).mapper() }
  .toList()
  .also {
    if (it.isEmpty()) {
      throw IllegalStateException("Empty file")
    }
  }

private fun <A, B, C, D, E> Triple<A, B, C>.map(firstMapper: (A) -> D, secondMapper: (B, C) -> E) =
  Pair(
    first = firstMapper(first),
    second = secondMapper(second, third)
  )

private fun String.startsWithDigit() = first().isDigit()
private fun String.replaceCommas() = replace(',', '.')
private fun Scanner.safeDouble() = if (hasNextDouble()) nextDouble() else Double.NaN

fun String.normalized(): String {
  val realSuffixPosition = indexOf(" Real")
  val imaginarySuffixPosition = indexOf(" Imaginary")
  return when {
    realSuffixPosition != -1 -> substring(0, realSuffixPosition)
    imaginarySuffixPosition != -1 -> substring(0, imaginarySuffixPosition)
    else -> this
  }
}

// TODO activeState
fun exportFileName() = with(activeState()) {
  StringBuilder().apply {
    val mode = computationState.opticalParams.mode
    val start = computationState.range.start
    val end = computationState.range.end

    append("computation_${mode}_${start}_${end}")
    if (mode == Mode.REFLECTANCE || mode == Mode.TRANSMITTANCE || mode == Mode.ABSORBANCE) {
      append("_${polarization()}-POL")
      append("_${String.format(Locale.US, "%04.1f", angle())}deg")
    }
    append("_${String.format(Locale.US, "%04.1f", temperature())}K")
  }.toString()
}

fun importPath() = safePath(KnownPaths.importDir)

fun exportPath() = safePath(KnownPaths.exportDir)

fun randomizationsExportPath() = safePath(KnownPaths.randomizationsExportDir)

private fun safePath(path: String) = if (Files.isDirectory(Paths.get(path))) {
  path
} else {
  // use current directory as a fallback if path directory is not found in a filesystem
  Paths.get(".").toAbsolutePath().toString()
}

fun File.copy(newPath: String): Path? {
  val copied = File(newPath)
  FileUtils.copyFile(this, copied)

  return copied.toPath()

  // fails with NoSuchFileException using File.copy on Windows
//  return Files.copy(toPath(), Paths.get(newPath), StandardCopyOption.REPLACE_EXISTING)
}

/**
 * Replaces file if one already exists
 */
fun String.removeExtension() = substring(0, indexOfLast { it == '.' })