package core.state

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

internal class MediumMigrationTest {

  private fun parse(json: String): Medium = mapper.readValue(json, Medium::class.java)

  @Test
  fun `new format with description is read directly`() {
    assertThat(parse("""{"description":"material: GaN"}""").description, equalTo("material: GaN"))
  }

  @Test
  fun `legacy AIR maps to const eps 1`() {
    assertThat(parse("""{"type":"AIR","epsReal":1.0,"epsImaginary":0.0}""").description,
      equalTo("material: custom, eps: 1.0"))
  }

  @Test
  fun `legacy CUSTOM maps to complex const eps`() {
    assertThat(parse("""{"type":"CUSTOM","epsReal":13.2225,"epsImaginary":0.0}""").description,
      equalTo("material: custom, eps: (13.2225, 0.0)"))
  }

  @Test
  fun `legacy GAAS_ADACHI maps to GaAs adachi_simple`() {
    assertThat(parse("""{"type":"GAAS_ADACHI","epsReal":3.6,"epsImaginary":0.0}""").description,
      equalTo("material: GaAs, eps: adachi_simple"))
  }

  @Test
  fun `legacy GAAS_GAUSS maps to GaAs adachi_gauss`() {
    assertThat(parse("""{"type":"GAAS_GAUSS","epsReal":3.6,"epsImaginary":0.0}""").description,
      equalTo("material: GaAs, eps: adachi_gauss"))
  }

  @Test
  fun `legacy GAN maps to GaN`() {
    assertThat(parse("""{"type":"GAN","epsReal":5.0,"epsImaginary":0.0}""").description,
      equalTo("material: GaN"))
  }

  @Test
  fun `legacy AIR with non-unit eps is treated as custom`() {
    assertThat(parse("""{"type":"AIR","epsReal":2.5,"epsImaginary":0.1}""").description,
      equalTo("material: custom, eps: (2.5, 0.1)"))
  }

  @Test
  fun `serialization writes new format`() {
    assertThat(mapper.writeValueAsString(Medium("material: GaN")),
      equalTo("""{"description":"material: GaN"}"""))
  }
}
