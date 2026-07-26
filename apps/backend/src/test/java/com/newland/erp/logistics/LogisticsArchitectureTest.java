package com.newland.erp.logistics;

import com.newland.erp.NewlandErpApplication;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

final class LogisticsArchitectureTest {
  @Test
  void verifiesModulithBoundaries() {
    ApplicationModules.of(NewlandErpApplication.class).verify();
  }

  @Test
  void domainHasNoFrameworkOrInfrastructureDependencies() {
    final var classes =
        new ClassFileImporter().importPackages("com.newland.erp.logistics.domain");
    ArchRuleDefinition.noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..", "org.jooq..", "jakarta..",
            "com.newland.erp.logistics.application..",
            "com.newland.erp.logistics.infrastructure..",
            "com.newland.erp.logistics.api..")
        .check(classes);
  }
}
