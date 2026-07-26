import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

import {
  classifyJavaBoundaryViolation,
  classifyRepositoryPathViolation,
  inspectFoundation,
} from './verify.mjs';

describe('Repository architecture verification', () => {
  it('accepts the checked-in repository foundation and approved P3.1 backend', async () => {
    await expect(inspectFoundation(resolve(import.meta.dirname, '../..'))).resolves.toEqual([]);
  });

  it.each([
    ['apps/api/src/Main.java', 'unapproved application boundary'],
    ['apps/web/dashboard/index.html', 'unapproved frontend artifact'],
    [
      'apps/backend/src/main/java/com/newland/erp/accounting/domain/Journal.java',
      'unapproved backend artifact',
    ],
    [
      'apps/backend/src/main/java/com/newland/erp/enterprise/Legacy.java',
      'inside an approved layer',
    ],
    ['apps/backend/src/main/resources/db/migration/V2__sales.sql', 'unapproved backend artifact'],
    ['contracts/openapi.yaml', 'reserved repository boundary contains unapproved implementation'],
    ['platform/compose.yml', 'reserved repository boundary contains unapproved implementation'],
    ['libs/ui/src/page.tsx', 'reserved repository boundary contains unapproved implementation'],
  ])('rejects %s', (path, message) => {
    expect(classifyRepositoryPathViolation(path)).toContain(message);
  });

  it.each([
    'apps/README.md',
    'apps/backend/build.gradle.kts',
    'apps/backend/src/main/java/com/newland/erp/NewlandErpApplication.java',
    'apps/backend/src/main/java/com/newland/erp/enterprise/domain/Enterprise.java',
    'apps/backend/src/main/java/com/newland/erp/enterprise/application/EnterpriseStructureService.java',
    'apps/backend/src/main/java/com/newland/erp/enterprise/api/EnterpriseStructureDtos.java',
    'apps/backend/src/main/java/com/newland/erp/enterprise/infrastructure/JooqEnterpriseStructureRepository.java',
    'apps/backend/src/main/resources/db/migration/V1__enterprise_structure_foundation.sql',
    'apps/backend/src/main/java/com/newland/erp/sales/domain/SalesOrder.java',
    'apps/backend/src/main/resources/db/migration/V8__sales_foundation.sql',
    'apps/web/enterprise-structure/index.html',
  ])('allows approved P3.1 path %s', (path) => {
    expect(classifyRepositoryPathViolation(path)).toBeUndefined();
  });

  it('rejects another bounded-context import even when enterprise imports are also present', () => {
    expect(
      classifyJavaBoundaryViolation(
        'apps/backend/src/main/java/com/newland/erp/enterprise/application/LeakyService.java',
        [
          'package com.newland.erp.enterprise.application;',
          'import com.newland.erp.enterprise.domain.Enterprise;',
          'import com.newland.erp.sales.domain.Order;',
          '',
          'final class LeakyService { }',
        ].join('\n'),
      ),
    ).toContain('must not depend on another bounded context');
  });

  it('rejects another bounded-context import from test source', () => {
    expect(
      classifyJavaBoundaryViolation(
        'apps/backend/src/test/java/com/newland/erp/finance/LeakyTest.java',
        [
          'package com.newland.erp.finance;',
          'import com.newland.erp.sales.domain.SalesOrder;',
          '',
          'final class LeakyTest { }',
        ].join('\n'),
      ),
    ).toContain('must not depend on another bounded context');
  });

  it('allows a bounded context to consume an explicit integration contract', () => {
    expect(
      classifyJavaBoundaryViolation(
        'apps/backend/src/main/java/com/newland/erp/finance/infrastructure/ReferenceAdapter.java',
        [
          'package com.newland.erp.finance.infrastructure;',
          'import com.newland.erp.enterprise.application.integration.EnterpriseReferencePort;',
          '',
          'final class ReferenceAdapter { }',
        ].join('\n'),
      ),
    ).toBeUndefined();
  });
});
