package arrow;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
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

    public class RaiseImportVisitor extends KotlinIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(
                J.MethodInvocation method,
                ExecutionContext executionContext
        ) {
            // If the method invocation is an ensure invocation, add an import for ensure
            if (isEnsureInvocation(method)) {
                maybeAddImport("arrow.core.RaiseKt", "ensure", false);
            }
            return method;
        }
    }

    public boolean isEnsureInvocation(J.MethodInvocation method) {
        return method.getMethodType() != null &&
                (method.getMethodType().getDeclaringType().getFullyQualifiedName().equals("arrow.core.continuations.EffectScope") ||
                        method.getMethodType().getDeclaringType().getFullyQualifiedName().equals("arrow.core.continuations.EagerEffectScope")
                ) && method.getMethodType().getName().equals("ensure");
    }
}
