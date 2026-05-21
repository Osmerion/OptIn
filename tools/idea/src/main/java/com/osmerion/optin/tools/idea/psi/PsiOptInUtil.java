/*
 * Copyright 2022-2026 Leon Linhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.osmerion.optin.tools.idea.psi;

import com.intellij.codeInsight.daemon.impl.analysis.JavaModuleGraphUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.osmerion.optin.tools.idea.OptInBundle;
import com.osmerion.optin.tools.idea.OptInConstants;
import com.osmerion.optin.tools.idea.markers.ConsentAnnotation;
import com.osmerion.optin.tools.idea.markers.OptInAnnotation;
import com.osmerion.optin.tools.idea.markers.RequirementAnnotation;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.psi.*;

import java.util.*;
import java.util.stream.Collectors;

public final class PsiOptInUtil {

    private static @Nullable ConsentAnnotation deriveConsentAnnotation(PsiAnnotation annotation) {
        ConsentAnnotation res = deriveOptIn(annotation);
        if (res != null) return res;

        return deriveRequirementMarker(annotation);
    }

    private static @Nullable OptInAnnotation deriveOptIn(PsiAnnotation annotation) {
        if (OptInConstants.OPT_IN_FQ_NAME.equals(annotation.getQualifiedName())) {
            return deriveOptInAnnotation(annotation, "value");
        } else if (OptInConstants.KOTLIN_OPT_IN_FQ_NAME.equals(annotation.getQualifiedName())) {
            return deriveOptInAnnotation(annotation, "markerClass");
        }

        return null;
    }

    private static @Nullable OptInAnnotation deriveOptInAnnotation(PsiAnnotation annotation, String markerAttributeName) {
        PsiAnnotationMemberValue markerValue = annotation.findAttributeValue(markerAttributeName);
        if (!(markerValue instanceof PsiClassObjectAccessExpression markerClassAccessExpression)) return null;

        PsiClass annotationClass = PsiTypesUtil.getPsiClass(markerClassAccessExpression.getOperand().getType());
        if (annotationClass == null) return null;

        String markerFqName = annotationClass.getQualifiedName();
        if (markerFqName == null) return null;

        return new OptInAnnotation(markerFqName);
    }

    private static @Nullable RequirementAnnotation deriveRequirementMarker(PsiAnnotation annotation) {
        PsiJavaCodeReferenceElement nameReferenceElement = annotation.getNameReferenceElement();
        if (nameReferenceElement == null) return null;

        PsiElement element = nameReferenceElement.resolve();

        if (element instanceof PsiClass aClass) {
            return deriveRequirementMarker(aClass);
        } else if (element instanceof KtClass ktClass) {
            PsiClass aClass = LightClassUtilsKt.toLightClass(ktClass);
            if (aClass != null) return deriveRequirementMarker(aClass);
        }

        return null;
    }

    public static @Nullable RequirementAnnotation deriveRequirementMarker(PsiClass annotationType) {
        if (!annotationType.isAnnotationType()) throw new IllegalArgumentException("Expected annotation type but got: " + annotationType);

        String markerFqName = annotationType.getQualifiedName();
        if (markerFqName == null) return null;

        PsiAnnotation annotation = annotationType.getAnnotation(OptInConstants.REQUIRES_OPT_IN_FQ_NAME);
        if (annotation == null) annotation = annotationType.getAnnotation(OptInConstants.KOTLIN_REQUIRES_OPT_IN_FQ_NAME);

        if (annotation != null) {
            PsiAnnotationMemberValue messageValue = annotation.findAttributeValue("message");

            String message = "";
            if (messageValue != null) {
                if ((messageValue instanceof PsiLiteral messageLiteral) && (messageLiteral.getValue() instanceof String m)) {
                    message = m;
                } else {
                    return null;
                }
            }

            if (message.isEmpty()) {
                message = OptInBundle.message("inspection.opt-in.missing-opt-in.name", markerFqName);
            }

            PsiAnnotationMemberValue levelValue = annotation.findAttributeValue("level");

            String levelString = "ERROR";
            if (levelValue != null) {
                if ((levelValue instanceof PsiReferenceExpression levelExpression) && (levelExpression.resolve() instanceof PsiEnumConstant levelEnumConstant)) {
                    levelString = levelEnumConstant.getName();
                } else {
                    return null;
                }
            }

            RequirementAnnotation.Level level = switch (levelString) {
                case "WARNING" -> RequirementAnnotation.Level.WARNING;
                case "ERROR" -> RequirementAnnotation.Level.ERROR;
                default -> null;
            };

            if (level == null) return null;

            return new RequirementAnnotation(markerFqName, message, level);
        }

        return null;
    }

    private static @Nullable RequirementAnnotation deriveSubtypingRequirement(PsiAnnotation annotation) {
        if (OptInConstants.SUBTYPING_REQUIRES_OPT_IN_FQ_NAME.equals(annotation.getQualifiedName())) {
            return deriveSubtypingRequirement(annotation, "value");
        } else if (OptInConstants.KOTLIN_SUBTYPING_REQUIRES_OPT_IN_FQ_NAME.equals(annotation.getQualifiedName())) {
            return deriveSubtypingRequirement(annotation, "markerClass");
        }

        return null;
    }

    private static @Nullable RequirementAnnotation deriveSubtypingRequirement(PsiAnnotation annotation, String markerAttributeName) {
        PsiAnnotationMemberValue markerValue = annotation.findAttributeValue(markerAttributeName);
        if (!(markerValue instanceof PsiClassObjectAccessExpression markerClassAccessExpression)) return null;

        PsiClass annotationClass = PsiTypesUtil.getPsiClass(markerClassAccessExpression.getOperand().getType());
        if (annotationClass == null) return null;

        return deriveRequirementMarker(annotationClass);
    }

    public static Map<String, ? extends Object> findAllConsent(PsiElement element) {
        HashMap<String, Object> consents = new HashMap<>();

        while (true) {
            if (element instanceof PsiModifierListOwner modifierListOwner) {
                Arrays.stream(modifierListOwner.getAnnotations())
                    .map(PsiOptInUtil::deriveConsentAnnotation)
                    .filter(Objects::nonNull)
                    .forEach(consentAnnotation -> consents.put(consentAnnotation.fqMarkerName(), consentAnnotation));
            }

            PsiElement parent = PsiTreeUtil.getParentOfType(element, PsiModifierListOwner.class);
            if (parent == null) break;

            element = parent;
        }

        PsiFile file = element.getContainingFile();
        if (file != null) {
            PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(file.getContainingDirectory());
            if (aPackage != null) {
                Arrays.stream(aPackage.getAnnotations())
                    .map(PsiOptInUtil::deriveConsentAnnotation)
                    .filter(Objects::nonNull)
                    .forEach(consentAnnotation -> consents.put(consentAnnotation.fqMarkerName(), consentAnnotation));
            }

            PsiJavaModule module = JavaModuleGraphUtil.findDescriptorByElement(file);
            if (module != null) {
                Arrays.stream(module.getAnnotations())
                    .map(PsiOptInUtil::deriveConsentAnnotation)
                    .filter(Objects::nonNull)
                    .forEach(consentAnnotation -> consents.put(consentAnnotation.fqMarkerName(), consentAnnotation));
            }
        }

        return Map.copyOf(consents);
    }

    public static Set<? extends RequirementAnnotation> findAllRequirements(PsiElement element) {
        Set<RequirementAnnotation> requirements = new HashSet<>();

        while (true) {
            if (element instanceof PsiModifierListOwner modifierListOwner) {
                Arrays.stream(modifierListOwner.getAnnotations())
                    .map(PsiOptInUtil::deriveRequirementMarker)
                    .filter(Objects::nonNull)
                    .forEach(requirements::add);
            }

            PsiElement parent = PsiTreeUtil.getParentOfType(element, PsiModifierListOwner.class);
            if (parent == null) break;

            element = parent;
        }

        PsiFile file = element.getContainingFile();
        if (file != null) {
            PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(file.getContainingDirectory());
            if (aPackage != null) {
                Arrays.stream(aPackage.getAnnotations())
                    .map(PsiOptInUtil::deriveRequirementMarker)
                    .filter(Objects::nonNull)
                    .forEach(requirements::add);
            }

            PsiJavaModule module = JavaModuleGraphUtil.findDescriptorByElement(file);
            if (module != null) {
                Arrays.stream(module.getAnnotations())
                    .map(PsiOptInUtil::deriveRequirementMarker)
                    .filter(Objects::nonNull)
                    .forEach(requirements::add);
            }
        }

        return Set.copyOf(requirements);
    }

    public static Set<? extends RequirementAnnotation> findSubtypingRequirements(PsiClass aClass) {
        return Arrays.stream(aClass.getAnnotations())
            .map(PsiOptInUtil::deriveSubtypingRequirement)
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
    }

    private PsiOptInUtil() {}

}
