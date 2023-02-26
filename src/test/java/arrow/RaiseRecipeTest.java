package arrow;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class RaiseRecipeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RaiseRecipe())
          .parser(JavaParser.fromJavaVersion().classpath("arrow-core"));
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
