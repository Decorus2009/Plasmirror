package core.structure.layer.mutable.particles

import core.structure.layer.IParticle
import core.structure.layer.mutable.VarParameter

abstract class AbstractMutableParticle(open val r: VarParameter<Double>?) : IParticle {

  abstract fun variableParameters(): List<VarParameter<Double>>

  fun isVariable() = variableParameters().any { it.isVariable }
}