package core.structure.parser

import com.fasterxml.jackson.databind.JsonNode
import core.math.Complex
import core.optics.ExternalDispersionsContainer
import core.util.*
import core.validators.StructureDescriptionException

// call on 'eps' node
/**
 * a layer may contain eps json node as
 * 1. a number: (eps: 3.6)
 *
 * 2. a complex number: (eps: (3.6, -0.1))
 *
 * 3. a link to an external dispersion file (only file name should be used without extension), e.g. eps: GaAsRII
 *
 * 4. an expression, e.g.
 *    eps: {
 *      expr: {
 *        fun f(q)=5.1529+(92842.09/(q*q-86436))
 *        return (f(x), 0)
 *      }
 *    }
 */
fun JsonNode.permittivityType(): PermittivityType {
  val maybeEpsText = requireTextOrNull()

  if (maybeEpsText != null) {
    // cases 1, 2
    if (maybeEpsText.isNumber()) {
      return PermittivityType.Number(requireComplex())
    }

    // case 3
    if (maybeEpsText in ExternalDispersionsContainer.externalDispersions) {
      return PermittivityType.ExternalDispersion(maybeEpsText)
    }

    throw StructureDescriptionException("Permittivity dispersion \"$maybeEpsText\" not found for custom material type")
  }
  // case 4
  else {
    val exprText = requireTextOrNull(DescriptionParameters.expr)
      ?: throw StructureDescriptionException("Cannot find permittivity expression for custom material type")

    return PermittivityType.Expression(exprText)
  }
}

sealed class PermittivityType {
  class Number(val numberValue: Complex) : PermittivityType()
  class ExternalDispersion(val dispersionName: String) : PermittivityType()
  class Expression(val exprText: String) : PermittivityType()
}
