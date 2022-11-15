/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.idea;

import com.intellij.codeInsight.daemon.impl.analysis.JavaModuleGraphUtil;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.lang.jvm.annotation.JvmAnnotationClassValue;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OptInAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        PsiReference reference = element.getReference();
        if (reference == null) return;

        List<String> allowedMarkers = new ArrayList<>();

        PsiElement parent = element.getParent();

        while (parent != null) {
            if (parent instanceof PsiModifierListOwner modifierListOwner) {
                for (PsiAnnotation annotation : modifierListOwner.getAnnotations()) {
                    if ("com.osmerion.optin.OptIn".equals(annotation.getQualifiedName())) {
                        List<JvmAnnotationAttribute> attributes = annotation.getAttributes();

                        if (!attributes.isEmpty()) {
                            for (JvmAnnotationAttribute attribute : attributes) {
                                if ("value".equals(attribute.getAttributeName())) {
                                    allowedMarkers.add(((JvmAnnotationClassValue) attribute).getQualifiedName());
                                    break;
                                }
                            }
                        }
                    } else {
                        PsiClass annotationType = annotation.resolveAnnotationType();
                        if (annotationType == null) continue;

                        PsiAnnotation requiresOptInAnnotation = annotationType.getAnnotation("com.osmerion.optin.RequiresOptIn");
                        if (requiresOptInAnnotation != null) {
                            allowedMarkers.add(annotationType.getQualifiedName());
                        }
                    }
                }
            }

            parent = parent.getParent();
        }

        PsiJavaModule module = JavaModuleGraphUtil.findDescriptorByElement(element);
        if (module != null) {
            for (PsiAnnotation annotation : module.getAnnotations()) {
                if ("com.osmerion.optin.OptIn".equals(annotation.getQualifiedName())) {
                    List<JvmAnnotationAttribute> attributes = annotation.getAttributes();

                    if (!attributes.isEmpty()) {
                        for (JvmAnnotationAttribute attribute : attributes) {
                            if ("value".equals(attribute.getAttributeName())) {
                                allowedMarkers.add(((JvmAnnotationClassValue) attribute).getQualifiedName());
                                break;
                            }
                        }
                    }
                } else {
                    PsiClass annotationType = annotation.resolveAnnotationType();
                    if (annotationType == null) continue;

                    PsiAnnotation requiresOptInAnnotation = annotationType.getAnnotation("com.osmerion.optin.RequiresOptIn");
                    if (requiresOptInAnnotation != null) {
                        allowedMarkers.add(annotationType.getQualifiedName());
                    }
                }
            }
        }

        PsiElement referencedElement = reference.resolve();
        if (referencedElement == null) return;

        if (referencedElement instanceof PsiModifierListOwner modifierListOwner) {
            PsiAnnotation[] annotations = modifierListOwner.getAnnotations();

            for (PsiAnnotation annotation : annotations) {
                PsiClass annotationType = annotation.resolveAnnotationType();
                PsiAnnotation optInAnnotation = annotationType.getAnnotation("com.osmerion.optin.RequiresOptIn");

                if (optInAnnotation != null && !allowedMarkers.contains(annotation.getQualifiedName())) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "This declaration is experimental and its usage should be marked with '" + annotationType.getName() + "' or '@OptIn(" + annotationType.getName() + ".class)'")
                        .range(element)
                        .create();
                }
            }
        }
    }

}