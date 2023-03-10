package arrow;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class RaiseRecipeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RaiseRecipe())
          .parser(
            KotlinParser.builder()
              .logCompilationWarningsAndErrors(true)
              .classpath("arrow-core")
          );
    }

    @Test
    void addEnsureImportForRaiseExtensionFunction() {
        rewriteRun(
          //language=kotlin
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
