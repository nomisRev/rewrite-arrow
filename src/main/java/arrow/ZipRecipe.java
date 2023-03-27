package arrow;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType.ShallowClass;
import org.openrewrite.java.tree.Space;
import org.openrewrite.kotlin.KotlinVisitor;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class ZipRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Rewrite zip to zipOrAccumulate";
    }

    @Override
    public String getDescription() {
        return "Rewrites Validated.zip to Either.zipOrAccumulate.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ZipRecipeVisitor();
    }

    public static class ZipRecipeVisitor extends KotlinVisitor<ExecutionContext> {

        private final MethodMatcher matcher = new MethodMatcher("arrow.core.ValidatedKt zip(*)");

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);

            if (matcher.matches(m)) {
                J.Identifier select = new J.Identifier(randomId(),
                        Space.EMPTY,
                        m.getMarkers(),
                        "Either",
                        ShallowClass.build("arrow.core.Either"),
                        null
                );

                final List<Expression> arguments = new ArrayList<>(m.getArguments().size() + 1);
                arguments.add(m.getSelect());
                arguments.add(m.getArguments().get(0).withPrefix(Space.build(" ", emptyList())));
                arguments.addAll(m.getArguments().subList(1, m.getArguments().size()));

                m = m.withArguments(arguments)
                        .withSelect(select)
                        .withName(m.getName().withSimpleName("zipOrAccumulate"));

                maybeRemoveImport("arrow.core.zip");
            }
            return m;
        }
    }
}