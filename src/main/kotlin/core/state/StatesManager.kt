package core.state

import core.util.KnownPaths
import core.util.writeTo
import statesManager
import java.util.*
import kotlin.collections.HashMap

object StatesManager {
  private val states = HashMap<StateId, State>()

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

  /**
   * Saves all the states to config only when there was a successful computation after "Compute" button click.
   * Successful computation means that all the computation parameters are correct, especially structure description
   * which is regularly edited by a user.
   *
   * To avoid config inconsistency states are saved iff [lastValidState] isn't null.
   * This variable is set in "Compute" button callback only after a successful computation.
   */
  fun saveStates() {
    mapper.writeValueAsString(states.values.toList()).writeTo(KnownPaths.config)
  }

  private fun validate(stateId: StateId) = require(states.containsKey(stateId)) {
    "Only existing state with valid id can be updated, deleted, activated or deactivated"
  }
}

fun activeState() = statesManager.activeState()

fun saveStates() = statesManager.saveStates()

typealias StateId = UUID