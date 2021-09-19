package core.state

interface StateComponent {
  fun updateFromUI()

  fun updateUI()
}