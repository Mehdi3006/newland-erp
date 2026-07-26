package com.newland.erp.crm;

import com.newland.erp.NewlandErpApplication;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

final class CrmArchitectureTest {
  @Test
  void verifiesModulithBoundaries() {
    ApplicationModules.of(NewlandErpApplication.class).verify();
  }

  @Test
  void domainHasNoFrameworkOrInfrastructureDependencies() {
    final var classes = new ClassFileImporter().importPackages("com.newland.erp.crm.domain");
    ArchRuleDefinition.noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..", "org.jooq..", "jakarta..",
            "com.newland.erp.crm.application..",
            "com.newland.erp.crm.infrastructure..",
            "com.newland.erp.crm.api..")
        .check(classes);
  }
}
