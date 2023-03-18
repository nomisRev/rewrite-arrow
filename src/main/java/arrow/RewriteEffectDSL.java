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

    private final String oldPackage = "arrow.core.continuations.";
    private final String dslName = "either";
    private final String fullyQualifiedObject = oldPackage + dslName;
    private final String eagerPattern = fullyQualifiedObject + " eager(..)";
    private final String invokePattern = fullyQualifiedObject + " invoke(..)";
    private final String implicitInvokePattern = fullyQualifiedObject + " " + dslName + "(..)";

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

    public class ChangeObjectMethodToTopLevelFunctionVisitor extends KotlinIsoVisitor<ExecutionContext> {
        MethodMatcher eager = new MethodMatcher(eagerPattern);
        MethodMatcher implicitInvoke = new MethodMatcher(implicitInvokePattern);
        MethodMatcher invoke = new MethodMatcher(invokePattern);

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
            boolean implicit = implicitInvoke.matches(method);
            boolean matches = eager.matches(method) || implicit || invoke.matches(method);

            if (matches) {
                m = m.withName(m.getName().withSimpleName(dslName));

                if (!implicit) {
                    m = m.withSelect(null);
                }

                String topLevelImport = "arrow.core.raise." + dslName;
                maybeAddImport(topLevelImport, null, false);
                maybeRemoveImport(fullyQualifiedObject);
                maybeRemoveImport(oldPackage + "eager");
            }
            return m;
        }
    }
}
