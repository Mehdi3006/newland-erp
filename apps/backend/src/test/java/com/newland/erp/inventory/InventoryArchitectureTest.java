package com.newland.erp.inventory;

import com.newland.erp.NewlandErpApplication;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

final class InventoryArchitectureTest {
    private static final String BASE_PACKAGE = "com.newland.erp.inventory";
    private static final JavaClasses CLASSES = new ClassFileImporter().importPackages(BASE_PACKAGE);

    @Test
    void verifiesSpringModulithBoundaries() {
        ApplicationModules.of(NewlandErpApplication.class).verify();
    }

    @Test
    void domainDoesNotDependOnApplicationApiInfrastructureOrSpring() {
        ArchRuleDefinition.noClasses()
                .that()
                .resideInAPackage(BASE_PACKAGE + ".domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(BASE_PACKAGE + ".application..", BASE_PACKAGE + ".api..",
                        BASE_PACKAGE + ".infrastructure..", "org.springframework..", "jakarta..", "org.jooq..")
                .check(CLASSES);
    }
}
