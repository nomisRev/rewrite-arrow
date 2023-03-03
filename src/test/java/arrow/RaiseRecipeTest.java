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

              fun EffectScope<String>.test(): Int {
                ensure(false) { "failure" }
                return 1
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise
              import arrow.core.raise.ensure

              fun Raise<String>.test(): Int {
                ensure(false) { "failure" }
                return 1
              }
              """
          )
        );
    }
}
