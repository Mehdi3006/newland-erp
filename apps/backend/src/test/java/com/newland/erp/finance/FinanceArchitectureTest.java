package com.newland.erp.finance;

import com.newland.erp.NewlandErpApplication;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

final class FinanceArchitectureTest {
  @Test
  void verifiesModulithBoundaries() {
    ApplicationModules.of(NewlandErpApplication.class).verify();
  }

  @Test
  void domainHasNoFrameworkOrAdapterDependency() {
    ArchRuleDefinition.noClasses()
        .that()
        .resideInAPackage("com.newland.erp.finance.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "com.newland.erp.finance.api..",
            "com.newland.erp.finance.application..",
            "com.newland.erp.finance.infrastructure..",
            "org.springframework..",
            "org.jooq..")
        .check(new ClassFileImporter().importPackages("com.newland.erp.finance"));
  }

  @Test
  void postingDomainHasNoInfrastructureDependency() {
    ArchRuleDefinition.noClasses()
        .that()
        .resideInAPackage("com.newland.erp.finance.posting.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "com.newland.erp.finance.posting.infrastructure..",
            "org.springframework..",
            "org.jooq..")
        .check(new ClassFileImporter().importPackages("com.newland.erp.finance.posting"));
  }

  @Test
  void publishedFoundationContractsDoNotDependOnAdaptersOrApi() {
    ArchRuleDefinition.noClasses()
        .that()
        .resideInAPackage("com.newland.erp.finance.application.integration..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "com.newland.erp.finance.api..",
            "com.newland.erp.finance.infrastructure..",
            "org.jooq..")
        .check(new ClassFileImporter().importPackages("com.newland.erp.finance"));
  }
}
