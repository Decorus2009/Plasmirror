package core.structure.parser

import com.fasterxml.jackson.databind.JsonNode
import core.math.Complex
import core.optics.ExternalDispersionsContainer
import core.optics.KnownCustomModels
import core.optics.KnownModel
import core.optics.isKnownCustomModel
import core.structure.description.DescriptionParameters
import core.util.*
import core.validators.StructureDescriptionException
import core.validators.fail

// call on 'eps' node
/**
 * a layer may contain eps json node as
 * 1. a number: 'eps: 3.6'
 *
 * 2. a complex number: 'eps: (3.6, -0.1)'
 *
 * 3. a link to an external dispersion file, e.g. 'eps: GaAsRII' (file name without extension)
 *
 * 4. known custom model, e.g.
 *    eps: tanguy95_general
 *
 * 5. an expression, e.g.
 *    eps: {
 *      expr: {
 *        fun f(q)=5.1529+(92842.09/(q*q-86436))
 *        return (f(x), 0)
 *      }
 *    }
 *
 * 6. a variable parameter
 *    eps: {
 *      var: true,
 *      mean: 12.5,
 *      deviation: 0.1
 *    }
 */
fun JsonNode.permittivityType(): PermittivityType {
  val maybeEpsText = requireTextOrNull()

  if (maybeEpsText != null) {
    // cases 1, 2
    if (maybeEpsText.isRealNumber()) {
      return PermittivityType.Number(requireComplex()) // read real number as complex
    }

    // case 3
    if (maybeEpsText in ExternalDispersionsContainer.externalDispersions) {
      return PermittivityType.ExternalDispersion(maybeEpsText)
    }

    // case 4
    if (maybeEpsText.isKnownCustomModel()) {
      return PermittivityType.CustomModel(maybeEpsText)
    }

    fail("Permittivity dispersion or known custom model with name \"$maybeEpsText\" not found for custom material type")
  }
  // cases 5, 6 (eps is a json node)
  else {
    if (isComplexNumber()) {
      return PermittivityType.Number(requireComplex())
    }

    if (isVarParameter()) {
      return PermittivityType.Number(requireComplex(DescriptionParameters.mean))
    }

    val exprText = requireTextOrNull(DescriptionParameters.expr)
      ?: fail("Cannot find permittivity expression for custom material type")

    return PermittivityType.Expression(exprText)
  }
}

sealed class PermittivityType {
  class Number(val numberValue: Complex) : PermittivityType()
  class ExternalDispersion(val dispersionName: String) : PermittivityType()
  class Expression(val exprText: String) : PermittivityType()
  class CustomModel(val modelName: String): PermittivityType()
}
