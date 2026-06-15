package com.caimanproject.mapstruct.spi;

import java.util.Collection;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.mapstruct.ap.spi.BuilderInfo;
import org.mapstruct.ap.spi.DefaultBuilderProvider;
import org.mapstruct.ap.spi.MoreThanOneBuilderCreationMethodException;

public class CaimanBuilderProvider extends DefaultBuilderProvider {

    private static final String RESTORE_BUILDER = "restoreBuilder";

    @Override
    public BuilderInfo findBuilderInfo(TypeMirror type) {
        try {
            return super.findBuilderInfo(type);
        } catch (MoreThanOneBuilderCreationMethodException e) {
            return findRestoreBuilderInfo(type);
        }
    }

    private BuilderInfo findRestoreBuilderInfo(TypeMirror type) {
        TypeElement typeElement = getTypeElement(type);
        if (typeElement == null) {
            return null;
        }

        ExecutableElement creationMethod = typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .filter(e -> e.getModifiers().contains(Modifier.STATIC))
                .filter(e -> e.getModifiers().contains(Modifier.PUBLIC))
                .filter(e -> e.getParameters().isEmpty())
                .filter(e -> e.getSimpleName().contentEquals(RESTORE_BUILDER))
                .findFirst()
                .orElse(null);

        if (creationMethod == null) {
            return null;
        }

        TypeElement builderElement = getTypeElement(creationMethod.getReturnType());
        if (builderElement == null) {
            return null;
        }

        Collection<ExecutableElement> buildMethods = findBuildMethods(builderElement, typeElement);
        if (buildMethods.isEmpty()) {
            return null;
        }

        return new BuilderInfo.Builder()
                .builderCreationMethod(creationMethod)
                .buildMethod(buildMethods)
                .build();
    }
}
