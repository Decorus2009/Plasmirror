package core.state

import core.math.complexList

data class ExternalData(val name: String, val data: Data) {
  fun x() = data.x

  fun y() = complexList(yReal(), yImaginary())

  fun yReal() = data.yReal

  fun yImaginary() = data.yImaginary
}