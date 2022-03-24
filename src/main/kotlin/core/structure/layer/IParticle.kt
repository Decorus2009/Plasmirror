package core.structure.layer

import core.math.Complex

interface IParticle {

  fun permittivity(wl: Double): Complex
}