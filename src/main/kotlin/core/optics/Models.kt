package core.optics

interface KnownModel

enum class AlGaAsPermittivityModel : KnownModel {
  ADACHI_SIMPLE,
  ADACHI_1989,
  ADACHI_1992,
  ADACHI_T,
  ADACHI_GAUSS,
  ADACHI_MOD_GAUSS,
  TANGUY_1995,
  TANGUY_1999,
  ADACHI_SIMPLE_TANGUY_1995,

  // hidden helper model with matr_el and infraredPermittivity taken from UI, not computed as functions (see TANGUY_1995 model)
  TANGUY_95_MANUAL
}

enum class KnownCustomModels {
  TANGUY_95_GENERAL
}

// TODO obsolete?
enum class ParticlesPermittivityModel : KnownModel {
  DRUDE,
  DRUDE_LORENTZ,
  CUSTOM,
  SB,
  BI_ORTHOGONAL,
  BI_PARALLEL
}

val knownDispersionModels: Set<KnownModel> = setOf(
  *AlGaAsPermittivityModel.values(),
  *ParticlesPermittivityModel.values()
)


fun String.isKnownModel() = toUpperCase() in knownDispersionModels.map { it.toString() }
fun String.isKnownCustomModel() = toUpperCase() in KnownCustomModels.values().map { it.toString() }
