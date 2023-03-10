package arrow;

import com.fasterxml.jackson.annotation.JsonCreator;
import kotlin.collections.CollectionsKt;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;

import java.lang.invoke.MethodType;
import java.util.stream.Collectors;

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

    public class SayHelloVisitor extends KotlinIsoVisitor<ExecutionContext> {

//        @Override
//        public J.Import visitImport(J.Import _import, ExecutionContext executionContext) {
//            // TODO rewriting imports can easily be done using yaml
//            if (_import.getQualid().isFullyQualifiedClassReference("arrow.core.continuations.EffectScope")) {
//                _import.withTemplate(
//                        JavaTemplate.builder(this::getCursor, "import arrow.core.raise.Raise").build(),
//                        _import.getCoordinates().replace()
//                );
//            }
//            return _import;
//        }

//        @Override
//        public J.MethodDeclaration visitMethodDeclaration(
//                J.MethodDeclaration method,
//                ExecutionContext executionContext
//        ) {
//            // TODO Rewrite arrow.core.continuations.EffectScope receiver, or parameters to arrow.core.raise.Raise
//            // TODO Rewrite arrow.core.continuations.EagerEffectScope receiver, or parameters to arrow.core.raise.Raise
//            return super.visitMethodDeclaration(method, executionContext);
//        }


//        @Override
//        public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
//            // TODO Rewrite arrow.core.continuations.EffectScope receiver, or parameters to arrow.core.raise.Raise
//            // TODO Rewrite arrow.core.continuations.EagerEffectScope receiver, or parameters to arrow.core.raise.Raise
//
//            if (
//                    method.getParameters().stream().anyMatch((statement) -> {
//                        J.VariableDeclarations padded = (J.VariableDeclarations) statement;
//                        return padded.getVariables().stream().anyMatch((variable) -> {
//                            JavaType.Parameterized parameterized = (JavaType.Parameterized) variable.getInitializer().getType();
//                            return parameterized.getFullyQualifiedName().equals("arrow.core.continuations.EffectScope");
//                        });
//                    })
//            ) {
//                J.MethodDeclaration method2 = method.withParameters(
//                        method.getParameters().stream().map((statement) -> {
//                            if (statement instanceof J.VariableDeclarations) {
//                                J.VariableDeclarations padded = (J.VariableDeclarations) statement;
//                                J.VariableDeclarations updated = padded.withVariables(
//
//                                        padded.getVariables().stream().map((variable) -> {
//                                            JavaType.Parameterized parameterized = (JavaType.Parameterized) variable.getInitializer().getType();
//                                            if (parameterized.getFullyQualifiedName().equals("arrow.core.continuations.EffectScope")) {
//                                                String s = parameterized.getTypeParameters().get(0).toString();
//
////                                                J.VariableDeclarations.NamedVariable j = variable.withTemplate(
////                                                        JavaTemplate.builder(
////                                                                this::getCursor,
////                                                                "Raise<kotlin.String>"
////                                                        ).imports("arrow.core.raise.Raise").build(),
////                                                        variable.getInitializer().getCoordinates().replace()
////                                                );
//                                                return variable;
//
////                                                JavaType.FullyQualified updated2 = parameterized.withFullyQualifiedName("arrow.core.raise.Raise");
////                                                JavaType.Parameterized parameterized1 = parameterized.withType(updated2);
////                                                J.VariableDeclarations.NamedVariable namedVariable = variable
////                                                        .withType(parameterized1)
////                                                        .withInitializer(variable.getInitializer().withType(parameterized1));
////                                                return variable;
//                                            } else {
//                                                return variable;
//                                            }
//                                        }).collect(Collectors.toList()));
//                                return updated;
//                            } else {
//                                return statement;
//                            }
//                        }).collect(Collectors.toList())
//                );
//                return method2;
//            }
//            return super.visitMethodDeclaration(method, executionContext);
//        }

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
                (method.getMethodType().getDeclaringType().getFullyQualifiedName().equals("arrow.core.continuations.EffectScope") ||
                        method.getMethodType().getDeclaringType().getFullyQualifiedName().equals("arrow.core.continuations.EagerEffectScope")
                ) && method.getMethodType().getName().equals("ensure");
    }
}
