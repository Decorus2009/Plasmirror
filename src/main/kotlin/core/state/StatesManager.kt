package core.state

import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.HashMap


object StatesManager {
  private val states = HashMap<UUID, State>()

  init {
    initStates().forEachIndexed { index, state ->
      if (index == 0) {
        state.activate()
      }
      add(state)
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

  fun activeState() = states.values.singleOrNull { it.isActive }
    ?: throw IllegalStateException("Exactly one active state is allowed")

  private fun validate(stateId: UUID) =
    require(states.containsKey(stateId)) {
      "Only existing state with valid id can be updated, deleted, activated or deactivated"
    }
}