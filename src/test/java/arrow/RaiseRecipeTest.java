package arrow;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.ChangeType;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class RaiseRecipeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeType("arrow.core.continuations.EffectScope", "arrow.core.Raise", true))
          .parser(
            KotlinParser.builder()
              .logCompilationWarningsAndErrors(true)
              .classpath("arrow-core-jvm")
          );
    }

    @Test
    void addEnsureImportForRaiseExtensionFunction() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EffectScope

              fun test(scope: EffectScope<String>): Int {
                return scope.shift(false) { "failure" }
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise

              fun test(scope: Raise<String>): Int {
                return scope.shift(false) { "failure" }
              }
              """
          )
        );
    }
}