package arrow;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Applicability;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
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
                new UsesType<>("arrow.core.raise.Raise"),
                new UsesType<>("arrow.core.raise.Effect"),
                new UsesType<>("arrow.core.raise.EagerEffect")
        );
    }

    private static class RaiseImportVisitor extends KotlinIsoVisitor<ExecutionContext> {
        private final MethodMatcher effectScopeMatcher = new MethodMatcher("arrow.core.continuations.EffectScope ensure(..)");
        private final MethodMatcher eagerEffectScopeMatcher = new MethodMatcher("arrow.core.continuations.EagerEffectScope ensure(..)");
        private final MethodMatcher raiseEnsureMatcher = new MethodMatcher("arrow.core.raise.Raise ensure(..)");

        // We need to override visitLambda, so that visitMethodInvocation will also get called on the lambda's body.
        @Override
        public J visitLambda(J.Lambda lambda, ExecutionContext executionContext) {
            return super.visitLambda(lambda, executionContext);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
            // If we get called on a lambda's body, the method invocation will still be typed to EffectScope & EagerEffectScope.
            if (raiseEnsureMatcher.matches(m) || effectScopeMatcher.matches(m) || eagerEffectScopeMatcher.matches(m)) {
                maybeAddImport("arrow.core.raise.ensure", false);
            }
            return m;
        }
    }
}
