package core.state

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import core.structure.buildMediumLayer
import core.structure.layer.ILayer
import core.validators.StateException

/**
 * A semi-infinite left/right medium described by a single-layer description string
 * (same syntax as Structure Description). Only n(wl, T) of the resulting layer is used
 * by the transfer-matrix computation; thickness is irrelevant.
 */
@JsonDeserialize(using = MediumDeserializer::class)
data class Medium(val description: String) {
  fun toLayer(): ILayer = description.buildMediumLayer()
}

/**
 * Builds a [Medium] from its description and validates it by parsing the single layer now,
 * so a bad medium reports a clear "<header> error" dialog instead of surfacing later as a
 * raw parse error (or a misleading "Structure description error").
 *
 * Pure (no UI dependency) so it can be unit-tested directly.
 *
 * @param header e.g. "Left medium" / "Right medium" — becomes the "<header> error" dialog title.
 */
fun validatedMedium(description: String, header: String): Medium {
  val medium = Medium(description)
  try {
    medium.toLayer() // validate eagerly; the layer itself is rebuilt later during computation
  } catch (e: Exception) {
    // buildMediumLayer may fail via StructureDescriptionException (our own single-layer rules)
    // or raw parse errors (JsonParseException, IllegalArgumentException) on malformed text.
    // Normalize all of them into a clear "<header> error" dialog.
    throw StateException(
      headerMessage = "$header error",
      contentMessage = e.message ?: "Invalid medium description",
      cause = e
    )
  }
  return medium
}
