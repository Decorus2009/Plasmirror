package core

import core.math.Complex
import core.math.TransferMatrix
import core.optics.*
import core.optics.Polarization.P
import core.optics.Polarization.S
import core.state.OpticalParams
import core.structure.*
import core.structure.layer.ILayer
import core.structure.layer.immutable.composite.Mie
import core.structure.layer.immutable.material.ConstPermittivityLayer
import org.apache.commons.math3.complex.Complex.NaN
import statesManager
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.pow

/**
 * Mirror: left medium layer + structure + right medium layer
 */
// TODO all properties are vars? they can be changed on UI
class Mirror(
  var structure: Structure,
  var leftMediumLayer: ILayer,
  var rightMediumLayer: ILayer
) {

  fun updateVia(opticalParams: OpticalParams, textDescription: String) {
    structure = textDescription.buildStructure()
    leftMediumLayer = opticalParams.leftMedium.toLayer()
    rightMediumLayer = opticalParams.rightMedium.toLayer()
  }

  fun copyWithStructure(structure: Structure) = Mirror(structure, leftMediumLayer, rightMediumLayer)

  fun reflectance(wl: Double, pol: Polarization, angle: Double, temperature: Double) =
    r(wl, pol, angle, temperature).abs().pow(2)

  fun transmittance(wl: Double, pol: Polarization, angle: Double, temperature: Double): Double {
    val T = t(wl, pol, angle, temperature).abs().pow(2)

    val n1 = leftMediumLayer.n(wl, temperature)
    val n2 = rightMediumLayer.n(wl, temperature)

    val cos1 = cosThetaIncident(angle)
    val cos2 = cosThetaInLayer(rightMediumLayer.n(wl, temperature), wl, angle, temperature)

    return when (pol) {
      P -> ((n2 * cos1) / (n1 * cos2)).abs() * T
      S -> ((n2 * cos2) / (n1 * cos1)).abs() * T
    }
  }

  fun absorbance(wl: Double, pol: Polarization, angle: Double, temperature: Double) =
    1.0 - reflectance(wl, pol, angle, temperature) - transmittance(wl, pol, angle, temperature)

  fun refractiveIndex(wl: Double, temperature: Double) = firstLayer.n(wl, temperature) // TODO fiststLayer check

  fun permittivity(wl: Double, temperature: Double) = firstLayer.permittivity(wl, temperature) // TODO fiststLayer check

  fun extinctionCoefficient(wl: Double, temperature: Double) = firstLayer.extinctionCoefficient(wl, temperature)

  fun scatteringCoefficient(wl: Double, temperature: Double) = (firstLayer as Mie).scatteringCoefficient(wl, temperature)

  private fun r(wl: Double, pol: Polarization, angle: Double, temperature: Double) =
    matrix(wl, pol, angle, temperature).let { it[1, 0] / it[1, 1] * (-1.0) }

  private fun t(wl: Double, pol: Polarization, angle: Double, temperature: Double) =
    matrix(wl, pol, angle, temperature).let { it.det() / it[1, 1] }

  /**
   * Странный алгоритм перемножения матриц. Оно происходит не последовательно.
   * Не стал разделять этот метод на вычисление отдельных матриц для блоков, матрицы структуры и т.д.
   * Все делается здесь, как в оригинале, иначе почему-то не работает
   * (возможно, этот как-то связано с некоммутативностью перемножения матриц).
   *
   *
   * Примерный алгоритм:
   * 1. Рассматриваются все блоки последовательно
   * 2. Берется первый блок и верхний слой в нем.
   * Матрица для этого слоя умножается на матрицу интерфейса слева относительно данного слоя. Именно в таком порядке.
   * Матрица интерфейса - единичная, если слой - первый.
   * 3. Рассматривается следующий слой. Аналогично его матрица умножается на матрицу левого интерфейса.
   * 4. Результат из п.3 умножается на результат из п.2 и т.д.
   * Т.е., умножение происходит не совсем линейно.
   * Далее учитывается интерфейс с левой средой и интерфейс с правой средой.
   * Для подробностей см. код, он более-менее human-readable.
   * *
   * @return transfer matrix for mirror
   */

  // TODO refactor
  private fun matrix(wl: Double, pol: Polarization, angle: Double, temperature: Double): TransferMatrix {
    var prev = leftMediumLayer
    /* blank layer (for formal initialization) */
    var first: ILayer = ConstPermittivityLayer(d = POSITIVE_INFINITY, eps = Complex(NaN))
    /* blank layer (for formal initialization) */
    var beforeFirst: ILayer = ConstPermittivityLayer(d = POSITIVE_INFINITY, eps = Complex(NaN))

    var periodMatrix: TransferMatrix
    var tempMatrix: TransferMatrix
    var mirrorMatrix: TransferMatrix = TransferMatrix.unaryMatrix()

    var isFirst: Boolean
    for (i in 0..structure.blocks.size - 1) {

      with(structure.blocks[i]) {
        periodMatrix = TransferMatrix.unaryMatrix()

        isFirst = true
        var cur: ILayer = ConstPermittivityLayer(d = POSITIVE_INFINITY, eps = Complex(NaN))  // blank layer (for formal initialization)
        for (j in 0..layers.size - 1) {

          cur = layers[j]
          if (isFirst) {

            first = cur
            beforeFirst = prev
            isFirst = false

            tempMatrix = TransferMatrix.unaryMatrix()

          } else {
            tempMatrix = interfaceMatrix(prev, cur, wl, angle, temperature)
          }

          tempMatrix = cur.matrix(wl, pol, angle, temperature) * tempMatrix
          periodMatrix = tempMatrix * periodMatrix
          prev = cur
        }

        if (repeat > 1) {
          tempMatrix = interfaceMatrix(cur, first, wl, angle, temperature) * periodMatrix
          tempMatrix = tempMatrix.pow(repeat - 1)
          periodMatrix *= tempMatrix
        }

        periodMatrix *= interfaceMatrix(beforeFirst, first, wl, angle, temperature)
        mirrorMatrix = periodMatrix * mirrorMatrix
      }
    }
    mirrorMatrix = interfaceMatrix(prev, rightMediumLayer, wl, angle, temperature) * mirrorMatrix
    return mirrorMatrix
  }

  /**
   * @param leftLayer  layer on the left side of the interface
   * @param rightLayer layer on the right side of the interface
   * @return interface matrix
   */
  private fun interfaceMatrix(leftLayer: ILayer, rightLayer: ILayer, wl: Double, angle: Double, temperature: Double) =
    TransferMatrix().apply {
      val n1 = leftLayer.n(wl, temperature)
      val n2 = rightLayer.n(wl, temperature)

      /**
       * cos theta in left and right layers are computed using the Snell law.
       * Left and right layers are considered to be next to the left medium (AIR, CUSTOM, etc.)
       */
      val cos1 = cosThetaInLayer(leftLayer.n(wl, temperature), wl, angle, temperature)
      val cos2 = cosThetaInLayer(rightLayer.n(wl, temperature), wl, angle, temperature)

      val n1e = when (statesManager.activeState().polarization()) {
        S -> n1 * cos1
        else -> n1 / cos1
      }
      val n2e = when (statesManager.activeState().polarization()) {
        S -> n2 * cos2
        else -> n2 / cos2
      }
      setDiagonal((n2e + n1e) / (n2e * 2.0))

      /**
       * При отражении света от среды с бОльшим показателем преломления,
       * возникает эффект инверсии фазы (Pi/2) относительно фазы падающей волны.
       * [diff] - это определенный костыль, который позволяет это учесть в коде,
       * поскольку в оригинальной версии Поддубного/Большакова этого сделано не было.
       */
      val diff = if (n2e.real > n1e.real) {
        n1e - n2e
      } else {
        n2e - n1e
      }
      setAntiDiagonal(diff / (n2e * 2.0)) //      setAntiDiagonal((n2e - n1e) / (n2e * 2.0))
    }

  private val firstLayer get() = structure.blocks.first().layers.first()
}

private fun Structure.isOfSingleLayer() = blocks.size == 1 && blocks.first().layers.size == 1
