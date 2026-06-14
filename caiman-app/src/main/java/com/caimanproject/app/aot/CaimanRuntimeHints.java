package com.caimanproject.app.aot;

import com.caimanproject.app.property.CaimanServerPropsConfig;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.hibernate.community.dialect.SQLiteDialect;

public class CaimanRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        // Hibernate Validator uses JPATraversableResolver (JPA is on classpath) which accesses
        // private fields via reflection when validating @ConfigurationProperties records at startup.
        // Spring AOT handles binding but does not register field access needed for BV traversal.
        hints.reflection()
            .registerType(CaimanServerPropsConfig.class, MemberCategory.values())
            .registerType(CaimanServerPropsConfig.LoggingPropImpl.class, MemberCategory.values())
            .registerType(CaimanServerPropsConfig.DatabasePropImpl.class, MemberCategory.values())
            .registerType(CaimanServerPropsConfig.ProjectPropImpl.class, MemberCategory.values())
            .registerType(CaimanServerPropsConfig.ApplicationPropImpl.class, MemberCategory.values())
            .registerType(CaimanServerPropsConfig.OpenApiPropImp.class, MemberCategory.values())
            .registerType(CaimanServerPropsConfig.OpenApiGenericPropImpl.class, MemberCategory.values())
            .registerType(CaimanServerPropsConfig.OpenApiApplicationPropImpl.class, MemberCategory.values())
            .registerType(CaimanServerPropsConfig.OpenApiApplicationContactPropImpl.class, MemberCategory.values())
            .registerType(CaimanServerPropsConfig.OpenApiApplicationDocumentationPropImpl.class, MemberCategory.values());

        // hibernate-community-dialects has no native-image support. Hibernate loads the dialect
        // by class name via ClassLoaderServiceImpl.classForName() so it must be explicitly registered.
        hints.reflection().registerType(
            SQLiteDialect.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_DECLARED_METHODS
        );
    }
}
