package arrow;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;

class SayHelloRecipeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SayHelloRecipe("com.yourorg.FooBar"));
    }

    @Test
    void addsHelloToFooBar() {
        rewriteRun(
          //language=kotlin
          kotlin(
            """
              package com.yourorg

              class FooBar {
              }
              """,
            """
              package com.yourorg

              class FooBar {
                  public fun hello(): String {
                      return "Hello from com.yourorg.FooBar!"
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotChangeExistingHello() {
        rewriteRun(
          //language=kotlin
          kotlin(
            """
              package com.yourorg

              class FooBar {
                  public fun hello(): String {
                      return "Hello from com.yourorg.FooBar!"
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotChangeOtherClasses() {
        rewriteRun(
          //language=kotlin
          kotlin(
            """
              package com.yourorg

              class Bash {
              }
              """
          )
        );
    }
}
