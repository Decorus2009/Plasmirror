package core.state

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.Assert.*
import org.junit.Test

class ConfigParserTest {

  private val mapper = jacksonObjectMapper()

  @Test
  fun deserializeToRangeObject() {
    val rangeJson = "{\"unit\":\"NM\",\"start\":400.0,\"end\":800.0,\"step\":100.0}"

    val expected = Range(
      unit = ComputationUnit.NM,
      start = 400.0,
      end = 800.0,
      step = 100.0
    )
    val actual = mapper.readValue<Range>(rangeJson)
    assertEquals(expected, actual)
  }

  @Test
  fun serializeRangeObject() {
    val range = Range(
      unit = ComputationUnit.NM,
      start = 400.0,
      end = 800.0,
      step = 100.0
    )
    val expected = "{\"unit\":\"NM\",\"start\":400.0,\"end\":800.0,\"step\":100.0}"
    val actual = mapper.writeValueAsString(range)
    assertEquals(expected, actual)
  }
}