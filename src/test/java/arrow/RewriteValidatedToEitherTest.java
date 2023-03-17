package arrow;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class RewriteValidatedToEitherTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
            Environment.builder()
              .scanRuntimeClasspath()
              .build()
              .activateRecipes("arrow.ValidatedToEitherRecipe")
          )
          .parser(
            KotlinParser.builder()
              .logCompilationWarningsAndErrors(true)
              .classpath("arrow-core-jvm")
          );
    }

    @Test
    void rewriteTopLevelExtensionFunctions() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.Validated
              import arrow.core.valid

              val x: Validated<String, Int> = 1.valid()
              """,
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.right

              val x: Either<String, Int> = 1.right()
              """
          )
        );
    }
}