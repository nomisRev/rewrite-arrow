package org.example

import arrow.core.continuations.EffectScope

suspend fun test(effect: EffectScope<String>): Int {
  return effect.shift("failure")
}

suspend fun test2(effect: EffectScope<String>): Int {
  effect.ensure(false) { "failure" }
  return 1
}
