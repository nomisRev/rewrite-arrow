package arrow;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class SayHelloRecipeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SayHelloRecipe("com.yourorg.FooBar"));
    }

    @Test
    void addsHelloToFooBar() {
        rewriteRun(
          //language=java
          java(
            """
              package com.yourorg;

              class FooBar {
              }
              """,
            """
              package com.yourorg;

              class FooBar {
                  public String hello() {
                      return "Hello from com.yourorg.FooBar!";
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotChangeExistingHello() {
        rewriteRun(
          //language=java
          java(
            """
              package com.yourorg;

              class FooBar {
                  public String hello() { return ""; }
              }
              """
          )
        );
    }

    @Test
    void doesNotChangeOtherClasses() {
        rewriteRun(
          //language=java
          java(
            """
              package com.yourorg;

              class Bash {
              }
              """
          )
        );
    }
}
