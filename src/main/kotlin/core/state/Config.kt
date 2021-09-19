package core.state

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import core.optics.ExternalDispersionsContainer.interpolateExternalDispersion
import core.util.*
import java.nio.file.InvalidPathException
import java.nio.file.Paths

val mapper = jacksonObjectMapper()//.also { it.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true) }

val config = Config()

/**
 * Initially config contains empty states list which is filled during config.json parsing stage
 */
data class Config(
  val states: MutableMap<StateId, State> = mutableMapOf(),
  val commonData: CommonData = readCommonData()
)

data class CommonData(
  // dispersion name -> dispersion descriptor
  @get:JsonProperty("externalDispersions")
  val externalDispersions: MutableMap<String, ExternalDispersionPathDescriptor> = mutableMapOf()
) {
  fun removeExternalDispersion(name: String) {
    val descriptor = externalDispersions[name] ?: return

    try {
      // trying to take a file from internal/...  and remove it as well
      Paths.get(descriptor.path).toFile().delete()
    } catch (ex: Exception) {
    }
    externalDispersions.remove(name)
  }
}

data class ExternalDispersionPathDescriptor(
  val isPermittivity: Boolean,
  val path: String
)

/**
 * Saves all the states to config only when there was a successful computation after "Compute" button click.
 * Successful computation means that all the computation parameters are correct, especially structure description
 * which is regularly edited by a user.
 *
 * To avoid config inconsistency states are saved iff [lastValidState] isn't null.
 * This variable is set in "Compute" button callback only after a successful computation.
 */
fun saveConfig() {
  // cannot save config before the initialization
  require(config.states.isNotEmpty())

  mapper
    .writeValueAsString(config)
    .writeTo(KnownPaths.config)
}

fun requireStatesNodes() = configNode()
  .requireNode("states")
  .also {
    require(it.size() > 0)
  }
  .map { it }


private fun readCommonData() = configNode()
  .requireNode("commonData")
  .parse<CommonData>()
  .also { data ->
    // a list to contain potentially missing dispersion files in folder to remove them from config later
    val missingDispersions = mutableSetOf<String>()

    data.externalDispersions.forEach { (name, descriptor) ->
      try {
        // taking a file from internal/...
        val dispersionFile = Paths.get(descriptor.path).toFile()
        dispersionFile.interpolateExternalDispersion(name, descriptor.isPermittivity)
      } catch (ex: Exception) {
        // file might be not found,
        // but we'll check the presence of dispersion by its name at structure description parsing stage
        missingDispersions += when (ex) {
          is java.io.FileNotFoundException -> name
          is InvalidPathException -> name
          else -> throw ex
        }
      }
    }

    data.externalDispersions.entries.removeIf { (name, _) -> name in missingDispersions }
  }

private fun configNode() = mapper.readTree(KnownPaths.config.requireFile())

// medium: { material: custom, eps: GaAsRII },