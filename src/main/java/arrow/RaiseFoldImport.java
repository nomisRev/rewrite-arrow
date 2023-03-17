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
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinIsoVisitor;

@Value
@EqualsAndHashCode(callSuper = true)
public class RaiseFoldImport extends Recipe {

    private final String foldPattern = "arrow.core.continuations.Effect fold(..)";
    private final String eagerFoldPattern = "arrow.core.continuations.EagerEffect fold(..)";

    @JsonCreator
    public RaiseFoldImport() {
    }

    @Override
    public String getDisplayName() {
        return "Raise Fold Import";
    }

    @Override
    public String getDescription() {
        return "Add import for arrow.core.raise.fold.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RaiseFoldImportVisitor(new MethodMatcher(foldPattern), new MethodMatcher(eagerFoldPattern));
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.or(new UsesMethod<>(foldPattern), new UsesMethod<>(eagerFoldPattern));
    }

    private static class RaiseFoldImportVisitor extends KotlinIsoVisitor<ExecutionContext> {
        private final MethodMatcher foldEffectMatcher;
        private final MethodMatcher foldEagerEffectMatcher;

        public RaiseFoldImportVisitor(MethodMatcher foldEffectMatcher, MethodMatcher foldEagerEffectMatcher) {
            this.foldEffectMatcher = foldEffectMatcher;
            this.foldEagerEffectMatcher = foldEagerEffectMatcher;
        }


        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
            if (foldEffectMatcher.matches(m) || foldEagerEffectMatcher.matches(m)) {
                maybeAddImport("arrow.core.raise.fold", false);
            }
            return m;
        }
    }
}
