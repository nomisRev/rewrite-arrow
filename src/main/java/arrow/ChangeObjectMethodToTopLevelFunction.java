package arrow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.KotlinIsoVisitor;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeObjectMethodToTopLevelFunction extends Recipe {

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method declarations/invocations.",
            example = "my.package.ObjectName methodName(..)")
    String methodPattern;

    @Option(displayName = "New method name fully qualified import",
            description = "The method name that will replace the existing name.",
            example = "my.new.package.newMethodName(..)")
    String newMethodName;

    @JsonCreator
    public ChangeObjectMethodToTopLevelFunction(
            @JsonProperty("methodPattern") String methodPattern,
            @JsonProperty("newMethodName") String newMethodName
    ) {
        this.methodPattern = methodPattern;
        this.newMethodName = newMethodName;
    }

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
        return Applicability.or(new UsesMethod<>(methodPattern));
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeObjectMethodToTopLevelFunctionVisitor(new MethodMatcher(methodPattern));
    }

    public class ChangeObjectMethodToTopLevelFunctionVisitor extends KotlinIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        private ChangeObjectMethodToTopLevelFunctionVisitor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        // TODO add import for new method
        // TODO Remove import for old method

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
            if (methodMatcher.matches(method) && !method.getSimpleName().equals(newMethodName)) {
                m = m
                         // TODO remove declaring type
//                        .withDeclaringType(JavaType.ShallowClass.build("arrow.core.RaiseKt"))
                        .withDeclaringType(JavaType.ShallowClass.build("arrow.core.RaiseKt"))
                        .withName(m.getName().withSimpleName(newMethodName));
            }
            return m;
        }
    }
}
