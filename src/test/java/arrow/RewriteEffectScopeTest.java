package arrow;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class RewriteEffectScopeTest implements RewriteTest {
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
    void effectScopeParameter() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EffectScope

              suspend fun test(scope: EffectScope<String>): Int {
                return scope.shift("failure")
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise

              suspend fun test(scope: Raise<String>): Int {
                return scope.raise("failure")
              }
              """
          )
        );
    }

    @Test
    void effectScopeReceiver() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EffectScope

              suspend fun EffectScope<String>.test(): Int {
                return shift("failure")
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise

              suspend fun Raise<String>.test(): Int {
                return raise("failure")
              }
              """
          )
        );
    }

    @Test
    void effectScopeReceiverExpression() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EffectScope

              suspend fun EffectScope<String>.test(): Int =
                shift("failure")
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise

              suspend fun Raise<String>.test(): Int =
                raise("failure")
              """
          )
        );
    }

    @Test
    void effectScopeEnsure() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EffectScope

              suspend fun EffectScope<Int>.test(): Unit =
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
    void effectDSLRewrite() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.Effect
              import arrow.core.continuations.effect

              val x: Effect<String, Int> = effect { 1 }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Effect
              import arrow.core.raise.effect

              val x: Effect<String, Int> = effect { 1 }
              """
          )
        );
    }
}