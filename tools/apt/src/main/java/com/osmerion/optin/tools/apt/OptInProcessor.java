package com.osmerion.optin.tools.apt;

import com.osmerion.optin.OptIn;
import com.osmerion.optin.RequiresOptIn;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.AbstractTypeVisitor14;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.tools.Diagnostic;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

/**
 * TODO doc
 *
 * @author  Leon Linhart
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class OptInProcessor extends AbstractProcessor {

    private Elements elements;
    private Messager messager;
    private Trees trees;

    private OptInElementVisitor elementVisitor;
    private OptInTreeVisitor treeVisitor;
    private OptInTypeVisitor typeVisitor;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.messager = processingEnv.getMessager();
        this.trees = Trees.instance(processingEnv);

        this.elementVisitor = new OptInElementVisitor();
        this.treeVisitor = new OptInTreeVisitor();
        this.typeVisitor = new OptInTypeVisitor();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getRootElements()) {
            TreePath rootPath = trees.getPath(element);

            OptInResolverContext optInResolver = new OptInResolverContext() {

                @Override
                public TreePath getPath(Tree tree) {
                    return TreePath.getPath(rootPath, tree);
                }

                @Override
                public boolean isAccepted(String fqName, Element target) {
                    // TODO respect options
                    return false;
                }

            };

            element.accept(this.elementVisitor, optInResolver);
        }

        return false;
    }

    /**
     * TODO doc
     *
     * @param element
     * @return
     */
    private List<AcceptedOptionality> collectAcceptedOptionalities(Element element) {
        return collectAcceptedOptionalities(element.getAnnotationMirrors(), null);
    }

    /**
     * TODO doc
     *
     * @param element
     * @return
     */
    private List<AcceptedOptionality> collectAndBindAcceptedOptionalities(Element element) {
        return collectAcceptedOptionalities(element.getAnnotationMirrors(), element);
    }

    /**
     * TODO doc
     *
     * @param typeMirror
     *
     * @return
     */
    private List<AcceptedOptionality> collectAcceptedOptionalities(TypeMirror typeMirror) {
        return collectAcceptedOptionalities(typeMirror.getAnnotationMirrors(), null);
    }

    /**
     * TODO doc
     *
     * @param annotationMirrors
     * @return
     */
    private List<AcceptedOptionality> collectAcceptedOptionalities(List<? extends AnnotationMirror> annotationMirrors, Element binder) {
        List<AcceptedOptionality> optionalities = new ArrayList<>(annotationMirrors.size() / 4);

        for (AnnotationMirror annotationMirror : annotationMirrors) {
            AcceptedOptionality optionality = resolveAcceptedOptionalityFromOptIn(annotationMirror, binder);
            if (optionality != null) {
                optionalities.add(optionality);
                continue;
            }

            Element annotationDeclaration = annotationMirror.getAnnotationType().asElement();
            optionality = resolveAcceptedOptionalityFromMarker(annotationDeclaration, binder);
            if (optionality != null) optionalities.add(optionality);
        }

        return optionalities;
    }

    /**
     * TODO doc
     *
     * @param element
     *
     * @return
     */
    private List<RequiredOptionality> collectRequiredOptionalities(Element element) {
        List<RequiredOptionality> optionalities = new ArrayList<>();

        do {
            for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
                Element annotationDeclaration = annotationMirror.getAnnotationType().asElement();
                RequiredOptionality optionality = resolvedRequiredOptionalityFromMarker(annotationDeclaration);
                if (optionality != null) optionalities.add(optionality);
            }

            element = element.getEnclosingElement();
        } while (element != null);

        return optionalities;
    }

    /**
     * Resolves and {@return the {@link AcceptedOptionality} from the given {@code element}}.
     *
     * <p>This method does not verify that the given {@code element} is a proper
     * opt-in marker.</p>
     *
     * @param element   the element which might represent an opt-in marker
     */
    private AcceptedOptionality resolveAcceptedOptionalityFromMarker(Element element, Element binder) {
        RequiresOptIn requiresOptIn = element.getAnnotation(RequiresOptIn.class);
        if (requiresOptIn == null) return null;

        String fqName = element.asType().toString();
        return new AcceptedOptionality(fqName, binder);
    }

    /**
     * Resolves and {@return the {@link AcceptedOptionality} from the given {@code element}}.
     *
     * <p>If the given {@code mirror} represents an {@link OptIn} annotation,
     * an additional check is performed to validate that the passed value is a
     * valid opt-in marker.</p>
     *
     * @param mirror    the mirror which might represent an {@link OptIn} annotation
     */
    private AcceptedOptionality resolveAcceptedOptionalityFromOptIn(AnnotationMirror mirror, Element binder) {
        String annotationFqName = mirror.getAnnotationType().toString();
        if (!"com.osmerion.optin.OptIn".equals(annotationFqName)) return null;

        Map<? extends ExecutableElement, ? extends AnnotationValue> values = elements.getElementValuesWithDefaults(mirror);

        AnnotationValue markerValue = values.entrySet().stream()
            .filter(entry -> "value".contentEquals(entry.getKey().getSimpleName()))
            .findAny()
            .orElseThrow()
            .getValue();

        TypeMirror markerTypeMirror = ((TypeMirror) markerValue.getValue());
        verifyMarker(markerTypeMirror);

        return new AcceptedOptionality(markerTypeMirror.toString(), binder);
    }

    /**
     * Resolves and {@return the {@link RequiredOptionality} from the given {@code element}}.
     *
     * <p>This method does not verify that the given {@code element} is a proper
     * opt-in marker.</p>
     *
     * @param element   the element which might represent an opt-in marker
     */
    private RequiredOptionality resolvedRequiredOptionalityFromMarker(Element element) {
        RequiresOptIn requiresOptIn = element.getAnnotation(RequiresOptIn.class);
        if (requiresOptIn == null) return null;

        String fqName = element.asType().toString();
        return new RequiredOptionality(fqName, requiresOptIn.message(), requiresOptIn.level());
    }

    private void reportNotAcceptedOptionalities(
        OptInResolverContext resolver,
        List<RequiredOptionality> requiredOptionalities,
        Element owner
    ) {
        reportNotAcceptedOptionalities(resolver, requiredOptionalities, owner, null, null);
    }

    private void reportNotAcceptedOptionalities(
        OptInResolverContext resolver,
        List<RequiredOptionality> requiredOptionalities,
        Tree owner,
        Element target
    ) {
        // TODO Spec: What to do if an annotation has an opt-in marker annotation and @OptIn?
        List<RequiredOptionality> notAcceptedOptionalities = requiredOptionalities.stream()
            .filter(it -> !resolver.isAccepted(it.fqMarkerName(), target))
            .toList();

        for (RequiredOptionality undeclaredOptionality : notAcceptedOptionalities) {
            Diagnostic.Kind kind = switch (undeclaredOptionality.level()) {
                case ERROR -> Diagnostic.Kind.ERROR;
                case WARNING -> Diagnostic.Kind.WARNING;
            };

            // TODO respect the custom message (if set)
            trees.printMessage(kind,"Undeclared optionality: " + undeclaredOptionality.fqMarkerName(), owner, resolver.getPath(owner).getCompilationUnit());
        }
    }

    private void reportNotAcceptedOptionalities(
        OptInResolverContext resolver,
        List<RequiredOptionality> requiredOptionalities,
        Element owner,
        AnnotationMirror annotation
    ) {
        reportNotAcceptedOptionalities(resolver, requiredOptionalities, owner, annotation, null);
    }

    private void reportNotAcceptedOptionalities(
        OptInResolverContext resolver,
        List<RequiredOptionality> requiredOptionalities,
        Element owner,
        AnnotationMirror annotation,
        AnnotationValue value
    ) {
       // TODO Spec: What to do if an annotation has an opt-in marker annotation and @OptIn?
        List<RequiredOptionality> notAcceptedOptionalities = requiredOptionalities.stream()
            .filter(it -> !resolver.isAccepted(it.fqMarkerName(), null))
            .toList();

        for (RequiredOptionality undeclaredOptionality : notAcceptedOptionalities) {
            Diagnostic.Kind kind = switch (undeclaredOptionality.level()) {
                case ERROR -> Diagnostic.Kind.ERROR;
                case WARNING -> Diagnostic.Kind.WARNING;
            };

            // TODO respect the custom message (if set)
            messager.printMessage(kind, "Undeclared optionality: " + undeclaredOptionality.fqMarkerName(), owner, annotation, value);
        }
    }















    private static void verifyMarker(TypeMirror markerTypeMirror) {
        boolean isOptInMarker = markerTypeMirror.getAnnotationMirrors().stream().anyMatch(annotationMirror -> {
            DeclaredType annotationType = annotationMirror.getAnnotationType();
            String fqName = annotationType.toString();

            return "com.osmerion.optin.RequiresOptIn".equals(fqName);
        });

        if (!isOptInMarker) {
//            messager.printMessage(Diagnostic.Kind.ERROR, "", element, mirror, markerValue);
        }
    }

    private final class OptInElementVisitor extends SimpleElementVisitor14<Void, OptInResolverContext> {

//        @Override
//        protected Void defaultAction(Element e, OptInResolverContext o) {
//            List<AcceptedOptionality> optionalities = new ArrayList<>();
//
//            for (AnnotationMirror mirror : e.getAnnotationMirrors()) {
//                DeclaredType annotationType = mirror.getAnnotationType();
//
//                AcceptedOptionality optionality = resolveAcceptedOptionalityFromOptIn(mirror);
//                if (optionality != null) {
//                    optionalities.add(optionality);
//                    continue;
//                }
//
//                optionality = resolveAcceptedOptionalityFromMarker(annotationType.asElement());
//                if (optionality != null) optionalities.add(optionality);
//            }
//
//            optionalities = List.copyOf(optionalities);
////            o = o.withOptionalities(optionalities);
//
//            for (Element enclosedElement : e.getEnclosedElements()) {
//                enclosedElement.accept(this, o);
//            }
//
//            return null;
//        }

        @Override
        public Void visitExecutable(ExecutableElement element, OptInResolverContext resolver) {
            List<AcceptedOptionality> acceptedOptionalities = collectAcceptedOptionalities(element);
            resolver = resolver.withAcceptedOptionalities(acceptedOptionalities);

            for (VariableElement parameterElement : element.getParameters()) {
                acceptedOptionalities = collectAndBindAcceptedOptionalities(parameterElement);
                resolver = resolver.withAcceptedOptionalities(acceptedOptionalities);

                // TODO Support scoping type use annotation optionalities to specific bindings
                Element parameterTypeElement = processingEnv.getTypeUtils().asElement(parameterElement.asType());

                List<RequiredOptionality> requiredOptionalities = collectRequiredOptionalities(parameterTypeElement);
                reportNotAcceptedOptionalities(resolver, requiredOptionalities, parameterElement);
            }

            MethodTree methodTree = trees.getTree(element);
            if (methodTree != null) methodTree.accept(treeVisitor, resolver);

            element.asType().accept(typeVisitor, resolver);

            return null;
        }

        @Override
        public Void visitRecordComponent(RecordComponentElement element, OptInResolverContext resolver) {
            List<AcceptedOptionality> acceptedOptionalities = collectAcceptedOptionalities(element);
            resolver = resolver.withAcceptedOptionalities(acceptedOptionalities);

            TypeMirror typeMirror = element.asType();
            Element componentTypeElement = processingEnv.getTypeUtils().asElement(typeMirror);

            List<RequiredOptionality> declaredOptionalities = collectRequiredOptionalities(componentTypeElement);
            reportNotAcceptedOptionalities(resolver, declaredOptionalities, element);

            return null;
        }

        @Override
        public Void visitType(TypeElement element, OptInResolverContext resolver) {
            // Verify opt-in marker annotations
            if (element.getKind() == ElementKind.ANNOTATION_TYPE && element.getAnnotation(RequiresOptIn.class) != null) {
                /* Prohibit SOURCE retention */
                Retention retention = element.getAnnotation(Retention.class);
                RetentionPolicy retentionPolicy = (retention != null) ? retention.value() : RetentionPolicy.CLASS;

                if (retentionPolicy == RetentionPolicy.SOURCE) {
                    String message = "@RequiresOptIn marker annotation cannot be used with SOURCE retention";
                    messager.printMessage(Diagnostic.Kind.ERROR, message, element);
                }

                /* Validate supported targets */
                EnumSet<ElementType> validTargets = EnumSet.of(
                    ElementType.ANNOTATION_TYPE,
                    ElementType.CONSTRUCTOR,
                    ElementType.FIELD,
                    ElementType.METHOD,
                    ElementType.MODULE,
                    ElementType.PACKAGE,
                    ElementType.TYPE
                );

                Target target = element.getAnnotation(Target.class);

                if (target != null) {
                    ElementType[] targets = target.value();

                    List<ElementType> invalidTargets = Arrays.stream(targets)
                        .distinct()
                        .filter(it -> !validTargets.contains(it))
                        .toList();

                    if (!invalidTargets.isEmpty()) {
                        String message = String.format(Locale.ROOT, "@RequiresOptIn marker annotation cannot be used on: %s", invalidTargets);
                        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
                    }
                }

                // TODO Spec: Should attributes be allowed in @RequiresOptIn marker annotations?
            }

            List<AcceptedOptionality> optionalities = collectAcceptedOptionalities(element);
            resolver = resolver.withAcceptedOptionalities(optionalities);

            /*
             * Verify super-types and permitted subclasses
             */
            TypeMirror superClassMirror = element.getSuperclass();
            verifySuperTypeOrPermittedSubclass(element, superClassMirror, resolver);

            for (TypeMirror interfaceMirror : element.getInterfaces()) {
                verifySuperTypeOrPermittedSubclass(element, interfaceMirror, resolver);
            }

            for (TypeMirror permittedSubclassMirror : element.getPermittedSubclasses()) {
                verifySuperTypeOrPermittedSubclass(element, permittedSubclassMirror, resolver);
            }

            // TODO type parameters
            element.asType().accept(typeVisitor, resolver);


            for (Element enclosedElement : element.getEnclosedElements()) {
                enclosedElement.accept(this, resolver);
            }

            return null;
        }

        private void verifySuperTypeOrPermittedSubclass(Element owner, TypeMirror typeMirror, OptInResolverContext resolver) {
            List<AcceptedOptionality> acceptedOptionalities = collectAcceptedOptionalities(typeMirror);
            resolver = resolver.withAcceptedOptionalities(acceptedOptionalities);

            processingEnv.getTypeUtils().asElement(typeMirror);

            List<RequiredOptionality> declaredOptionalities = collectRequiredOptionalities(owner);

            // TODO
        }

    }

    private final class OptInTreeVisitor extends TreeScanner<Void, OptInResolverContext> {

        @Override
        public Void visitIdentifier(IdentifierTree node, OptInResolverContext resolver) {
            TreePath path = resolver.getPath(node);
            Element element = trees.getElement(path);

            List<RequiredOptionality> requiredOptionalities = collectRequiredOptionalities(element);
            reportNotAcceptedOptionalities(resolver, requiredOptionalities, node, element);

            return super.visitIdentifier(node, resolver);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node, OptInResolverContext resolver) {
            TreePath path = resolver.getPath(node);
            Element element = trees.getElement(path);

            List<RequiredOptionality> requiredOptionalities = collectRequiredOptionalities(element);
            reportNotAcceptedOptionalities(resolver, requiredOptionalities, node, element);

            return super.visitMemberSelect(node, resolver);
        }

        @Override
        public Void visitMethod(MethodTree node, OptInResolverContext resolver) {
            scan(node.getModifiers(), resolver);
            scan(node.getReturnType(), resolver);
            scan(node.getTypeParameters(), resolver);
//            scan(node.getParameters(), resolver);
            scan(node.getReceiverParameter(), resolver);
            scan(node.getThrows(), resolver);
            scan(node.getBody(), resolver);
            scan(node.getDefaultValue(), resolver);

            return null;
        }

    }

    private final class OptInTypeVisitor extends AbstractTypeVisitor14<Void, OptInResolverContext> {

        @Override
        public Void visitIntersection(IntersectionType t, OptInResolverContext resolverContext) {
            return null;
        }

        @Override
        public Void visitUnion(UnionType t, OptInResolverContext resolverContext) {
            return null;
        }

        @Override
        public Void visitPrimitive(PrimitiveType t, OptInResolverContext resolverContext) {
            return null;
        }

        @Override
        public Void visitNull(NullType t, OptInResolverContext resolverContext) {
            return null;
        }

        @Override
        public Void visitArray(ArrayType t, OptInResolverContext resolverContext) {


            return null;
        }

        @Override
        public Void visitDeclared(DeclaredType t, OptInResolverContext resolverContext) {
            return null;
        }

        @Override
        public Void visitError(ErrorType t, OptInResolverContext resolverContext) {
            return null;
        }

        @Override
        public Void visitTypeVariable(TypeVariable t, OptInResolverContext resolverContext) {
            return null;
        }

        @Override
        public Void visitWildcard(WildcardType t, OptInResolverContext resolverContext) {
            return null;
        }

        @Override
        public Void visitExecutable(ExecutableType t, OptInResolverContext resolver) {
            t.getReturnType().accept(this, resolver);

            return null;
        }

        @Override
        public Void visitNoType(NoType t, OptInResolverContext resolverContext) {
            return null;
        }
    }

}