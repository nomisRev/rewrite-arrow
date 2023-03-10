package arrow;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeType extends Recipe {

    @Option(displayName = "Old fully-qualified type name",
            description = "Fully-qualified class name of the original type.",
            example = "org.junit.Assume")
    String oldFullyQualifiedTypeName;

    @Option(displayName = "New fully-qualified type name",
            description = "Fully-qualified class name of the replacement type, or the name of a primitive such as \"int\". The `OuterClassName$NestedClassName` naming convention should be used for nested classes.",
            example = "org.junit.jupiter.api.Assumptions")
    String newFullyQualifiedTypeName;

    @Option(displayName = "Ignore type definition",
            description = "When set to `true` the definition of the old type will be left untouched. " +
                    "This is useful when you're replacing usage of a class but don't want to rename it.",
            required = false)
    @Nullable
    Boolean ignoreDefinition;

    @Override
    public String getDisplayName() {
        return "Change type";
    }

    @Override
    public String getDescription() {
        return "Change a given type to another.";
    }

    @Override
    public boolean causesAnotherCycle() {
        return true;
    }

    @Override
    protected KotlinVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new KotlinIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visitSourceFile(SourceFile sourceFile, ExecutionContext executionContext) {
                J sf = super.visitSourceFile(sourceFile, executionContext);
                if (!Boolean.TRUE.equals(ignoreDefinition)) {
                    Boolean visit = getCursor().pollNearestMessage("TARGET_CLASS");
                    if (visit != null && visit) {
                        return SearchResult.found(sf);
                    }
                }
                doAfterVisit(new UsesType<>(oldFullyQualifiedTypeName));
                return sf;
            }

            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration cd = (J.ClassDeclaration) super.visitClassDeclaration(classDecl, executionContext);
                if (!Boolean.TRUE.equals(ignoreDefinition) &&
                        TypeUtils.isOfClassType(cd.getType(), oldFullyQualifiedTypeName)) {
                    getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, "TARGET_CLASS", true);
                }
                return cd;
            }
        };
    }

    @Override
    public KotlinVisitor<ExecutionContext> getVisitor() {
        return new ChangeType.ChangeTypeVisitor(oldFullyQualifiedTypeName, newFullyQualifiedTypeName, ignoreDefinition);
    }

    public static class ChangeTypeVisitor extends KotlinVisitor<ExecutionContext> {
        private final JavaType.Class originalType;
        private final JavaType targetType;

        @Nullable
        private final Boolean ignoreDefinition;

        private final Map<JavaType, JavaType> oldNameToChangedType = new IdentityHashMap<>();

        public ChangeTypeVisitor(String oldFullyQualifiedTypeName, String newFullyQualifiedTypeName, @Nullable Boolean ignoreDefinition) {
            this.originalType = JavaType.ShallowClass.build(oldFullyQualifiedTypeName);
            this.targetType = JavaType.buildType(newFullyQualifiedTypeName);
            this.ignoreDefinition = ignoreDefinition;
        }

        @Override
        public J visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
            if (!Boolean.TRUE.equals(ignoreDefinition)) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(targetType);
                if (fq != null) {
                    ChangeType.ChangeClassDefinition changeClassDefinition = new ChangeType.ChangeClassDefinition(originalType.getFullyQualifiedName(), fq.getFullyQualifiedName());
                    cu = (JavaSourceFile) changeClassDefinition.visit(cu, executionContext);
                    assert cu != null;
                }
            }
            return super.visitJavaSourceFile(cu, executionContext);
        }

        @SuppressWarnings({"unchecked", "rawtypes", "ConstantConditions"})
        @Override
        public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J.CompilationUnit c = visitAndCast(cu, ctx, super::visitCompilationUnit);
            if (targetType instanceof JavaType.FullyQualified) {
                for (J.Import anImport : c.getImports()) {
                    if (anImport.isStatic()) {
                        continue;
                    }

                    JavaType maybeType = anImport.getQualid().getType();
                    if (maybeType instanceof JavaType.FullyQualified) {
                        JavaType.FullyQualified type = (JavaType.FullyQualified) maybeType;
                        if (originalType.getFullyQualifiedName().equals(type.getFullyQualifiedName())) {
                            c = (J.CompilationUnit) new RemoveImport<ExecutionContext>(originalType.getFullyQualifiedName()).visit(c, ctx);
                            assert c != null;
                        } else if (originalType.getOwningClass() != null && originalType.getOwningClass().getFullyQualifiedName().equals(type.getFullyQualifiedName())) {
                            c = (J.CompilationUnit) new RemoveImport<ExecutionContext>(originalType.getOwningClass().getFullyQualifiedName()).visit(c, ctx);
                            assert c != null;
                        }
                    }
                }
            }

            JavaType.FullyQualified fullyQualifiedTarget = TypeUtils.asFullyQualified(targetType);
            Set<String> classNames = fullyQualifiedTarget == null ? emptySet() : cu.getClasses().stream()
                    .filter(it -> it.getType() != null)
                    .map(it -> it.getType().getFullyQualifiedName())
                    .collect(Collectors.toSet());

            if (fullyQualifiedTarget != null && !(fullyQualifiedTarget.getOwningClass() != null && classNames.contains(getTopLevelClassName(fullyQualifiedTarget)))) {
                if (fullyQualifiedTarget.getOwningClass() != null && !"java.lang".equals(fullyQualifiedTarget.getPackageName())) {
                    c = (J.CompilationUnit) new AddImport(fullyQualifiedTarget.getOwningClass().getFullyQualifiedName(), null, true).visit(c, ctx);
                }
                if (!"java.lang".equals(fullyQualifiedTarget.getPackageName())) {
                    c = (J.CompilationUnit) new AddImport(fullyQualifiedTarget.getFullyQualifiedName(), null, true).visit(c, ctx);
                }
            }

            if (c != null) {
                c = c.withImports(ListUtils.map(c.getImports(), i -> visitAndCast(i, ctx, super::visitImport)));
            }
            return c;
        }

        @Override
        public J visitImport(J.Import impoort, ExecutionContext executionContext) {
            // visitCompilationUnit() handles changing the imports.
            // If we call super.visitImport() then visitFieldAccess() will change the imports before AddImport/RemoveImport see them.
            // visitFieldAccess() doesn't have the import-specific formatting logic that AddImport/RemoveImport do.
            return impoort;
        }

        @Override
        public @Nullable JavaType visitType(@Nullable JavaType javaType, ExecutionContext executionContext) {
            return updateType(javaType);
        }

        @Override
        public @Nullable J postVisit(J tree, ExecutionContext executionContext) {
            J j = super.postVisit(tree, executionContext);
            if (j instanceof J.MethodDeclaration) {
                J.MethodDeclaration method = (J.MethodDeclaration) j;
                j = method.withMethodType(updateType(method.getMethodType()));
            } else if (j instanceof J.MethodInvocation) {
                J.MethodInvocation method = (J.MethodInvocation) j;
                j = method.withMethodType(updateType(method.getMethodType()));
            } else if (j instanceof J.NewClass) {
                J.NewClass n = (J.NewClass) j;
                j = n.withConstructorType(updateType(n.getConstructorType()));
            } else if (tree instanceof TypedTree) {
                j = ((TypedTree) tree).withType(updateType(((TypedTree) tree).getType()));
            }
            return j;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            if (fieldAccess.isFullyQualifiedClassReference(originalType.getFullyQualifiedName())) {
                if (targetType instanceof JavaType.FullyQualified) {
                    return updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getFullyQualifiedName())
                            .withPrefix(fieldAccess.getPrefix()));
                } else if (targetType instanceof JavaType.Primitive) {
                    return new J.Primitive(
                            fieldAccess.getId(),
                            fieldAccess.getPrefix(),
                            Markers.EMPTY,
                            (JavaType.Primitive) targetType
                    );
                }
            } else {
                StringBuilder maybeClass = new StringBuilder();
                for (Expression target = fieldAccess; target != null; ) {
                    if (target instanceof J.FieldAccess) {
                        J.FieldAccess fa = (J.FieldAccess) target;
                        maybeClass.insert(0, fa.getSimpleName()).insert(0, '.');
                        target = fa.getTarget();
                    } else if (target instanceof J.Identifier) {
                        maybeClass.insert(0, ((J.Identifier) target).getSimpleName());
                        target = null;
                    } else {
                        maybeClass = new StringBuilder("__NOT_IT__");
                        break;
                    }
                }
                JavaType.Class oldType = JavaType.ShallowClass.build(originalType.getFullyQualifiedName());
                if (maybeClass.toString().equals(oldType.getClassName())) {
                    maybeRemoveImport(oldType.getOwningClass());
                    Expression e = updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getClassName())
                            .withPrefix(fieldAccess.getPrefix()));
                    // If a FieldAccess like Map.Entry has been replaced with an Identifier, ensure that identifier has the correct type
                    if (e instanceof J.Identifier && e.getType() == null) {
                        J.Identifier i = (J.Identifier) e;
                        e = i.withType(targetType);
                    }
                    return e;
                }
            }
            return super.visitFieldAccess(fieldAccess, ctx);
        }

        @Override
        public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            // if the ident's type is equal to the type we're looking for, and the classname of the type we're looking for is equal to the ident's string representation
            // Then transform it, otherwise leave it alone
            if (TypeUtils.isOfClassType(ident.getType(), originalType.getFullyQualifiedName())) {
                String className = originalType.getClassName();
                JavaType.FullyQualified iType = TypeUtils.asFullyQualified(ident.getType());
                if (iType != null && iType.getOwningClass() != null) {
                    className = originalType.getFullyQualifiedName().substring(iType.getOwningClass().getFullyQualifiedName().length() + 1);
                }

                if (ident.getSimpleName().equals(className)) {
                    if (targetType instanceof JavaType.FullyQualified) {
                        if (((JavaType.FullyQualified) targetType).getOwningClass() != null) {
                            return updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getClassName())
                                    .withType(null)
                                    .withPrefix(ident.getPrefix()));
                        } else {
                            ident = ident.withSimpleName(((JavaType.FullyQualified) targetType).getClassName());
                        }
                    } else if (targetType instanceof JavaType.Primitive) {
                        ident = ident.withSimpleName(((JavaType.Primitive) targetType).getKeyword());
                    }
                }
            }
            ident = ident.withType(updateType(ident.getType()));
            return visitAndCast(ident, ctx, super::visitIdentifier);
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if (method.getMethodType() != null && method.getMethodType().hasFlags(Flag.Static)) {
                if (method.getMethodType().getDeclaringType().isAssignableFrom(originalType)) {
                    JavaSourceFile cu = getCursor().firstEnclosingOrThrow(JavaSourceFile.class);

                    for (J.Import anImport : cu.getImports()) {
                        if (anImport.isStatic() && anImport.getQualid().getTarget().getType() != null) {
                            JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType());
                            if (fqn != null && TypeUtils.isOfClassType(fqn, originalType.getFullyQualifiedName()) &&
                                    method.getSimpleName().equals(anImport.getQualid().getSimpleName())) {
                                maybeAddImport(((JavaType.FullyQualified) targetType).getFullyQualifiedName(), method.getName().getSimpleName());
                                break;
                            }
                        }
                    }
                }
            }
            return super.visitMethodInvocation(method, ctx);
        }

        private Expression updateOuterClassTypes(Expression typeTree) {
            if (typeTree instanceof J.FieldAccess) {
                JavaType.FullyQualified type = (JavaType.FullyQualified) targetType;

                if (type.getOwningClass() == null) {
                    // just a performance shortcut when this isn't an inner class
                    typeTree.withType(updateType(targetType));
                }

                Stack<Expression> typeStack = new Stack<>();
                typeStack.push(typeTree);

                Stack<JavaType.FullyQualified> attrStack = new Stack<>();
                attrStack.push(type);

                for (Expression t = ((J.FieldAccess) typeTree).getTarget(); ; ) {
                    typeStack.push(t);
                    if (t instanceof J.FieldAccess) {
                        if (Character.isUpperCase(((J.FieldAccess) t).getSimpleName().charAt(0))) {
                            if (attrStack.peek().getOwningClass() != null) {
                                attrStack.push(attrStack.peek().getOwningClass());
                            }
                        }
                        t = ((J.FieldAccess) t).getTarget();
                    } else if (t instanceof J.Identifier) {
                        if (Character.isUpperCase(((J.Identifier) t).getSimpleName().charAt(0))) {
                            if (attrStack.peek().getOwningClass() != null) {
                                attrStack.push(attrStack.peek().getOwningClass());
                            }
                        }
                        break;
                    }
                }

                Expression attributed = null;
                for (Expression e = typeStack.pop(); ; e = typeStack.pop()) {
                    if (e instanceof J.Identifier) {
                        if (attrStack.size() == typeStack.size() + 1) {
                            attributed = ((J.Identifier) e).withType(attrStack.pop());
                        } else {
                            attributed = e;
                        }
                    } else if (e instanceof J.FieldAccess) {
                        if (attrStack.size() == typeStack.size() + 1) {
                            attributed = ((J.FieldAccess) e).withTarget(attributed)
                                    .withType(attrStack.pop());
                        } else {
                            attributed = ((J.FieldAccess) e).withTarget(attributed);
                        }
                    }
                    if (typeStack.isEmpty()) {
                        break;
                    }
                }

                assert attributed != null;
                return attributed;
            }
            return typeTree;
        }

        @Nullable
        private JavaType updateType(@Nullable JavaType oldType) {
            if (oldType == null || oldType instanceof JavaType.Unknown) {
                return oldType;
            }

            JavaType type = oldNameToChangedType.get(oldType);
            if (type != null) {
                return type;
            }

            if (oldType instanceof JavaType.Parameterized) {
                JavaType.Parameterized pt = (JavaType.Parameterized) oldType;
                pt = pt.withTypeParameters(ListUtils.map(pt.getTypeParameters(), tp -> {
                    if (tp instanceof JavaType.FullyQualified) {
                        JavaType.FullyQualified tpFq = (JavaType.FullyQualified) tp;
                        if (isTargetFullyQualifiedType(tpFq)) {
                            return updateType(tpFq);
                        }
                    }
                    return tp;
                }));

                if (isTargetFullyQualifiedType(pt)) {
                    pt = pt.withType((JavaType.FullyQualified) updateType(pt.getType()));
                }
                oldNameToChangedType.put(oldType, pt);
                oldNameToChangedType.put(pt, pt);
                return pt;
            } else if (oldType instanceof JavaType.FullyQualified) {
                JavaType.FullyQualified original = TypeUtils.asFullyQualified(oldType);
                if (isTargetFullyQualifiedType(original)) {
                    return targetType;
                }
            } else if (oldType instanceof JavaType.GenericTypeVariable) {
                JavaType.GenericTypeVariable gtv = (JavaType.GenericTypeVariable) oldType;
                gtv = gtv.withBounds(ListUtils.map(gtv.getBounds(), b -> {
                    if (b instanceof JavaType.FullyQualified && isTargetFullyQualifiedType((JavaType.FullyQualified) b)) {
                        return updateType(b);
                    }
                    return b;
                }));

                oldNameToChangedType.put(oldType, gtv);
                oldNameToChangedType.put(gtv, gtv);
                return gtv;
            } else if (oldType instanceof JavaType.Variable) {
                JavaType.Variable variable = (JavaType.Variable) oldType;
                variable = variable.withOwner(updateType(variable.getOwner()));
                variable = variable.withType(updateType(variable.getType()));
                oldNameToChangedType.put(oldType, variable);
                oldNameToChangedType.put(variable, variable);
                return variable;
            } else if (oldType instanceof JavaType.Array) {
                JavaType.Array array = (JavaType.Array) oldType;
                array = array.withElemType(updateType(array.getElemType()));
                oldNameToChangedType.put(oldType, array);
                oldNameToChangedType.put(array, array);
                return array;
            }

            return oldType;
        }

        @Nullable
        private JavaType.Method updateType(@Nullable JavaType.Method oldMethodType) {
            if (oldMethodType != null) {
                JavaType.Method method = (JavaType.Method) oldNameToChangedType.get(oldMethodType);
                if (method != null) {
                    return method;
                }

                method = oldMethodType;
                method = method.withDeclaringType((JavaType.FullyQualified) updateType(method.getDeclaringType()))
                        .withReturnType(updateType(method.getReturnType()))
                        .withParameterTypes(ListUtils.map(method.getParameterTypes(), this::updateType));
                oldNameToChangedType.put(oldMethodType, method);
                oldNameToChangedType.put(method, method);
                return method;
            }
            return null;
        }

        private boolean isTargetFullyQualifiedType(@Nullable JavaType.FullyQualified fq) {
            return fq != null && TypeUtils.isOfClassType(fq, originalType.getFullyQualifiedName()) && targetType instanceof JavaType.FullyQualified;
        }
    }

    private static class ChangeClassDefinition extends JavaIsoVisitor<ExecutionContext> {
        private final JavaType.Class originalType;
        private final JavaType.Class targetType;
        private final MethodMatcher originalConstructor;

        private ChangeClassDefinition(String oldFullyQualifiedTypeName, String newFullyQualifiedTypeName) {
            this.originalType = JavaType.ShallowClass.build(oldFullyQualifiedTypeName);
            this.targetType = JavaType.ShallowClass.build(newFullyQualifiedTypeName);
            this.originalConstructor = new MethodMatcher(oldFullyQualifiedTypeName + "<constructor>(..)");
        }

        @Override
        public JavaSourceFile visitJavaSourceFile(JavaSourceFile sf, ExecutionContext ctx) {
            String oldPath = ((SourceFile) sf).getSourcePath().toString().replace('\\', '/');
            // The old FQN must exist in the path.
            String oldFqn = fqnToPath(originalType.getFullyQualifiedName());
            String newFqn = fqnToPath(targetType.getFullyQualifiedName());

            Path newPath = Paths.get(oldPath.replaceFirst(oldFqn, newFqn));
            if (updatePath(sf, oldPath, newPath.toString())) {
                sf = ((SourceFile) sf).withSourcePath(newPath);
            }
            return super.visitJavaSourceFile(sf, ctx);
        }

        private String fqnToPath(String fullyQualifiedName) {
            int index = fullyQualifiedName.indexOf("$");
            String topLevelClassName = index == -1 ? fullyQualifiedName : fullyQualifiedName.substring(0, index);
            return topLevelClassName.replace('.', '/');
        }

        private boolean updatePath(JavaSourceFile sf, String oldPath, String newPath) {
            return !oldPath.equals(newPath) && sf.getClasses().stream()
                    .anyMatch(o -> !J.Modifier.hasModifier(o.getModifiers(), J.Modifier.Type.Private) &&
                            o.getType() != null && !o.getType().getFullyQualifiedName().contains("$") &&
                            TypeUtils.isOfClassType(o.getType(), getTopLevelClassName(originalType)));
        }

        @Override
        public J.Package visitPackage(J.Package pkg, ExecutionContext executionContext) {
            J.Package p = super.visitPackage(pkg, executionContext);
            String original = p.getExpression().printTrimmed(getCursor()).replaceAll("\\s", "");
            if (original.equals(originalType.getPackageName())) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(targetType);
                if (fq != null) {
                    if (fq.getPackageName().isEmpty()) {
                        getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, "UPDATE_PREFIX", true);
                        p = null;
                    } else {
                        String newPkg = targetType.getPackageName();
                        p = p.withTemplate(JavaTemplate.builder(this::getCursor, newPkg).build(), p.getCoordinates().replace());
                    }
                }
            }
            //noinspection ConstantConditions
            return p;
        }

        @Override
        public J.Import visitImport(J.Import _import, ExecutionContext executionContext) {
            Boolean updatePrefix = getCursor().pollNearestMessage("UPDATE_PREFIX");
            if (updatePrefix != null && updatePrefix) {
                _import = _import.withPrefix(Space.EMPTY);
            }
            return super.visitImport(_import, executionContext);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
            Boolean updatePrefix = getCursor().pollNearestMessage("UPDATE_PREFIX");
            if (updatePrefix != null && updatePrefix) {
                cd = cd.withPrefix(Space.EMPTY);
            }

            if (TypeUtils.isOfClassType(classDecl.getType(), originalType.getFullyQualifiedName())) {
                String newClassName = getNewClassName(targetType);
                cd = cd.withName(cd.getName().withSimpleName(newClassName));
                cd = cd.withType(updateType(cd.getType()));
            }
            return cd;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            if (method.isConstructor() && originalConstructor.matches(method.getMethodType())) {
                method = method.withName(method.getName().withSimpleName(targetType.getClassName()));
                method = method.withMethodType(updateType(method.getMethodType()));
            }
            return super.visitMethodDeclaration(method, executionContext);
        }

        private String getNewClassName(JavaType.FullyQualified fq) {
            return fq.getOwningClass() == null ? fq.getClassName() :
                    fq.getFullyQualifiedName().substring(fq.getOwningClass().getFullyQualifiedName().length() + 1);
        }

        private JavaType updateType(@Nullable JavaType oldType) {
            if (oldType instanceof JavaType.FullyQualified) {
                JavaType.FullyQualified original = TypeUtils.asFullyQualified(oldType);
                if (isTargetFullyQualifiedType(original)) {
                    return targetType;
                }
            }

            //noinspection ConstantConditions
            return oldType;
        }

        @Nullable
        private JavaType.Method updateType(@Nullable JavaType.Method mt) {
            if (mt != null) {
                return mt.withDeclaringType((JavaType.FullyQualified) updateType(mt.getDeclaringType()))
                        .withReturnType(updateType(mt.getReturnType()))
                        .withParameterTypes(ListUtils.map(mt.getParameterTypes(), this::updateType));
            }
            return null;
        }

        private boolean isTargetFullyQualifiedType(@Nullable JavaType.FullyQualified fq) {
            return fq != null && TypeUtils.isOfClassType(fq, originalType.getFullyQualifiedName());
        }
    }

    public static String getTopLevelClassName(JavaType.FullyQualified classType) {
        if (classType.getOwningClass() == null) {
            return classType.getFullyQualifiedName();
        }
        return getTopLevelClassName(classType.getOwningClass());
    }
}
