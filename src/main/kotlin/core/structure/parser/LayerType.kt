package core.structure.parser

// TODO what's the point? replace with enum? 
sealed class LayerType(open val descriptor: String) {
  sealed class Material(override val descriptor: String) : LayerType(descriptor) {
    object GaAs : Material(GAAS)
    object AlGaAs : Material(ALGAAS)
    object AlGaAsSb : Material(ALGAASSB)
    object GaN : Material(GAN)
    object AlGaN : Material(ALGAN)
    object Custom : Material(CUSTOM)
  }

  sealed class Composite(override val descriptor: String) : LayerType(descriptor) {
    object Excitonic : Composite(EXCITONIC)
    object EffectiveMedium : Composite(EFF_MEDIUM)
    object SpheresLattice : Composite(SPHERES_LATTICE)
    object Mie : Composite(MIE)
  }

  /**
   * This type of material is created to represent a material definition at the root level of structure description.
   * Use-case: someone has to repeat a certain layer (with the same expression for eps) multiple times with different widths
   * It's easier to define this layer once at the beginning via [DescriptionParameters.definition] node and reuse it further
   *
   * Note that user-defined material is a bit another abstraction than [LayerType.Material.Custom] material
   * The first one is created for a single-time definition and reusage during structure description,
   * whereas the second one is intended for in-place usage only
   */
  class UserDefined(override val descriptor: String = USER_DEFINED, val name: String) : LayerType(descriptor)
}

// TODO not used
sealed class ParticlesType(open val descriptor: String) {
  object Drude : ParticlesType(DRUDE)
  object DrudeLorentz : ParticlesType(DRUDE_LORENTZ)
  object Custom : ParticlesType(CUSTOM)
  object Sb : ParticlesType(SB)
  object BiOrthogonal : ParticlesType(BI_ORTHOGONAL)
  object BiParallel : ParticlesType(BI_PARALLEL)
}

val GAAS = "GAAS"
val ALGAAS = "ALGAAS"
val ALGAASSB = "ALGAASSB"
val GAN = "GAN"
val ALGAN = "ALGAN"
val CUSTOM = "CUSTOM"
val USER_DEFINED = "USER_DEFINED"
val EXCITONIC = "EXCITONIC"
val EFF_MEDIUM = "EFF_MEDIUM"
val SPHERES_LATTICE = "SPHERES_LATTICE"
val MIE = "MIE" // TODO remove "layer: mie" from computation if activeState().mode() is Scattering or Extinction

val DRUDE = "DRUDE"
val DRUDE_LORENTZ = "DRUDE_LORENTZ"
val SB = "SB"
val BI_ORTHOGONAL = "BI_ORTHOGONAL"
val BI_PARALLEL = "BI_PARALLEL"

val predefinedMaterialNames = setOf(
  GAAS, ALGAAS, ALGAASSB, GAN, ALGAN, CUSTOM, USER_DEFINED
)

val predefinedCompositeNames = setOf(
  EXCITONIC, EFF_MEDIUM, MIE, SPHERES_LATTICE
)

val predefinedParticleTypeNames = setOf(
  DRUDE, DRUDE_LORENTZ, CUSTOM, SB, BI_ORTHOGONAL, BI_PARALLEL
)

enum class ParticleType(name: String) {
  DRUDE("DRUDE"),
  DRUDE_LORENTZ("DRUDE_LORENTZ"),
  CUSTOM("CUSTOM"),
  SB_CARDONA("SB_CARDONA"),
  SB_PALLIK("SB_PALLIK"),
  BI_CARDONA_ADACHI_ORTHOGONAL("BI_CARDONA_ADACHI_ORTHOGONAL"),
  BI_CARDONA_ADACHI_PARALLEL("BI_CARDONA_ADACHI_PARALLEL"),
//  BI_WERNER_EXPERIMENT("BI_WERNER_EXPERIMENT"),
//  BI_WERNER_DFT_CALCULATIONS("BI_WERNER_DFT_CALCULATIONS"),
}