package core.structure

interface Copyable<T> {
  fun deepCopy(): T
}