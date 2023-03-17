package org.example

import arrow.core.continuations.EffectScope

suspend fun test(effect: EffectScope<String>): Int {
  return effect.shift("failure")
}
