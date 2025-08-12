import kotlin.math.*

/**
 * Обрабатывает особенности в спектральных данных в области критической точки
 *
 * @param wavelengths список длин волн (ось X)
 * @param values список значений спектра (ось Y)
 * @param criticalWavelength критическая точка E0, соответствующая ширине запрещенной зоны (в нм)
 * @param smoothingWidth ширина области сглаживания в точках для ступеньки (по умолчанию 20)
 * @param linearFitRangeNm диапазон для линейной аппроксимации справа в нм (по умолчанию 50)
 * @return обработанный список значений
 */
fun processSpectrumAtCriticalPoint(
  wavelengths: List<Double>,
  values: List<Double>,
  criticalWavelength: Double,
  smoothingWidth: Int = 20,
  linearFitRangeNm: Double = 50.0
): List<Double> {
  if (values.size < 10) return values

  // Находим индекс, ближайший к критической точке
  val criticalIndex = findClosestIndex(wavelengths, criticalWavelength)

  // Находим ближайшие точки слева и справа от критической
  val leftIndex = if (criticalIndex > 0 && wavelengths[criticalIndex] >= criticalWavelength) {
    criticalIndex - 1
  } else {
    criticalIndex
  }

  val rightIndex = if (criticalIndex < values.size - 1 && wavelengths[criticalIndex] < criticalWavelength) {
    criticalIndex + 1
  } else {
    criticalIndex
  }

  // Проверяем, что у нас есть точки с обеих сторон
  if (leftIndex == rightIndex || leftIndex < 0 || rightIndex >= values.size) {
    return values // Не можем определить тип скачка
  }

  // Просто сравниваем значения ближайших точек
  val valueLeft = values[leftIndex]
  val valueRight = values[rightIndex]

  return if (valueLeft > valueRight) {
    // Случай 1: Ступенька (значение слева больше) - применяем сглаживание
    val result = values.toMutableList()
    smoothStepTransition(result, criticalIndex, smoothingWidth)
    result
  } else {
    // Случай 2: Обратная ситуация - заменяем линейной функцией
    replaceWithLinearFunction(
      wavelengths,
      values,
      criticalWavelength,
      linearFitRangeNm
    )
  }
}

/**
 * Находит индекс элемента, ближайшего к заданному значению
 */
private fun findClosestIndex(wavelengths: List<Double>, targetWavelength: Double): Int {
  var minDiff = Double.MAX_VALUE
  var closestIndex = 0

  for (i in wavelengths.indices) {
    val diff = abs(wavelengths[i] - targetWavelength)
    if (diff < minDiff) {
      minDiff = diff
      closestIndex = i
    }
  }

  return closestIndex
}

/**
 * Сглаживает ступеньку в критической точке (случай 1)
 */
private fun smoothStepTransition(
  values: MutableList<Double>,
  criticalIndex: Int,
  smoothingWidth: Int
) {
  val halfWidth = smoothingWidth / 2
  val startIdx = maxOf(0, criticalIndex - halfWidth)
  val endIdx = minOf(values.size - 1, criticalIndex + halfWidth)

  if (endIdx - startIdx < 3) return

  val valueBefore = values[startIdx]
  val valueAfter = values[endIdx]

  for (i in startIdx + 1 until endIdx) {
    val t = (i - startIdx).toDouble() / (endIdx - startIdx)
    val sigmoid = 1.0 / (1.0 + exp(-10 * (t - 0.5)))
    values[i] = valueBefore + (valueAfter - valueBefore) * sigmoid
  }
}

/**
 * Заменяет часть спектра линейной функцией (случай 2)
 */
private fun replaceWithLinearFunction(
  wavelengths: List<Double>,
  values: List<Double>,
  criticalWavelength: Double,
  linearFitRangeNm: Double
): List<Double> {
  val result = values.toMutableList()

  // 1. Находим точки справа от критической для линейной аппроксимации
  val fitIndices = wavelengths.indices.filter {
    wavelengths[it] > criticalWavelength &&
        wavelengths[it] <= criticalWavelength + linearFitRangeNm
  }

  if (fitIndices.size < 2) {
    return result // Недостаточно точек для аппроксимации
  }

  // 2. Выполняем линейную аппроксимацию методом наименьших квадратов
  val (slope, intercept) = linearRegression(
    fitIndices.map { wavelengths[it] },
    fitIndices.map { values[it] }
  )

  // 3. Находим точку пересечения линейной функции с оригинальным спектром слева
  val intersectionIndex = findIntersectionPoint(
    wavelengths,
    values,
    slope,
    intercept,
    criticalWavelength
  )

  if (intersectionIndex == -1) {
    return result // Пересечение не найдено
  }

  // 4. Заменяем значения между точкой пересечения и критической точкой
  for (i in intersectionIndex + 1 until wavelengths.size) {
    if (wavelengths[i] >= criticalWavelength) break
    // Заменяем значения линейной функцией
    result[i] = slope * wavelengths[i] + intercept
  }

  return result
}

/**
 * Выполняет линейную регрессию методом наименьших квадратов
 * Возвращает пару (slope, intercept) для уравнения y = slope * x + intercept
 */
private fun linearRegression(x: List<Double>, y: List<Double>): Pair<Double, Double> {
  val n = x.size
  if (n < 2) throw IllegalArgumentException("Недостаточно точек для регрессии")

  val sumX = x.sum()
  val sumY = y.sum()
  val sumXY = x.zip(y).sumOf { it.first * it.second }
  val sumX2 = x.sumOf { it * it }

  val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
  val intercept = (sumY - slope * sumX) / n

  return Pair(slope, intercept)
}

/**
 * Находит точку пересечения линейной функции с оригинальным спектром
 * Ищет слева от критической точки
 */
private fun findIntersectionPoint(
  wavelengths: List<Double>,
  values: List<Double>,
  slope: Double,
  intercept: Double,
  criticalWavelength: Double
): Int {
  // Ищем пересечение, двигаясь от критической точки влево
  val criticalIndex = findClosestIndex(wavelengths, criticalWavelength)

  for (i in criticalIndex - 1 downTo 1) {
    val linearValue = slope * wavelengths[i] + intercept
    val linearValuePrev = slope * wavelengths[i - 1] + intercept

    // Проверяем смену знака разности (пересечение)
    val diff = values[i] - linearValue
    val diffPrev = values[i - 1] - linearValuePrev

    if (diff * diffPrev <= 0) {
      // Нашли пересечение между i-1 и i
      // Возвращаем индекс точки до пересечения
      return i - 1
    }
  }

  // Если пересечение не найдено, возвращаем начало массива
  return 0
}

/**
 * Альтернативная версия с более точным поиском пересечения
 */
fun processSpectrumAtCriticalPointAdvanced(
  wavelengths: List<Double>,
  values: List<Double>,
  criticalWavelength: Double,
  smoothingWidth: Int = 20,
  linearFitRangeNm: Double = 50.0
): List<Double> {
  if (values.size < 10) return values

  val criticalIndex = findClosestIndex(wavelengths, criticalWavelength)

  // Находим ближайшие точки слева и справа
  val leftIndex = if (criticalIndex > 0 && wavelengths[criticalIndex] >= criticalWavelength) {
    criticalIndex - 1
  } else {
    criticalIndex
  }

  val rightIndex = if (criticalIndex < values.size - 1 && wavelengths[criticalIndex] < criticalWavelength) {
    criticalIndex + 1
  } else {
    criticalIndex
  }

  if (leftIndex == rightIndex || leftIndex < 0 || rightIndex >= values.size) {
    return values
  }

  val valueLeft = values[leftIndex]
  val valueRight = values[rightIndex]

  return if (valueLeft > valueRight) {
    // Ступенька - сглаживаем
    val result = values.toMutableList()
    smoothStepTransition(result, criticalIndex, smoothingWidth)
    result
  } else {
    // Обратная ситуация - линейная замена с интерполяцией в точке пересечения
    replaceWithLinearFunctionInterpolated(
      wavelengths,
      values,
      criticalWavelength,
      linearFitRangeNm
    )
  }
}

/**
 * Версия с интерполяцией в точке пересечения для более гладкого перехода
 */
private fun replaceWithLinearFunctionInterpolated(
  wavelengths: List<Double>,
  values: List<Double>,
  criticalWavelength: Double,
  linearFitRangeNm: Double
): List<Double> {
  val result = values.toMutableList()

  // Находим точки для линейной аппроксимации
  val fitIndices = wavelengths.indices.filter {
    wavelengths[it] > criticalWavelength &&
        wavelengths[it] <= criticalWavelength + linearFitRangeNm
  }

  if (fitIndices.size < 2) return result

  val (slope, intercept) = linearRegression(
    fitIndices.map { wavelengths[it] },
    fitIndices.map { values[it] }
  )

  // Находим точку пересечения
  val intersectionIndex = findIntersectionPoint(
    wavelengths,
    values,
    slope,
    intercept,
    criticalWavelength
  )

  if (intersectionIndex == -1) return result

  // Заменяем значения линейной функцией
  for (i in intersectionIndex + 1 until wavelengths.size) {
    if (wavelengths[i] >= criticalWavelength) break
    result[i] = slope * wavelengths[i] + intercept
  }

  // Опционально: добавляем небольшое сглаживание в точке пересечения
  // для более плавного перехода (3 точки вокруг пересечения)
  if (intersectionIndex > 0 && intersectionIndex < result.size - 2) {
    val smoothRange = 2
    for (offset in -smoothRange..smoothRange) {
      val idx = intersectionIndex + offset
      if (idx >= 0 && idx < result.size) {
        val originalWeight = abs(offset).toDouble() / smoothRange
        val linearValue = slope * wavelengths[idx] + intercept
        result[idx] = originalWeight * values[idx] + (1 - originalWeight) * linearValue
      }
    }
  }

  return result
}