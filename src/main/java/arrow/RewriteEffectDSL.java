package arrow;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinIsoVisitor;

/**
 * Rewrite object method invocations to top level functions.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class RewriteEffectDSL extends Recipe {

    private final String eagerPattern = "arrow.core.continuations.either eager(..)";
    private final String invokePattern = "arrow.core.continuations.either invoke(..)";
    private final String implicitInvokePattern = "arrow.core.continuations.either either(..)";

    @Override
    public String getDisplayName() {
        return "Raise Rewrite";
    }

    @Override
    public String getDescription() {
        return "Rewrites Kotlin's object method invocations to top level functions.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.or(
                new UsesMethod<>(eagerPattern),
                new UsesMethod<>(invokePattern),
                new UsesMethod<>(implicitInvokePattern)
        );
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeObjectMethodToTopLevelFunctionVisitor();
    }

    public static class ChangeObjectMethodToTopLevelFunctionVisitor extends KotlinIsoVisitor<ExecutionContext> {
        MethodMatcher eager = new MethodMatcher("arrow.core.continuations.either eager(..)");
        MethodMatcher implicitInvoke = new MethodMatcher("arrow.core.continuations.either either(..)");
        MethodMatcher invoke = new MethodMatcher("arrow.core.continuations.either invoke(..)");

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
            boolean implicit = implicitInvoke.matches(method);
            boolean matches = eager.matches(method) || implicit || invoke.matches(method);

            if (matches) {
                m = m.withName(m.getName().withSimpleName("either"));

                if (!implicit) {
                    m = m.withSelect(null);
                }

                maybeAddImport("arrow.core.raise.either", null, false);
                maybeRemoveImport("arrow.core.continuations.either");
                maybeRemoveImport("arrow.core.continuations.eager");
            }
            return m;
        }
    }
}
