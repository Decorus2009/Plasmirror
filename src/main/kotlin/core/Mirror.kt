package core

import core.layers.metal.clusters.mie.MieLayerOfMetalClustersInAlGaAs
import core.layers.semiconductor.ConstRefractiveIndexLayer
import core.layers.semiconductor.Layer
import core.optics.*
import core.optics.Polarization.P
import core.optics.Polarization.S
import core.state.OpticalParams
import core.structure.Structure
import core.structure.toStructure
import org.apache.commons.math3.complex.Complex.NaN
import rootController
import statesManager
import kotlin.Double.Companion.POSITIVE_INFINITY

/**
 * Mirror: left medium layer + structure + right medium layer
 */

// TODO all properties are vars? the can be changed on UI
class Mirror(
  var structure: Structure,
  var leftMediumLayer: Layer,
  var rightMediumLayer: Layer
) {
  fun updateUsing(opticalParams: OpticalParams) {
    structure = rootController.mainController.structureDescriptionController.structureDescriptionCodeArea.text.toStructure()

    leftMediumLayer = opticalParams.leftMedium.toLayer()
    rightMediumLayer = opticalParams.rightMedium.toLayer()
  }

  fun reflectance(wl: Double, pol: Polarization, angle: Double) = with(r(wl, pol, angle).abs()) { this * this }

  fun transmittance(wl: Double, pol: Polarization, angle: Double): Double {
    val t = t(wl, pol, angle).abs()

    val n1 = leftMediumLayer.n(wl)
    val n2 = rightMediumLayer.n(wl)

    val cos1 = cosThetaIncident(angle)
    val cos2 = cosThetaInLayer(rightMediumLayer.n(wl), wl, angle)

    return when (pol) {
      P -> ((n2 * cos1) / (n1 * cos2)).abs() * t * t
      else -> ((n2 * cos2) / (n1 * cos1)).abs() * t * t
    }
  }

  fun absorbance(wl: Double, pol: Polarization, angle: Double) =
    1.0 - reflectance(wl, pol, angle) - transmittance(wl, pol, angle)

  fun refractiveIndex(wl: Double) = structure.firstLayer().n(wl)

  fun permittivity(wl: Double) = with(refractiveIndex(wl)) { this * this }

  fun extinctionCoefficient(wl: Double) = structure.firstLayer().extinctionCoefficient(wl)

  fun scatteringCoefficient(wl: Double) = (structure.firstLayer() as MieLayerOfMetalClustersInAlGaAs).scatteringCoefficient(wl)

  private fun r(wl: Double, pol: Polarization, angle: Double) =
    with(matrix(wl, pol, angle)) { this[1, 0] / this[1, 1] * (-1.0) }

  private fun t(wl: Double, pol: Polarization, angle: Double) =
    with(matrix(wl, pol, angle)) { det() / this[1, 1] }

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
  private fun matrix(wl: Double, pol: Polarization, angle: Double): TransferMatrix {
    var prev = leftMediumLayer
    /* blank layer (for formal initialization) */
    var first: Layer = ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = Complex(NaN))
    /* blank layer (for formal initialization) */
    var beforeFirst: Layer = ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = Complex(NaN))

    var periodMatrix: TransferMatrix
    var tempMatrix: TransferMatrix
    var mirrorMatrix: TransferMatrix = TransferMatrix.unaryMatrix()

    var isFirst: Boolean
    for (i in 0..structure.blocks.size - 1) {

      with(structure.blocks[i]) {
        periodMatrix = TransferMatrix.unaryMatrix()

        isFirst = true
        var cur: Layer = ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = Complex(NaN))  // blank layer (for formal initialization)
        for (j in 0..layers.size - 1) {

          cur = layers[j]
          if (isFirst) {

            first = cur
            beforeFirst = prev
            isFirst = false

            tempMatrix = TransferMatrix.unaryMatrix()

          } else {
            tempMatrix = interfaceMatrix(prev, cur, wl, angle)
          }

          tempMatrix = cur.matrix(wl, pol, angle) * tempMatrix
          periodMatrix = tempMatrix * periodMatrix
          prev = cur
        }

        if (repeat > 1) {
          tempMatrix = interfaceMatrix(cur, first, wl, angle) * periodMatrix
          tempMatrix = tempMatrix.pow(repeat - 1)
          periodMatrix *= tempMatrix
        }

        periodMatrix *= interfaceMatrix(beforeFirst, first, wl, angle)
        mirrorMatrix = periodMatrix * mirrorMatrix
      }
    }
    mirrorMatrix = interfaceMatrix(prev, rightMediumLayer, wl, angle) * mirrorMatrix
    return mirrorMatrix
  }

  /**
   * @param leftLayer  layer on the left side of the interface
   * @param rightLayer layer on the right side of the interface
   * @return interface matrix
   */
  private fun interfaceMatrix(leftLayer: Layer, rightLayer: Layer, wl: Double, angle: Double) = TransferMatrix().apply {
    val n1 = leftLayer.n(wl)
    val n2 = rightLayer.n(wl)

    /**
     * cos theta in left and right layers are computed using the Snell law.
     * Left and right layers are considered to be next to the left medium (AIR, CUSTOM, etc.)
     */
    val cos1 = cosThetaInLayer(leftLayer.n(wl), wl, angle)
    val cos2 = cosThetaInLayer(rightLayer.n(wl), wl, angle)

    val n1e = when (statesManager.activeState().polarization()) {
      S -> n1 * cos1
      else -> n1 / cos1
    }
    val n2e = when (statesManager.activeState().polarization()) {
      S -> n2 * cos2
      else -> n2 / cos2
    }
    setDiagonal((n2e + n1e) / (n2e * 2.0))
    setAntiDiagonal((n2e - n1e) / (n2e * 2.0))
  }

  private fun Structure.firstLayer() = blocks.first().layers.first()
}

private fun Structure.isOfSingleLayer() = blocks.size == 1 && blocks.first().layers.size == 1
