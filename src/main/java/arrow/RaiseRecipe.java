package arrow;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

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
        return new ChangeType.ChangeTypeVisitor(
                "arrow.core.continuations.EffectScope",
                "arrow.core.raise.Raise",
                true
        );
    }

//    public class SayHelloVisitor extends KotlinIsoVisitor<ExecutionContext> {
//        @Override
//        public J.MethodInvocation visitMethodInvocation(
//                J.MethodInvocation method,
//                ExecutionContext executionContext
//        ) {
//            // TODO Rewrite shift method invocation to raise, this can easily be done with yaml?
//
//            // If the method invocation is an ensure invocation, add an import for ensure
//            if (isEnsureInvocation(method)) {
//                maybeAddImport("arrow.core.raise.ensure");
//            }
//            return method;
//        }
//    }
//
//    public boolean isEnsureInvocation(J.MethodInvocation method) {
//        return method.getMethodType() != null &&
//                (method.getMethodType().getDeclaringType().getFullyQualifiedName().equals("arrow.core.continuations.EffectScope") ||
//                        method.getMethodType().getDeclaringType().getFullyQualifiedName().equals("arrow.core.continuations.EagerEffectScope")
//                ) && method.getMethodType().getName().equals("ensure");
//    }
}
