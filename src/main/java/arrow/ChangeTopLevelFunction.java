package arrow;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinIsoVisitor;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeTopLevelFunction extends Recipe {

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method declarations/invocations.",
            example = "my.package.ObjectName methodName(..)")
    String methodPattern;

    @Option(displayName = "New Method pattern",
            description = "The method name that will replace the existing name.",
            example =  "methodName(..)")
    String newMethodName;

    @Option(displayName = "New Method Import",
            description = "The import for the new method name that will replace the existing name.",
            example =  "my.package.methodName")
    String newMethodImport;

    @JsonCreator
    public ChangeTopLevelFunction(
            @JsonProperty("methodPattern") String methodPattern,
            @JsonProperty("newMethodPattern") String newMethodName,
            @JsonProperty("newMethodImport") String newMethodImport
    ) {
        this.methodPattern = methodPattern;
        this.newMethodName = newMethodName;
        this.newMethodImport = newMethodImport;
    }

    @Override
    public String getDisplayName() {
        return "Rewrite top level function";
    }

    @Override
    public String getDescription() {
        return "Rewrites Kotlin's top level functions from one to another.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.or(new UsesMethod<>(methodPattern));
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeTopLevelFunctionVisitor(new MethodMatcher(methodPattern));
    }

    public class ChangeTopLevelFunctionVisitor extends KotlinIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        private ChangeTopLevelFunctionVisitor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        // TODO add import for new method
        // TODO Remove import for old method

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
            if (methodMatcher.matches(method) && !method.getSimpleName().equals(newMethodName)) {
                m = m.withName(m.getName().withSimpleName(newMethodName));
                maybeAddImport(newMethodImport);
            }
            return m;
        }
    }
}
