package com.osmerion.optin.tools.apt;

import com.osmerion.optin.RequiresOptIn;
import com.osmerion.optin.tools.apt.markers.AnnotationMarker;
import com.osmerion.optin.tools.apt.markers.OptInMarker;
import com.osmerion.optin.tools.apt.markers.RequirementMarker;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.annotation.Nullable;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.function.BiFunction;

/**
 * TODO doc
 *
 * @author  Leon Linhart
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class OptInProcessor extends AbstractProcessor {

    private static final String OPT_IN_FQ_NAME = "com.osmerion.optin.OptIn";
    private static final String KOTLIN_OPT_IN_FQ_NAME = "kotlin.OptIn";

    private static final String REQUIRES_OPT_IN_FQ_NAME = "com.osmerion.optin.RequiresOptIn";
    private static final String KOTLIN_REQUIRES_OPT_IN_FQ_NAME = "kotlin.RequiresOptIn";

    private static final ElementType[] DEFAULT_ANNOTATION_TARGETS = new ElementType[] {
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.LOCAL_VARIABLE,
        ElementType.METHOD,
        ElementType.MODULE,
        ElementType.PACKAGE,
        ElementType.PARAMETER,
        ElementType.TYPE,
        ElementType.TYPE_PARAMETER
    };

    private static EnumSet<ElementType> SUPPORTED_REQUIREMENT_MARKER_TARGETS = EnumSet.of(
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.MODULE,
        ElementType.PACKAGE,
        ElementType.TYPE
    );

    private Elements elements;
    private Messager messager;
    private Trees trees;
    private Types types;

    private OptInElementVisitor2 elementVisitor;
    private OptInTreeVisitor2 treeVisitor;
    private OptInTypeVisitor2 typeVisitor;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.messager = processingEnv.getMessager();
        this.trees = Trees.instance(processingEnv);
        this.types = processingEnv.getTypeUtils();

        this.elementVisitor = new OptInElementVisitor2();
        this.treeVisitor = new OptInTreeVisitor2();
        this.typeVisitor = new OptInTypeVisitor2();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getRootElements()) {
            TreePath rootPath = trees.getPath(element);

            OptInResolverContext optInResolver = new OptInResolverContext() {

                @Override
                public CompilationUnitTree getCompilationUnit() {
                    return rootPath.getCompilationUnit();
                }

                @Override
                public TreePath getPath(Tree tree) {
                    return TreePath.getPath(rootPath, tree);
                }

                @Override
                public boolean isSatisfied(RequirementMarker marker, @Nullable Object target) {
                    return false; // TODO CLI args
                }

            };

            element.accept(this.elementVisitor, optInResolver);
        }

        return false;
    }

    @Nullable
    private OptInMarker deriveOptInMarker(AnnotationMirror mirror, @Nullable Object binder) {
        String annotationFqName = mirror.getAnnotationType().toString();

        BiFunction<String, Object, OptInMarker> markerFactory;
        String markerClassValueName;

        if (OPT_IN_FQ_NAME.equals(annotationFqName)) {
            markerFactory = OptInMarker.JavaOptInMarker::new;
            markerClassValueName = "value";
        } else if (KOTLIN_OPT_IN_FQ_NAME.equals(annotationFqName)) {
            markerFactory = OptInMarker.KotlinOptInMarker::new;
            markerClassValueName = "markerValue";
        } else {
            return null;
        }

        Map<? extends ExecutableElement, ? extends AnnotationValue> values = elements.getElementValuesWithDefaults(mirror);

        AnnotationValue markerValue = values.entrySet().stream()
            .filter(entry -> markerClassValueName.contentEquals(entry.getKey().getSimpleName()))
            .findAny()
            .orElseThrow()
            .getValue();

        TypeMirror markerTypeMirror = ((TypeMirror) markerValue.getValue());
        return markerFactory.apply(markerTypeMirror.toString(), binder);
    }

    @Nullable
    private RequirementMarker deriveRequirementMarker(AnnotationMirror mirror) {
        DeclaredType annotationType = mirror.getAnnotationType();
        Element annotationTypeElement = annotationType.asElement();

        // TODO Kotlin support

        RequiresOptIn requiresOptIn = annotationTypeElement.getAnnotation(RequiresOptIn.class);
        if (requiresOptIn == null) return null;

        String annotationFqName = annotationType.toString();
        return new RequirementMarker.JavaRequirementMarker(annotationFqName, requiresOptIn.message(), requiresOptIn.level());
    }

    private List<AnnotationMarker> collectAnnotationMarkers(Element element, @Nullable Object binder) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        List<AnnotationMarker> markers = new ArrayList<>();

        for (AnnotationMirror annotationMirror : annotationMirrors) {
            AnnotationMarker marker = deriveOptInMarker(annotationMirror, binder);
            if (marker != null) {
                markers.add(marker);
                continue;
            }

            marker = deriveRequirementMarker(annotationMirror);
            if (marker != null) markers.add(marker);
        }

        return List.copyOf(markers);
    }

    private List<OptInMarker> collectOptInMarkers(TypeMirror mirror) {
        List<? extends AnnotationMirror> annotationMirrors = mirror.getAnnotationMirrors();
        List<OptInMarker> markers = new ArrayList<>(annotationMirrors.size() / 4);

        for (AnnotationMirror annotationMirror : annotationMirrors) {
            OptInMarker marker = deriveOptInMarker(annotationMirror, null);
            if (marker != null) markers.add(marker);
        }

        return List.copyOf(markers);
    }

    private List<RequirementMarker> collectRequirementMarkers(Element element) {
        return collectAnnotationMarkers(element, null).stream()
            .map(it -> it instanceof RequirementMarker marker ? marker : null)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<RequirementMarker> collectAllRequirementMarkers(ExecutableElement executableElement) {
        List<RequirementMarker> markers = new ArrayList<>();
        Element element = executableElement;

        do {
            List<RequirementMarker> elementMarkers = collectRequirementMarkers(element);
            markers.addAll(elementMarkers);
        } while ((element = element.getEnclosingElement()) != null);

        return List.copyOf(markers);
    }

    private List<RequirementMarker> collectAllRequirementMarkers(TypeMirror mirror) {
        if (mirror.getKind() != TypeKind.DECLARED) return List.of();

        DeclaredType type = (DeclaredType) mirror;
        Element element = type.asElement();

        List<RequirementMarker> markers = new ArrayList<>();

        do {
            List<RequirementMarker> elementMarkers = collectRequirementMarkers(element);
            markers.addAll(elementMarkers);
        } while ((element = element.getEnclosingElement()) != null);

        return List.copyOf(markers);
    }

    private void reportUnsatisfiedRequirements(
        OptInResolverContext context,
        List<RequirementMarker> requirements,
        Element target,
        @Nullable Object binder
    ) {
        List<RequirementMarker> unsatisfiedMarkers = requirements.stream()
            .filter(it -> !context.isSatisfied(it, binder))
            .toList();

        for (RequirementMarker marker : unsatisfiedMarkers) {
            Diagnostic.Kind kind = switch (marker.level()) {
                case ERROR -> Diagnostic.Kind.ERROR;
                case WARNING -> Diagnostic.Kind.WARNING;
            };

            messager.printMessage(kind, "Undeclared optionality: " + marker.fqMarkerName(), target);
        }
    }

    private void reportUnsatisfiedRequirements(
        OptInResolverContext context,
        List<RequirementMarker> requirements,
        Tree target,
        @Nullable Object binder
    ) {
        List<RequirementMarker> unsatisfiedMarkers = requirements.stream()
            .filter(it -> !context.isSatisfied(it, binder))
            .toList();

        for (RequirementMarker marker : unsatisfiedMarkers) {
            Diagnostic.Kind kind = switch (marker.level()) {
                case ERROR -> Diagnostic.Kind.ERROR;
                case WARNING -> Diagnostic.Kind.WARNING;
            };

            trees.printMessage(kind, "Undeclared optionality: " + marker.fqMarkerName(), target, context.getCompilationUnit());
        }
    }

    private final class OptInElementVisitor2 extends SimpleElementVisitor14<Void, OptInResolverContext> {

        private List<RequirementMarker> collectAllRequirementsFromOverriddenElements(ExecutableElement element) {
            Queue<TypeElement> typeElements = new ArrayDeque<>();

            Element enclosingElement = element.getEnclosingElement();
            if (!(enclosingElement instanceof TypeElement enclosingTypeElement)) return List.of();

            typeElements.add(enclosingTypeElement);

            while (!typeElements.isEmpty()) {
                TypeElement typeElement = typeElements.poll();

                for (Element enclosedElement : typeElement.getEnclosedElements()) {
                    if (enclosedElement.getKind() != ElementKind.METHOD) continue;

                    ExecutableElement executableElement = (ExecutableElement) enclosedElement;

                    if (elements.overrides(element, executableElement, enclosingTypeElement)) {
                        return collectAllRequirementMarkers(executableElement);
                    }
                }

                TypeMirror superTypeMirror = typeElement.getSuperclass();
                if (superTypeMirror.getKind() != TypeKind.NONE) {
                    Element superElement = types.asElement(superTypeMirror);
                    if (!(superElement instanceof TypeElement superTypeElement)) throw new IllegalStateException();

                    typeElements.add(superTypeElement);
                }

                for (TypeMirror interfaceMirror : typeElement.getInterfaces()) {
                    Element interfaceElement = types.asElement(interfaceMirror);
                    if (!(interfaceElement instanceof TypeElement interfaceTypeElement)) throw new IllegalStateException();

                    typeElements.add(interfaceTypeElement);
                }
            }

            return List.of();
        }

        @Override
        public Void visitExecutable(ExecutableElement element, OptInResolverContext context) {
            if (element.getKind() == ElementKind.CONSTRUCTOR) {
                Element enclosingElement = element.getEnclosingElement();
                if (enclosingElement != null && enclosingElement.getKind() == ElementKind.RECORD) return null;
            }

            List<? extends AnnotationMarker> markers = collectAnnotationMarkers(element, null);
            context = context.withMarkers(markers);

            List<RequirementMarker> requirementMarkers = collectAllRequirementsFromOverriddenElements(element);
            reportUnsatisfiedRequirements(context, requirementMarkers, element, null);

            Tree tree = trees.getTree(element);
            if (tree != null) tree.accept(treeVisitor, context);

            return null;
        }

//        @Override
//        public Void visitRecordComponent(RecordComponentElement element, OptInResolverContext context) {
//            // https://bugs.openjdk.org/browse/JDK-8295184
//
//            List<? extends AnnotationMarker> markers = collectAnnotationMarkers(element, null);
//            context = context.withMarkers(markers);
//
//            TypeMirror typeMirror = element.asType();
//            markers = collectOptInMarkers(typeMirror);
//            context = context.withMarkers(markers);
//
//            List<RequirementMarker> requirementMarkers = collectAllRequirementMarkers(typeMirror);
//            reportUnsatisfiedRequirements(context, requirementMarkers, element, null);
//
//            return null;
//        }

        @Override
        public Void visitType(TypeElement element, OptInResolverContext context) {
            if (element.getKind() == ElementKind.ANNOTATION_TYPE) {
                /*
                 * If the visited type is an annotation interface declaration, we check if the annotation is a
                 * requirement marker annotation and validate compliance with the specification if necessary.
                 *
                 * Specifically, we check
                 * 1. the annotation's retention, and
                 * 2. the annotation's targets.
                 *
                 * Additionally, while we don't have to check if the annotation is applied twice (javac does that for
                 * us), we do have to check if both, our and Kotlin's @RequiresOptIn annotation is present.
                 */
                boolean foundOsmerionMarker = false, foundKotlinMarker = false;

                for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
                    String annotationFqName = annotationMirror.getAnnotationType().toString();

                    switch (annotationFqName) {
                        case REQUIRES_OPT_IN_FQ_NAME -> foundOsmerionMarker = true;
                        case KOTLIN_REQUIRES_OPT_IN_FQ_NAME -> {
                            foundKotlinMarker = true;

                            messager.printMessage(
                                Diagnostic.Kind.WARNING,
                                """
                                Kotlin's "kotlin.RequiresOptIn" should not be used in Java code. Use "com.osmerion.optin.RequiresOptIn" instead.
                                """,
                                element,
                                annotationMirror
                            );
                        }
                        default -> {
                            /* Not a requirement marker. Moving on... */
                            continue;
                        }
                    }

                    /* 1. Require RUNTIME retention */
                    Retention retention = element.getAnnotation(Retention.class);
                    RetentionPolicy retentionPolicy = (retention != null) ? retention.value() : RetentionPolicy.CLASS;

                    if (retentionPolicy != RetentionPolicy.RUNTIME) {
                        String message = "@RequiresOptIn marker annotation must be used with RUNTIME retention";
                        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
                    }

                    /* 2. Validate targets */
                    Target target = element.getAnnotation(Target.class);
                    ElementType[] targets = (target != null) ? target.value() : DEFAULT_ANNOTATION_TARGETS;
                    List<ElementType> invalidTargets = Arrays.stream(targets)
                        .distinct()
                        .filter(it -> !SUPPORTED_REQUIREMENT_MARKER_TARGETS.contains(it))
                        .toList();

                    if (!invalidTargets.isEmpty()) {
                        String message = String.format(Locale.ROOT, "@RequiresOptIn marker annotation cannot be used on: %s", invalidTargets);
                        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
                    }

                    // TODO Spec: Should attributes be allowed in @RequiresOptIn marker annotations?
                }

                if (foundOsmerionMarker && foundKotlinMarker) {
                    messager.printMessage(Diagnostic.Kind.WARNING, "Both annotations present"); // TODO message
                }
            }

            List<? extends AnnotationMarker> markers = collectAnnotationMarkers(element, null);
            context = context.withMarkers(markers);

            ClassTree tree = trees.getTree(element);

            TypeMirror superTypeMirror = element.getSuperclass();
            if (superTypeMirror.getKind() != TypeKind.NONE) {
                Tree extendsClauseTree = tree.getExtendsClause();

                if (extendsClauseTree != null) {
                    extendsClauseTree.accept(treeVisitor, context);
                }
            }

            for (Tree interfaceTree : tree.getImplementsClause()) {
                interfaceTree.accept(treeVisitor, context);
            }

            for (Tree permittedSubclassTree : tree.getPermitsClause()) {
                permittedSubclassTree.accept(treeVisitor, context);
            }

            for (Element enclosedElement : element.getEnclosedElements()) {
                enclosedElement.accept(this, context);
            }

            return null;
        }

        @Override
        public Void visitVariable(VariableElement element, OptInResolverContext context) {
            List<? extends AnnotationMarker> markers = collectAnnotationMarkers(element, null);
            context = context.withMarkers(markers);

            // TODO document that this should never be passed to the tree
            VariableTree tree = (VariableTree) trees.getTree(element);
            Tree typeTree = tree.getType();
            TypeMirror typeMirror = element.asType();

            List<RequirementMarker> requirementMarkers = collectAllRequirementMarkers(typeMirror);
            reportUnsatisfiedRequirements(context, requirementMarkers, typeTree, null);

            Tree initializerTree = tree.getInitializer();
            if (initializerTree != null) initializerTree.accept(treeVisitor, context);

            return super.visitVariable(element, context);
        }

    }

    private final class OptInTreeVisitor2 extends TreeScanner<OptInResolverContext, OptInResolverContext> {

        @Nullable
        @Override
        public OptInResolverContext scan(@Nullable Tree tree, @Nullable OptInResolverContext context) {
            if (tree == null) return context;

            OptInResolverContext res = tree.accept(this, context);
            return res != null ? res : context;
        }

        @Nullable
        private OptInResolverContext scanAndReduce(Tree node, @Nullable OptInResolverContext p, @Nullable OptInResolverContext r) {
            return reduce(scan(node, p), r);
        }

        @Nullable
        @Override
        public OptInResolverContext scan(@Nullable Iterable<? extends Tree> nodes, OptInResolverContext context) {
            if (nodes != null) {
                boolean first = true;

                for (Tree node : nodes) {
                    context = (first ? scan(node, context) : scanAndReduce(node, context, context));
                    first = false;
                }
            }

            return context;
        }

        @Nullable
        @Override
        public OptInResolverContext reduce(@Nullable OptInResolverContext r1, @Nullable OptInResolverContext r2) {
            return (r2 == null) ? r1 : ((r1 == null) ? r2 : r1.mergedWith(r2));
        }

        @Override
        public OptInResolverContext visitAnnotatedType(AnnotatedTypeTree node, OptInResolverContext context) {
            TreePath path = context.getPath(node);
            TypeMirror typeMirror = trees.getTypeMirror(path);

            List<OptInMarker> markers = collectOptInMarkers(typeMirror);
            context = context.withMarkers(markers);

            return super.visitAnnotatedType(node, context);
        }

        @Override
        public OptInResolverContext visitBlock(BlockTree node, OptInResolverContext context) {
            super.visitBlock(node, context);
            return context;
        }

        @Override
        public OptInResolverContext visitClass(ClassTree node, OptInResolverContext context) {
            Element element = trees.getElement(context.getPath(node));
            element.accept(elementVisitor, context);

            return context;
        }

        @Override
        public OptInResolverContext visitIdentifier(IdentifierTree node, OptInResolverContext context) {
            Element element = trees.getElement(context.getPath(node));

            if (element != null) {
                TypeMirror typeMirror = element.asType();
                List<RequirementMarker> requirementMarkers = collectAllRequirementMarkers(typeMirror);
                reportUnsatisfiedRequirements(context, requirementMarkers, node, element);
            }

            return context;
        }

        @Override
        public OptInResolverContext visitMemberSelect(MemberSelectTree node, OptInResolverContext context) {
            context = super.visitMemberSelect(node, context);

            TreePath path = context.getPath(node);
            TypeMirror typeMirror = trees.getTypeMirror(path);

            // type contagiousness
            if (typeMirror != null) {
                List<RequirementMarker> requirementMarkers = typeMirror.accept(typeVisitor, null);
                reportUnsatisfiedRequirements(context, requirementMarkers, node, null); // TODO binder


            }


            // type contagiousness
//            List<RequirementMarker> requirementMarkers = collectAllRequirementMarkers(typeMirror);


//            List<? extends AnnotationMarker> markers = collectOptInMarkers(typeMirror);
//            context = context.withMarkers(markers);
//
//            List<RequirementMarker> requirementMarkers = collectAllRequirementMarkers(typeMirror);
//            reportUnsatisfiedRequirements(context, requirementMarkers, node, null);
//
//            OptInResolverContext finalContext = context;
//            List<RequirementMarker> unsatisfiedMarkers = requirementMarkers.stream()
//                .filter(it -> !finalContext.isSatisfied(it, null))
//                .toList();

            return context;
        }

        @Override
        public OptInResolverContext visitMethodInvocation(MethodInvocationTree node, OptInResolverContext context) {
            TreePath path = context.getPath(node);
            TypeMirror typeMirror = trees.getTypeMirror(path);

            // type contagiousness
            if (typeMirror != null) {
                List<RequirementMarker> requirementMarkers = typeMirror.accept(typeVisitor, null);
                reportUnsatisfiedRequirements(context, requirementMarkers, node, null);
            }

            // TODO flow typing

            return super.visitMethodInvocation(node, context);
        }

        @Override
        public OptInResolverContext visitVariable(VariableTree node, OptInResolverContext context) {
            // TODO It might be a good idea to guard against non-local variables here
            TreePath path = context.getPath(node);
            Element element = trees.getElement(context.getPath(node));

            List<AnnotationMarker> unboundMarkers = collectAnnotationMarkers(element, null);
            OptInResolverContext nestedContext = context.withMarkers(unboundMarkers);
            super.visitVariable(node, nestedContext);

            List<AnnotationMarker> markers = collectAnnotationMarkers(element, element);
            context = context.withMarkers(markers);

//            TypeMirror typeMirror = element.asType();
//            List<RequirementMarker> requirementMarkers = collectAllRequirementMarkers(typeMirror);
//
//            // This does not print properly for "var" variables
//            reportUnsatisfiedRequirements(context, requirementMarkers, node.getType(), element);

            return context;
        }

    }

    private final class OptInTypeVisitor2 extends SimpleTypeVisitor14<List<RequirementMarker>, Void> {

        @Override
        protected List<RequirementMarker> defaultAction(TypeMirror type, Void unused) {
            return List.of();
        }

        @Override
        public List<RequirementMarker> visitArray(ArrayType type, Void unused) {
            return type.getComponentType().accept(typeVisitor, null);
        }

        @Override
        public List<RequirementMarker> visitDeclared(DeclaredType type, Void unused) {
            List<RequirementMarker> requirementMarkers = new ArrayList<>(collectAllRequirementMarkers(type));

            for (TypeMirror typeArgument : type.getTypeArguments()) {
                requirementMarkers.addAll(typeArgument.accept(typeVisitor, null));
            }

            return List.copyOf(requirementMarkers);
        }

        @Override
        public List<RequirementMarker> visitIntersection(IntersectionType type, Void unused) {
            return type.getBounds().stream().flatMap(it -> it.accept(typeVisitor, null).stream()).toList();
        }

        @Override
        public List<RequirementMarker> visitUnion(UnionType type, Void unused) {
            return type.getAlternatives().stream().flatMap(it -> it.accept(typeVisitor, null).stream()).toList();
        }

    }

}