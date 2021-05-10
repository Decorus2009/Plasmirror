package core.structure

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import core.layer.*
import core.layer.materials.*
import core.layer.materials.composite.*
import core.layer.materials.excitonic.Exciton
import core.layer.materials.excitonic.Excitonic
import core.layer.materials.particle.*
import core.optics.PermittivityModel
import core.optics.particles.LorentzOscillator
import core.state.mapper
import core.structure.util.*
import core.util.*
import core.validators.fail

private val userDefinitions = mutableMapOf<String, JsonNode>()

data class Structure(val blocks: List<Block>)

/**
 * Block is a sequence of layers with repeat descriptor
 * [repeat] number of repetitions of a given sequence of layers
 */
data class Block(
  val repeat: Int,
  val layers: List<Layer>
)

fun String.buildStructure() = json().asArray()
  .extractDefinitions()
  .buildStructure()

/**
 * Builds structure of a list of blocks or of a single block with no repeat descriptors specified.
 * The active state is currently being built and not available yet,
 * so there's no way to check the mode and validate the necessity of presence of repeat descriptors
 *
 * [adjacentPositionsOfRepeatDescriptors] serve as bounds
 * when slicing into chunks to be converted to block descriptions
 */
fun List<JsonNode>.buildStructure(): Structure {
  val nodes = when {
    // no repeat descriptor is found, insert an artificial node before the node with a single layer description
    !first().isRepeatDescriptor() -> listOf(repeatDescriptorNode()) + this
    else -> this
  }
  val blocks = nodes.adjacentPositionsOfRepeatDescriptors()
    .map { (position, nextPosition) ->
      nodes.slice(position until nextPosition).toBlock()
    }
    // exclude blocks with 0 repeats (e.g. a user in structure description prints x0 to exclude a block from computation
    .filterNot { it.repeat == 0 }

  return Structure(blocks)
}

private fun List<JsonNode>.toBlock() = Block(
  repeat = first().requireNonNegativeInt(DescriptionParameters.repeat),
  layers = subList(1, size).map { it.toLayer() }
)

/**
 * See possible layer configurations in the big comment block below or in the help
 *
 * In case if [d] param is visually missing (e.g. in medium: { ... } node), it's actually added artificially before
 * [StructureInitializer.json]
 * */
private fun JsonNode.toLayer(): Layer {
  val layerType = requireLayerType()
  val d = requireNonNegativeDouble(DescriptionParameters.d)

  return when (layerType) {
    is LayerType.Material.GaAs -> GaAs(
      d = d,
      dampingFactor = requireDouble(DescriptionParameters.dampingFactor),
      permittivityModel = requirePermittivityModelFor(layerType)
    )
    is LayerType.Material.AlGaAs -> AlGaAs(
      d = d,
      dampingFactor = requireDouble(DescriptionParameters.dampingFactor),
      cAl = requireNonNegativeDouble(DescriptionParameters.cAl),
      permittivityModel = requirePermittivityModelFor(layerType)
    )
    is LayerType.Material.AlGaAsSb -> AlGaAsSb(
      d = d,
      cAl = requireNonNegativeDouble(DescriptionParameters.cAl),
      cAs = requireNonNegativeDouble(DescriptionParameters.cAs)
    )
    is LayerType.Material.GaN -> GaN(
      d = d
    )
    is LayerType.Material.AlGaN -> AlGaN(
      d = d,
      cAl = requireNonNegativeDouble(DescriptionParameters.cAl)
    )
    is LayerType.Material.Custom -> processCustomLayer(d)
    is LayerType.UserDefined -> processUserDefinedLayer()
    is LayerType.Composite.Excitonic -> Excitonic(
      d = d,
      medium = requireNode(DescriptionParameters.medium).toLayer(),
      exciton = requireExciton()
    )
    is LayerType.Composite.EffectiveMedium -> EffectiveMedium(
      d = d,
      medium = requireNode(DescriptionParameters.medium).toLayer(),
      particle = requireParticlesFor(layerType),
      f = requireNonNegativeDouble(DescriptionParameters.f)
    )
    is LayerType.Composite.SpheresLattice -> SpheresLattice(
      d = d,
      medium = requireNode(DescriptionParameters.medium).toLayer(),
      particle = requireParticlesFor(layerType),
      latticeFactor = requireNonNegativeDouble(DescriptionParameters.latticeFactor)
    )
    is LayerType.Composite.Mie -> Mie(
      d = d,
      medium = requireNode(DescriptionParameters.medium).toLayer(),
      particle = requireParticlesFor(layerType),
      f = requireNonNegativeDouble(DescriptionParameters.f),
      orders = requireOrders()
    )
  }
}

/**
 * @return pairs of adjacent positions of repeat descriptors. The last position is coupled with a size of [this]
 * e.g. [0, 2, 5] -> [(0, 2), (2, 5), (5, 8)], 8 is the size of tokenized lines list
 */
private fun List<JsonNode>.adjacentPositionsOfRepeatDescriptors() = with(repeatDescriptorPositions()) {
  mapIndexed { index: Int, position: Int ->
    val nextPosition = when (position) {
      last() -> this@adjacentPositionsOfRepeatDescriptors.size
      else -> this@with.elementAt(index + 1)
    }
    position to nextPosition
  }
}

/**
 * Finds, processes and filters out all definition nodes from resulting list
 */
private fun List<JsonNode>.extractDefinitions(): List<JsonNode> {
  userDefinitions.clear()

  return filter {
    val definitionNode = it.requireNodeOrNull(DescriptionParameters.definition)

    if (definitionNode != null) {
      userDefinitions[definitionNode.requireTextUpperCase(DescriptionParameters.name)] = definitionNode
    }

    definitionNode == null
  }
}

/**
 * @return positions of repeat descriptors
 * e.g.
 *
 * 0: x10           <-- repeat descriptor
 * 1: type = x, ...
 *
 * 2: x24           <-- repeat descriptor
 * 3: type = x, ...
 * 4: type = y, ...
 *
 * 5: x100          <-- repeat descriptor
 * 6: type = y, ...
 * 7: type = z, ...
 *
 * returns [0, 2, 5]
 */
private fun List<JsonNode>.repeatDescriptorPositions() =
  mapIndexed { idx, node -> if (node.isRepeatDescriptor()) idx else -1 }.filterNot { it == -1 }

private fun repeatDescriptorNode() = mapper.readTree("""{"repeat":"1"}""")

fun JsonNode.isRepeatDescriptor() = size() == 1 && has("repeat")


/**
 * Layer might be represented via
 * 1. predefined material (such as GaAs, or custom)
 * 2. user-defined material (which is different from custom material: user-defined material
 */
private fun JsonNode.requireLayerType(): LayerType {
  val maybeMaterial = requireTextOrNullUpperCase(DescriptionParameters.material)

  when {
    maybeMaterial != null && maybeMaterial in userDefinitions.keys -> {
      return LayerType.UserDefined(name = maybeMaterial)
    }
    maybeMaterial != null && maybeMaterial in predefinedMaterialNames -> {
      return when (maybeMaterial) {
        GAAS -> LayerType.Material.GaAs
        ALGAAS -> LayerType.Material.AlGaAs
        ALGAASSB -> LayerType.Material.AlGaAsSb
        GAN -> LayerType.Material.GaN
        ALGAN -> LayerType.Material.AlGaN
        CUSTOM -> LayerType.Material.Custom
        else -> fail("Unknown material parameter \"$maybeMaterial\"")
      }
    }
  }

  val maybeType = requireTextOrNullUpperCase(DescriptionParameters.type)

  when {
    maybeType != null && maybeType in userDefinitions.keys -> {
      return LayerType.UserDefined(name = maybeType)
    }
    maybeType != null && maybeType in predefinedCompositeNames -> {
      return when (maybeType) {
        EXCITONIC -> LayerType.Composite.Excitonic
        EFF_MEDIUM -> LayerType.Composite.EffectiveMedium
        MIE -> LayerType.Composite.Mie
        SPHERES_LATTICE -> LayerType.Composite.SpheresLattice
        else -> fail("Unknown \"type\" parameter \"$maybeType\"")
      }
    }
  }

  fail("Missing or unknown \"material\" or \"type\" parameter. Check syntax or definitions if any")
}

private fun JsonNode.requirePermittivityModelFor(layerType: LayerType): PermittivityModel {
  val maybeModelName = requireTextUpperCase(DescriptionParameters.eps)
  val permittivityModelNames = PermittivityModel.values().map { it.name }
  val isLayerTypeAllowed = layerType is LayerType.Material.GaAs || layerType is LayerType.Material.AlGaAs

  check(maybeModelName in permittivityModelNames && isLayerTypeAllowed) {
    "Unknown permittivity model \"$maybeModelName\""
  }

  return PermittivityModel.valueOf(maybeModelName)
}

private fun JsonNode.requireParticlesPermittivityModel(): ParticlesPermittivityModel {
  val maybeModel = requireTextOrNullUpperCase(DescriptionParameters.material)
    ?: fail("Particles type should to be described via \"material\" keyword")

  check(maybeModel in ParticlesPermittivityModel.values().map { it.name }) {
    "Unknown particles permittivity model \"$this\""
  }

  return ParticlesPermittivityModel.valueOf(maybeModel)
}

private fun JsonNode.requireParticlesFor(layerType: LayerType) = requireNode(DescriptionParameters.particles).run {
  val r = when (layerType) {
    is LayerType.Composite.Mie -> requireNonNegativeDouble(DescriptionParameters.r)
    // "r" parameter can be provided only for Mie layer type
    else -> requirePositiveDoubleOrNull(DescriptionParameters.r)?.let {
      fail("Particle radius should be provided only for Mie layer type")
    }
  }
  when (requireParticlesPermittivityModel()) {
    ParticlesPermittivityModel.DRUDE -> DrudeParticle(
      r = r,
      wPl = requireNonNegativeDouble(DescriptionParameters.w),
      g = requireDouble(DescriptionParameters.g),
      epsInf = requireDouble(DescriptionParameters.epsInf)
    )
    ParticlesPermittivityModel.DRUDE_LORENTZ -> DrudeLorentzParticle(
      // Drude params
      r = r,
      wPl = requireNonNegativeDouble(DescriptionParameters.w),
      g = requireDouble(DescriptionParameters.g),
      epsInf = requireDouble(DescriptionParameters.epsInf),
      oscillators = requireOscillators()
    )
    ParticlesPermittivityModel.CUSTOM -> {
      when (val maybeExpr = maybePermittivityExpression()) {
        null -> ConstPermittivityParticle(
          eps = requireComplex(DescriptionParameters.eps)
        )
        else -> ExpressionBasedPermittivityParticle(
          epsExpr = maybeExpr
        )
      }
    }
    ParticlesPermittivityModel.SB -> SbParticle(r)
  }
}

private fun JsonNode.requireOrders(): Orders {
  val maybeNumericOrders = requirePositiveIntOrNull(DescriptionParameters.orders)
  val failMessage = "Available orders for Mie layer type: \"1\", \"2\" or \"all\""

  if (maybeNumericOrders != null) {
    return when (maybeNumericOrders) {
      1 -> Orders.ONE
      2 -> Orders.TWO
      else -> fail(failMessage)
    }
  }

  return requireText(DescriptionParameters.orders).let {
    when (it) {
      DescriptionParameters.all -> Orders.ALL
      else -> fail(failMessage)
    }
  }
}

private fun JsonNode.requireExciton() = requireNode(DescriptionParameters.exciton).run {
  Exciton(
    w0 = requireNonNegativeDouble(DescriptionParameters.w0),
    G0 = requireDouble(DescriptionParameters.g0),
    G = requireDouble(DescriptionParameters.g),
    wb = requireDouble(DescriptionParameters.wb),
    Gb = requireDouble(DescriptionParameters.gb),
    B = requireDouble(DescriptionParameters.b),
    C = requireComplex(DescriptionParameters.c),
  )
}

private fun JsonNode.maybePermittivityExpression() = this
  .requireNode(DescriptionParameters.eps)
  .requireTextOrNull(DescriptionParameters.expr)

private fun JsonNode.processCustomLayer(d: Double): Layer {
  // CUSTOM layer type may contain eps as a number (e.g. eps: 3.6 or eps: (3.6, -0.1)) or as an expression
  return when (val maybeExpr = maybePermittivityExpression()) {
    null -> ConstPermittivityLayer(
      d = d,
      eps = requireComplex(DescriptionParameters.eps)
    )
    else -> ExpressionBasedPermittivityLayer(
      d = d,
      epsExpr = maybeExpr
    )
  }
}

private fun JsonNode.processUserDefinedLayer(): Layer {
  val maybeMaterial = requireTextOrNullUpperCase(DescriptionParameters.material)
  val maybeType = requireTextOrNullUpperCase(DescriptionParameters.type)
  val key = maybeMaterial ?: maybeType ?: fail("Material or type should be specified for a layer")

  val definitionNode = userDefinitions[key] ?: fail("Unknown material or type definition: $key")

  /*
  put definition node into the current one,
  values of fields in this node become overridden by those in definition node.
  This is is convenient for the replacements similar to:

  material: custom_GaN ->

  material: custom,
  eps: {
    fun f(q)=5.1529+(92842.09/(q*q-86436))
    return (f(x), 0)
  }

  Here "material" field value "custom_GaN" is replaced with "custom"
  so that the further recursive call of [toLayer()] was successful
  */
  (this as ObjectNode).setAll<ObjectNode>((definitionNode as ObjectNode))

  return toLayer()
}

/**
 * Reads a part of user-provided structure description:
 * oscillators: {
 *   1: { w: 1, f: 2, g: 3 },
 *   2: { w: 4, f: 5, g: 6 }
 * }
 *
 * converted to json:
 * "oscillators": {
 *   "1": {"w": "1", "f": "2", "g": "3"},
 *   "2": {"w": "4", "f": "5", "g": "6"}
 * }
 *
 * Iterates through json-map entries, sorts them by keys and returns a list of [LorentzOscillator] objects.
 * So, it's a user's responsibility to specify meaningful successive orders starting with 1.
 * In [core.optics.particles.DrudeLorentzModel] only list of oscillators is used
 * without any information about user-provided orders
 */
private fun JsonNode.requireOscillators() = requireNode(DescriptionParameters.oscillators)
  .fields()
  .asSequence()
  .map { oscillator: MutableMap.MutableEntry<String, JsonNode> ->
    val order = oscillator.key
    val params = oscillator.value
    order to LorentzOscillator(
      f_i = params.requireDouble(DescriptionParameters.f),
      g_i = params.requireDouble(DescriptionParameters.g),
      w_i = params.requireDouble(DescriptionParameters.w)
    )
  }
  .sortedBy { it.first }
  .map { it.second }
  .toList()

private sealed class LayerType(open val descriptor: String) {
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

private val GAAS = "GAAS"
private val ALGAAS = "ALGAAS"
private val ALGAASSB = "ALGAASSB"
private val GAN = "GAN"
private val ALGAN = "ALGAN"
private val CUSTOM = "CUSTOM"
private val USER_DEFINED = "USER_DEFINED"

private val EXCITONIC = "EXCITONIC"
private val EFF_MEDIUM = "EFF_MEDIUM"
private val SPHERES_LATTICE = "SPHERES_LATTICE"
private val MIE = "MIE" // TODO remove "layer: mie" from computation if activeState().mode() is Scattering or Extinction

private val predefinedMaterialNames = setOf(
  GAAS, ALGAAS, ALGAASSB, GAN, ALGAN, CUSTOM, USER_DEFINED
)

private val predefinedCompositeNames = setOf(
  EXCITONIC, EFF_MEDIUM, MIE, SPHERES_LATTICE
)


// TODO maybe irrelevant (see help)
/**
 * Possible layer descriptions according to `when` switch in the code above
 * (not all the layers might have been included):
 *
 * layer: GaAs, n: Adachi_simple, d: 5;
 *
 *
 * layer: AlGaAs, n: Adachi_simple, d: 5, df: 0.0, cAl: 0.3;
 *
 *
 * layer: AlGaAsSb, d: 5, cAl: 0.3, cAs: 0.02;
 *
 *
 * layer: const_n, n: 3.6, d: 5;
 * layer: const_n, n: (3.6, 0.1), d: 5;
 *
 *
 * layer: excitonic,
 * medium: { material: GaAs, n: adachi_simple, df: 0.0 },
 * exciton: { w0: 1.5, G0: 0.0005, G: 0.1 },
 * d: 12;
 *
 *
 * layer: eff_medium,
 * medium: { material: AlGaAs, n: Adachi_simple, df: 0.0, cAl: 0.3 },
 * particles: { n: Drude, w: 14.6, G: 0.5, epsInf: 1.0 },
 *
 * // or
 *
 * particles: { n: Drude-Lorentz, w: 14.6, G: 0.5, epsInf: 1.0,
 *   oscillators: {
 *     1: { w: 1, f: 2, g: 3 },
 *     2: { w: 4, f: 5, g: 6 }
 *   }
 * },
 * d: 1000, f: 0.01;
 *
 * layer: eff_medium,
 * medium: { layer: AlGaAsSb, n: Adachi_T, cAl: 0.3, cSb: 0.01 },
 * particles: { n: Drude, w: 14.6, G: 0.5, epsInf: 1.0 },
 * d: 1000, f: 0.01;
 *
 *
 * layer: mie,
 * orders: 1,
 * f = 0.01,
 * medium: { material: AlGaAs, n: Adachi_simple, df: 0.0, cAl: 0.3 },
 * particles: { n: Drude, r: 10.6, w: 14.6, G: 0.5, epsInf: 1.0 };
 *
 *
 * layer: spheres_lattice,
 * medium: { material: AlGaAs, n: Adachi_simple, df: 0.0, cAl: 0.3 },
 * particles: { n: Drude, w: 14.6, G: 0.5, epsInf: 1.0 },
 * d: 40, lattice_factor: 8.1;
 *
 *
 *
 * material: custom, d: 90, n: {
 *   val w0 = 1.5
 *   val gamma = 0.1
 *   return gamma / ((x - w0)^2 + gamma^2)
 * };
 *
material: custom, d: 90, n: {
fun f(x) = sin(x)
val offset = 1.0
return f(x) * offset
};


FAILS
x1
layer: excitonic,
medium: { material: GaAs, n: adachi_simple, df: 0.0 },
exciton: { w0: 1.7, G0: 0.0005, G: 0.01 },
d: 10;
material: custom, d: 90, n: {
fun f(x) = sin(x * 0.1) * 0.5
fun g(x) = cos(x * 0.1) * 0.5
return (f(x), g(x))
};

material: custom, d: 90, n: {
fun f(x) = sin(x * 0.1) * 0.5
fun g(x) = cos(x * 0.1) * 0.5
return (f(x), g(x))
};
 */


