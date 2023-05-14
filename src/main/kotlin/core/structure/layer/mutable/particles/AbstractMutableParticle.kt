package core.structure.layer.mutable.particles

import core.structure.layer.IParticle
import core.structure.layer.mutable.DoubleVarParameter

abstract class AbstractMutableParticle(open val r: DoubleVarParameter?) : IParticle {

  abstract fun variableParameters(): List<DoubleVarParameter>

  fun isVariable() = variableParameters().any { it.isVariable }
}