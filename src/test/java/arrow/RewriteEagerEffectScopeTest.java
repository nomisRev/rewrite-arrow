package arrow;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class RewriteEagerEffectScopeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
            Environment.builder()
              .scanRuntimeClasspath()
              .build()
              .activateRecipes("arrow.RaiseRefactor")
          )
          .parser(
            KotlinParser.builder()
              .logCompilationWarningsAndErrors(true)
              .classpath("arrow-core-jvm")
          );
    }

    @Test
    void eagerEffectScopeParameter() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EagerEffectScope

              fun test(scope: EagerEffectScope<String>): Int {
                return scope.shift("failure")
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise

              fun test(scope: Raise<String>): Int {
                return scope.raise("failure")
              }
              """
          )
        );
    }

    @Test
    void eagerEffectScopeReceiver() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EagerEffectScope

              fun EagerEffectScope<String>.test(): Int {
                return shift("failure")
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise

              fun Raise<String>.test(): Int {
                return raise("failure")
              }
              """
          )
        );
    }

    @Test
    void eagerEffectScopeReceiverExpression() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EagerEffectScope

              fun EagerEffectScope<String>.test(): Int =
                shift("failure")
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise

              fun Raise<String>.test(): Int =
                raise("failure")
              """
          )
        );
    }

    @Test
    void eagerEffectScopeEnsure() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EagerEffectScope

              suspend fun EagerEffectScope<Int>.test(): Unit =
                ensure(false) { -1 }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise
              import arrow.core.raise.ensure

              suspend fun Raise<Int>.test(): Unit =
                ensure(false) { -1 }
              """
          )
        );
    }

    @Test
    void eagerEffectDSLRewrite() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EagerEffect
              import arrow.core.continuations.eagerEffect

              val x: EagerEffect<String, Int> = eagerEffect { 1 }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.EagerEffect
              import arrow.core.raise.eagerEffect

              val x: EagerEffect<String, Int> = eagerEffect { 1 }
              """
          )
        );
    }
}