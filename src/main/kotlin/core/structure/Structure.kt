package core.structure

import core.Complex
import core.layers.metal.clusters.*
import core.layers.metal.clusters.mie.*
import core.layers.semiconductor.*
import core.optics.PermittivityType
import core.optics.PermittivityType.*

class LayerDescription(val type: String, val description: List<String>)

class BlockDescription(
  val repeat: String,
  val layerDescriptions: List<LayerDescription> = mutableListOf()
)

class StructureDescription(val blockDescriptions: List<BlockDescription> = mutableListOf())

/**
 * Block is a sequence of layers with repeat descriptor
 *
 * [repeat] number of repetitions of a given sequence of layers
 */
class Block(val repeat: Int, val layers: List<Layer>)

/**
 * Structure is a sequence of blocks
 */
class Structure(val blocks: List<Block>)

object StructureBuilder {
  fun build(structureDescription: StructureDescription) = Structure(structureDescription.blockDescriptions.map { bd ->
    Block(bd.repeat.toInt(), bd.layerDescriptions.map { ld -> ld.toLayer() })
  })

  private fun LayerDescription.toLayer(): Layer {
    val typesList = type.split("-") // type might look like 9-2-2
    val layerType = typesList.first()
    val mediumPermittivityType = permittivityType(typesList[1])

    return when (typesList.size) {
      1 -> {
        when (layerType) {
          "3" -> constRefractiveIndexLayer(description)
          "6" -> constRefractiveIndexLayerExcitonic(description)
          else -> throw IllegalStateException("Unknown const refractive index layer (must never be reached)")
        }
      }
      2 -> {
        when (layerType) {
          "1" -> GaAs(description, mediumPermittivityType)
          "2" -> AlGaAs(description, mediumPermittivityType)
          "4" -> GaAsExcitonic(description, mediumPermittivityType)
          "5" -> AlGaAsExcitonic(description, mediumPermittivityType)
          else -> throw IllegalStateException("Unknown GaAs or AlGaAs layer (must never be reached)")
        }
      }
      3 -> {
        val metalClustersType = typesList[2]
        when (layerType) {
          "7" -> {
            when (metalClustersType) {
              "1" -> effectiveMediumLayerOfDrudeMetalClustersAlGaAs(description, mediumPermittivityType)
              "2" -> effectiveMediumLayerOfSbClustersAlGaAs(description, mediumPermittivityType)
              else -> throw IllegalStateException("Unknown effective medium layer (must never be reached)")
            }
          }
          "8[1]" -> {
            when (metalClustersType) {
              "1" -> mieFirstOrderLayerOfDrudeMetalClustersAlGaAs(description, mediumPermittivityType)
              "2" -> mieFirstOrderLayerOfSbClustersAlGaAs(description, mediumPermittivityType)
              else -> throw IllegalStateException("Unknown Mie theory layer of metal clusters in AlGaAs (must never be reached)")
            }
          }
          "8[1&2]" -> {
            when (metalClustersType) {
              "1" -> mieFirstAndSecondOrderLayerOfDrudeMetalClustersAlGaAs(description, mediumPermittivityType)
              "2" -> mieFirstAndSecondOrderLayerOfSbClustersAlGaAs(description, mediumPermittivityType)
              else -> throw IllegalStateException("Unknown Mie theory layer of metal clusters in AlGaAs (must never be reached)")
            }
          }
          "8[all]" -> {
            when (metalClustersType) {
              "1" -> mieFullLayerOfDrudeMetalClustersAlGaAs(description, mediumPermittivityType)
              "2" -> mieFullLayerOfSbClustersAlGaAs(description, mediumPermittivityType)
              else -> throw IllegalStateException("Unknown Mie theory layer of metal clusters in AlGaAs (must never be reached)")
            }
          }
          "9" -> {
            when (metalClustersType) {
              "1" -> twoDimensionalLayerOfDrudeMetalClustersAlGaAs(description, mediumPermittivityType)
              "2" -> twoDimensionalLayerOfSbClustersAlGaAs(description, mediumPermittivityType)
              else -> throw IllegalStateException("Unknown two-dimensional layer of metal clusters in AlGaAs (must never be reached)"
              )
            }
          }
          else -> throw IllegalStateException("Unknown AlGaAs layer with metal clusters (must never be reached)")
        }
      }
      else -> throw IllegalStateException("Unknown layer (must never be reached)")
    }
  }

  fun List<String>.parseAt(i: Int) = this[i].toDouble()

  fun List<String>.parseComplexAt(i: Int) = this[i].toComplex()

  private fun permittivityType(permittivityTypeCode: String) = when (permittivityTypeCode) {
    "1" -> ADACHI
    "2" -> GAUSS
    "3" -> GAUSS_WITH_VARIABLE_IM_PERMITTIVITY_BELOW_E0
    else -> throw IllegalArgumentException("Unknown epsType (must never be reached)")
  }

  private fun String.toComplex(): Complex {
    val (real, imaginary) = replace(Regex("[()]"), "").split(";").map { it.toDouble() }
    return Complex(real, imaginary)
  }
}