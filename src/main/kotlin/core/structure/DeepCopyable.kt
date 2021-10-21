package core.structure

interface DeepCopyable<T> {
  fun deepCopy(): T
}