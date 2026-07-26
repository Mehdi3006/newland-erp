package com.newland.erp.servicewarranty;

import com.newland.erp.NewlandErpApplication;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

final class ServiceWarrantyArchitectureTest {
  @Test
  void verifiesModulithBoundaries() {
    ApplicationModules.of(NewlandErpApplication.class).verify();
  }

  @Test
  void domainHasNoFrameworkOrInfrastructureDependencies() {
    final var classes =
        new ClassFileImporter().importPackages("com.newland.erp.servicewarranty.domain");
    ArchRuleDefinition.noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..", "org.jooq..", "jakarta..",
            "com.newland.erp.servicewarranty.application..",
            "com.newland.erp.servicewarranty.infrastructure..",
            "com.newland.erp.servicewarranty.api..")
        .check(classes);
  }
}
