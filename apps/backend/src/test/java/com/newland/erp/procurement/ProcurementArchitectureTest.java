package com.newland.erp.procurement;

import com.newland.erp.NewlandErpApplication;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

final class ProcurementArchitectureTest {
  private static final String BASE_PACKAGE = "com.newland.erp.procurement";
  private static final JavaClasses CLASSES =
      new ClassFileImporter().importPackages(BASE_PACKAGE);

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
        .resideInAnyPackage(
            BASE_PACKAGE + ".application..",
            BASE_PACKAGE + ".api..",
            BASE_PACKAGE + ".infrastructure..",
            "org.springframework..",
            "jakarta..",
            "org.jooq..")
        .check(CLASSES);
  }

  @Test
  void procurementUsesOnlyThePublishedFinanceIntegrationApi() {
    ArchRuleDefinition.noClasses()
        .that()
        .resideInAPackage(BASE_PACKAGE + "..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "com.newland.erp.finance.domain..",
            "com.newland.erp.finance.infrastructure..",
            "com.newland.erp.finance.posting.domain..",
            "com.newland.erp.finance.posting.infrastructure..",
            "com.newland.erp.finance.posting.application")
        .because("Procurement may depend only on Finance's named posting-integration API")
        .check(CLASSES);
  }
}
