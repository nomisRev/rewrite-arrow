package arrow;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class RaiseRecipe extends Recipe {
    @JsonCreator
    public RaiseRecipe() {
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
        return new SayHelloVisitor();
    }

    public class SayHelloVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.Import visitImport(J.Import _import, ExecutionContext executionContext) {
            // TODO rewriting imports can easily be done using yaml
            if (_import.getQualid().isFullyQualifiedClassReference("arrow.core.continuations.EffectScope")) {
                _import.withTemplate(
                        JavaTemplate.builder(this::getCursor, "import arrow.core.raise.Raise").build(),
                        _import.getCoordinates().replace()
                );
            }
            return _import;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(
                J.MethodDeclaration method,
                ExecutionContext executionContext
        ) {
            // TODO Rewrite arrow.core.continuations.EffectScope receiver, or parameters to arrow.core.raise.Raise
            // TODO Rewrite arrow.core.continuations.EagerEffectScope receiver, or parameters to arrow.core.raise.Raise
            return super.visitMethodDeclaration(method, executionContext);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(
                J.MethodInvocation method,
                ExecutionContext executionContext
        ) {
            // TODO Rewrite shift method invocation to raise, this can easily be done with yaml?

            // If the method invocation is an ensure invocation, add an import for ensure
            if (isEnsureInvocation(method)) {
                maybeAddImport("arrow.core.raise.ensure");
            }
            return method;
        }
    }

    public boolean isEnsureInvocation(J.MethodInvocation method) {
        return method.getMethodType() != null &&
                (method.getMethodType().getDeclaringType().getFullyQualifiedName().equals("arrow.core.coroutine.EffectScope") ||
                        method.getMethodType().getDeclaringType().getFullyQualifiedName().equals("arrow.core.coroutine.EagerEffectScope")
                ) && method.getMethodType().getName().equals("ensure");
    }
}
