package core.state

import statesManager
import java.util.*

object StatesManager {
  val states = config.states

  init {
    requireStates().forEachIndexed { index, state ->
      add(state)
      if (index == 0) {
        activate(state)
      }
    }
  }

  fun add(state: State) {
    require(!states.containsKey(state.id)) { "State with id ${state.id} is already present in storage" }
    states[state.id] = state
  }

  fun update(state: State) {
    validate(state.id)
    TODO()
  }

  fun delete(state: State) {
    validate(state.id)
    states.remove(state.id)
  }

  fun activate(state: State) {
    validate(state.id)
    states[state.id]!!.activate()
  }

  fun deactivate(state: State) {
    validate(state.id)
    states[state.id]!!.deactivate()
  }

  fun activeState() = states.values.singleOrNull { it.active }
    ?: throw IllegalStateException("Exactly one active state is allowed")

  private fun validate(stateId: StateId) = require(states.containsKey(stateId)) {
    "Only existing state with valid id can be updated, deleted, activated or deactivated"
  }
}

fun activeState() = statesManager.activeState()

typealias StateId = UUID