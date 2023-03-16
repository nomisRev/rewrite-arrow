package arrow;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Applicability;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinIsoVisitor;

@Value
@EqualsAndHashCode(callSuper = true)
public class RaiseAddImport extends Recipe {
    @JsonCreator
    public RaiseAddImport() {
    }

    @Override
    public String getDisplayName() {
        return "Raise Rewrite";
    }

    @Override
    public String getDescription() {
        return "Rewrites all imports, and builders from arrow.core.computations.* and arrow.core.continuations.* to arrow.core.raise.*.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RaiseImportVisitor();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.or(
                new UsesType<>("arrow.core.continuations.EffectScope"),
                new UsesType<>("arrow.core.continuations.EagerEffectScope")
        );
    }

    public class RaiseImportVisitor extends KotlinIsoVisitor<ExecutionContext> {
        MethodMatcher effectScopeMatcher = new MethodMatcher("arrow.core.continuations.EffectScope ensure(..)");
        MethodMatcher eagerEffectScopeMatcher = new MethodMatcher("arrow.core.continuations.EagerEffectScope ensure(..)");

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            // If the method invocation is an ensure invocation, add an import for ensure
            if (effectScopeMatcher.matches(method) || eagerEffectScopeMatcher.matches(method)) {
                maybeAddImport("arrow.core.raise.RaiseKt", "ensure");
                maybeAddImport("arrow.core.raise.ensure");
            }
            return method;
        }
    }
}
