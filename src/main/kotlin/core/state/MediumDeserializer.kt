package core.state

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode

/**
 * Backward-compatible deserializer for [Medium].
 *
 * - New format: {"description": "..."} -> read directly.
 * - Legacy format: {"type": "...", "epsReal": .., "epsImaginary": ..} -> convert to an
 *   equivalent single-layer description string. The legacy mapping is inlined here; the
 *   old ExternalMediumType enum is not needed.
 */
class MediumDeserializer : JsonDeserializer<Medium>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Medium {
    val node: JsonNode = p.codec.readTree(p)

    if (node.hasNonNull("description")) {
      return Medium(node.get("description").asText())
    }

    val type = node.get("type")?.asText()?.uppercase()
    val epsReal = (node.get("epsReal") ?: node.get("nReal"))?.asDouble() ?: 1.0
    val epsImaginary = (node.get("epsImaginary") ?: node.get("nImaginary"))?.asDouble() ?: 0.0

    return Medium(legacyDescription(type, epsReal, epsImaginary))
  }

  private fun legacyDescription(type: String?, epsReal: Double, epsImaginary: Double): String = when (type) {
    "AIR" -> if (epsReal == 1.0 && epsImaginary == 0.0) "material: custom, eps: 1.0"
             else customEps(epsReal, epsImaginary)
    "GAAS_ADACHI" -> "material: GaAs, eps: adachi_simple"
    "GAAS_GAUSS" -> "material: GaAs, eps: adachi_gauss"
    "GAN" -> "material: GaN"
    else -> customEps(epsReal, epsImaginary)
  }

  private fun customEps(re: Double, im: Double) = "material: custom, eps: ($re, $im)"
}
