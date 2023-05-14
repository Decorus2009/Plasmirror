package core.structure.layer.immutable.particles

import core.structure.layer.IParticle

/**
 * NB: [r] is not used for [LayerType.SPHERES_LATTICE] layer
 * TODO think of necessity of [r]
 * */
abstract class AbstractParticle(open val r: Double?) : IParticle