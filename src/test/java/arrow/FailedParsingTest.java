package arrow;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.ChangeType;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class FailedParsingTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeType("a.b.Original", "x.y.Target", true))
          .parser(
            KotlinParser.builder()
              .logCompilationWarningsAndErrors(true)
              .classpath("arrow-core-jvm")
          );
    }

    @Test
    void failedParsing() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.continuations.Effect
              import arrow.core.continuations.either
              import arrow.core.continuations.effect
              
              fun example2(): Either<String, Int> = either.eager {
                ensure(false) { "failure" }
                1
              }
              
              val x: Effect<String, Int> = effect {
                3
              }
              """
          )
        );
    }
}
