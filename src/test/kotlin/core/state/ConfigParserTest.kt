package core.state

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import core.optics.*
import org.junit.Assert.*
import org.junit.Test
import core.util.ConfigParserException
import core.util.mapper

class ConfigParserTest {
  private val range = Range(
    unit = ComputationUnit.NM,
    start = 400.0,
    end = 800.0,
    step = 100.0
  )
  private val data = Data(
    range = range,
    yReal = listOf(0.1, 0.2, 0.3, 0.4, 0.5).toMutableList()
  )
  private val opticalParams = OpticalParams(
    mode = Mode.REFLECTANCE,
    angle = 0.0,
    polarization = Polarization.P,
    leftMedium = Medium(
      type = MediumType.AIR,
      nReal = 1.0,
      nImaginary = 0.0
    ),
    rightMedium = Medium(
      type = MediumType.GAAS_ADACHI,
      nReal = 3.6,
      nImaginary = 0.5
    )
  )
  private val textDescription = """
    x1\ntype = 1-2, d = 2\ntype = 2-1, d = 45, k = 0.0, x = 0.6\n\nx18\ntype = 9-2-2, d = 40, k = 0., x = 0.6, lattice_factor = 8.1\ntype = 2-3, d = 92., k = 0.01, x = 0.6
  """.trimIndent()

  @Test
  fun `range (deserialize to range object)`() {
    val rangeJson = computationStateNode().get("range")
    val actual = rangeJson.parse<Range>()
    assertEquals(range, actual)
  }

  @Test
  fun `range (deserialize null range node)`() {
    val nullRangeNode = (computationStateNode() as ObjectNode).set<JsonNode>("range", null)
    try {
      nullRangeNode.parse<Range>()
    } catch (e: Exception) {
      assertTrue(e is ConfigParserException)
    }
  }

  @Test
  fun `range (unknown range unit)`() {
    val unknownUnitRangeNode = (computationStateNode() as ObjectNode).set<ObjectNode>("unit", TextNode("M"))
    try {
      unknownUnitRangeNode.parse<Range>()
    } catch (e: Exception) {
      assertTrue(e is ConfigParserException)
    }
  }

  @Test
  fun `data (deserialize to data object with yReal only)`() {
    val dataJson = computationStateNode().get("data")
    val actual = dataJson.parse<Data>()
    assertEquals(data, actual)
  }

  @Test
  fun `opticalParams (deserialize to opticalParams object)`() {
    val opticalParamsJson = computationStateNode().get("opticalParams")
    val actual = opticalParamsJson.parse<OpticalParams>()
    assertEquals(opticalParams, actual)
  }

  @Test
  fun `opticalParams (unknown medium type unit)`() {
    val opticalParamsJson = (computationStateNode().get("opticalParams").get("leftMedium") as ObjectNode).set<ObjectNode>("type", TextNode("UNKNOWN"))
    try {
      opticalParamsJson.parse<OpticalParams>()
    } catch (e: Exception) {
      assertTrue(e is ConfigParserException)
    }
  }

  @Test
  fun `layers description`() {
    val textDescriptionJson = computationStateNode().get("textDescription")
    val actual = textDescriptionJson.parse<String>()
    assertEquals(textDescription, actual)
  }


  private fun computationStateNode() = mapper.readTree(config).get("states")[0].get("computationState")

  private val config = """
  {
    "states": [
      {
        "id": "main",
        "computationState": {
          "data": {
            "range": {
              "unit": "NM",
              "start": 400.0,
              "end": 800.0,
              "step": 100.0
            },
            "yReal": [
              0.1,
              0.2,
              0.3,
              0.4,
              0.5
            ],
            "yImaginary": null
          },
          "opticalParams": {
            "mode": "REFLECTANCE",
            "angle": 0.0,
            "polarization": "P",
            "leftMedium": {
              "type": "AIR",
              "nReal": 1.0,
              "nImaginary": 0.0
            },
            "rightMedium": {
              "type": "GAAS_ADACHI",
              "nReal": 3.6,
              "nImaginary": 0.0
            }
          },
          "textDescription": "x1\\ntype = 1-2, d = 2\\ntype = 2-1, d = 45, k = 0.0, x = 0.6\\n\\nx18\\ntype = 9-2-2, d = 40, k = 0., x = 0.6, lattice_factor = 8.1\\ntype = 2-3, d = 92., k = 0.01, x = 0.6"
        },
        "externalDataState": null
      },
      {}
    ]
  }
  """.trimIndent()
}