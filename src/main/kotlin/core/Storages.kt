package core

import core.util.*


// TODO FIX: get rid of
/**
 * Storages should be initialized before GUI elements initialization
 */
object StructureDescriptionStorage {
  private val structureDescriptionPath = "data${sep}internal${sep}state${sep}structure.txt"
  private val file = requireFile(structureDescriptionPath)
  var textDescription: String = file.readText()

  fun save() = textDescription.writeTo(file)
}

object ComputationParametersStorage {
  private val parametersPath = "data${sep}internal${sep}state${sep}parameters.json"
  private val file = requireFile(parametersPath)
  private var content = file.toJsonObject()

  fun save() = content.writeTo(file)

  var mode: String
    get() = content.getString(modeKey)
    set(value) {
      content.put(modeKey, value)
    }

  var leftMedium: String
    get() = content.getString(leftMediumKey)
    set(value) {
      content.put(leftMediumKey, value)
    }

  var rightMedium: String
    get() = content.getString(rightMediumKey)
    set(value) {
      content.put(rightMediumKey, value)
    }

  var leftMediumRefractiveIndexReal: String
    get() = content.getJSONObject(leftMediumRefractiveIndexKey).getString(realKey)
    set(value) {
      content.getJSONObject(leftMediumRefractiveIndexKey).put(realKey, value)
    }

  var leftMediumRefractiveIndexImaginary: String
    get() = content.getJSONObject(leftMediumRefractiveIndexKey).getString(imaginaryKey)
    set(value) {
      content.getJSONObject(leftMediumRefractiveIndexKey).put(imaginaryKey, value)
    }

  var rightMediumRefractiveIndexReal: String
    get() = content.getJSONObject(rightMediumRefractiveIndexKey).getString(realKey)
    set(value) {
      content.getJSONObject(rightMediumRefractiveIndexKey).put(realKey, value)
    }

  var rightMediumRefractiveIndexImaginary: String
    get() = content.getJSONObject(rightMediumRefractiveIndexKey).getString(imaginaryKey)
    set(value) {
      content.getJSONObject(rightMediumRefractiveIndexKey).put(imaginaryKey, value)
    }

  var polarization: String
    get() = content.getString(polarizationKey)
    set(value) {
      content.put(polarizationKey, value)
    }

  var angle: String
    get() = content.getString(angleKey)
    set(value) {
      content.put(angleKey, value)
    }

  var wavelengthStart: String
    get() = content.getJSONObject(computationRangeKey).getString(startKey)
    set(value) {
      content.getJSONObject(computationRangeKey).put(startKey, value)
    }

  var wavelengthEnd: String
    get() = content.getJSONObject(computationRangeKey).getString(endKey)
    set(value) {
      content.getJSONObject(computationRangeKey).put(endKey, value)
    }

  var wavelengthStep: String
    get() = content.getJSONObject(computationRangeKey).getString(stepKey)
    set(value) {
      content.getJSONObject(computationRangeKey).put(stepKey, value)
    }

  private const val modeKey = "mode"
  private const val leftMediumKey = "left_medium"
  private const val rightMediumKey = "right_medium"
  private const val leftMediumRefractiveIndexKey = "left_medium_n"
  private const val rightMediumRefractiveIndexKey = "right_medium_n"
  private const val realKey = "real"
  private const val imaginaryKey = "imaginary"
  private const val polarizationKey = "polarization"
  private const val angleKey = "angle"
  private const val computationRangeKey = "computation_range"
  private const val startKey = "start"
  private const val endKey = "end"
  private const val stepKey = "step"
}


