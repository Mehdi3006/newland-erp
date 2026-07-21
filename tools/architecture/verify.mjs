import { readdir, readFile } from 'node:fs/promises';
import { relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

export const requiredFoundationFiles = [
  '.dockerignore',
  '.editorconfig',
  '.gitattributes',
  '.github/CODEOWNERS',
  '.github/ISSUE_TEMPLATE/architecture-change.yml',
  '.github/ISSUE_TEMPLATE/bug-report.yml',
  '.github/ISSUE_TEMPLATE/change-request.yml',
  '.github/ISSUE_TEMPLATE/config.yml',
  '.github/pull_request_template.md',
  '.github/workflows/architecture.yml',
  '.github/workflows/ci.yml',
  '.github/workflows/osv-scanner.yml',
  '.github/workflows/security.yml',
  '.github/workflows/supply-chain.yml',
  '.gitignore',
  '.java-version',
  '.node-version',
  '.npmrc',
  '.nvmrc',
  '.prettierignore',
  '.prettierrc.json',
  'CHANGELOG.md',
  'CONTRIBUTING.md',
  'LICENSE',
  'README.md',
  'SECURITY.md',
  'build.gradle.kts',
  'docs/README.md',
  'docs/adr/0000-template.md',
  'docs/adr/0001-repository-foundation.md',
  'docs/architecture/file-catalog.md',
  'docs/architecture/foundation-dependency-graph.md',
  'docs/architecture/foundation-risks.md',
  'docs/architecture/module-onboarding.md',
  'docs/architecture/phase-p1-scope.md',
  'docs/standards/repository-governance.md',
  'eslint.config.mjs',
  'gradle/libs.versions.toml',
  'gradle/verification-metadata.xml',
  'gradle/wrapper/gradle-wrapper.properties',
  'gradlew',
  'gradlew.bat',
  'nx.json',
  'package.json',
  'playwright.config.ts',
  'pnpm-lock.yaml',
  'pnpm-workspace.yaml',
  'project.json',
  'settings.gradle.kts',
  'tsconfig.base.json',
  'tools/compliance/check-licenses.mjs',
  'vitest.config.ts',
];

const reservedDocumentation = new Set([
  'apps/README.md',
  'contracts/README.md',
  'libs/README.md',
  'platform/README.md',
]);
const reservedNonRuntimeDirectories = ['contracts', 'libs', 'platform'];
const approvedBackendFiles = new Set([
  'apps/backend/build.gradle.kts',
  'apps/backend/gradle.lockfile',
  'apps/backend/src/main/java/com/newland/erp/NewlandErpApplication.java',
  'apps/backend/src/main/java/com/newland/erp/enterprise/package-info.java',
  'apps/backend/src/main/java/com/newland/erp/identity/package-info.java',
  'apps/backend/src/main/java/com/newland/erp/masterdata/package-info.java',
  'apps/backend/src/main/java/com/newland/erp/platform/package-info.java',
  'apps/backend/src/test/java/com/newland/erp/enterprise/EnterpriseStructureArchitectureTest.java',
  'apps/backend/src/test/java/com/newland/erp/identity/IdentityArchitectureTest.java',
  'apps/backend/src/test/java/com/newland/erp/masterdata/MasterDataArchitectureTest.java',
  'apps/backend/src/test/java/com/newland/erp/platform/PlatformArchitectureTest.java',
  'apps/backend/src/main/resources/application.yml',
  'apps/backend/src/main/resources/db/migration/V1__enterprise_structure_foundation.sql',
  'apps/backend/src/main/resources/db/migration/V2__identity_access_foundation.sql',
  'apps/backend/src/main/resources/db/migration/V3__platform_foundation.sql',
  'apps/backend/src/main/resources/db/migration/V4__master_data_foundation.sql',
]);
const approvedFrontendFiles = new Set([
  'apps/web/enterprise-structure/index.html',
  'apps/web/identity-access/index.html',
]);
const approvedBackendJavaRoots = [
  'apps/backend/src/main/java/com/newland/erp/enterprise/',
  'apps/backend/src/test/java/com/newland/erp/enterprise/',
  'apps/backend/src/main/java/com/newland/erp/identity/',
  'apps/backend/src/test/java/com/newland/erp/identity/',
  'apps/backend/src/main/java/com/newland/erp/masterdata/',
  'apps/backend/src/test/java/com/newland/erp/masterdata/',
  'apps/backend/src/main/java/com/newland/erp/platform/',
  'apps/backend/src/test/java/com/newland/erp/platform/',
];
const approvedBackendResourceRoots = ['apps/backend/src/test/resources/'];
const approvedBoundedContextLayers = new Set(['api', 'application', 'domain', 'infrastructure']);
const generatedOrExternalDirectories = [
  '.gradle/',
  'build/',
  'coverage/',
  'dist/',
  'node_modules/',
  'playwright-report/',
];
const approvedEnterpriseTables = new Set([
  'enterprise',
  'legal_entity',
  'company',
  'branch',
  'warehouse',
  'warehouse_zone',
  'warehouse_location',
]);
const approvedIdentityTables = new Set([
  'iam_user',
  'iam_role',
  'iam_permission',
  'iam_user_role_assignment',
  'iam_role_permission_assignment',
  'iam_password_credential',
  'iam_session',
  'iam_refresh_token',
]);
const approvedPlatformTables = new Set([
  'platform_outbox',
  'platform_audit_log',
  'platform_background_job',
  'platform_stored_file',
  'platform_attachment',
  'platform_configuration',
  'platform_feature_flag',
  'platform_localization_message',
  'platform_error_catalog',
  'platform_domain_event_catalog',
]);
const approvedMasterDataTables = new Set(['master_data_record']);

async function walk(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const paths = [];

  for (const entry of entries) {
    const path = resolve(directory, entry.name);
    if (entry.isDirectory()) {
      const normalizedPath = relative(
        resolve(fileURLToPath(new URL('../..', import.meta.url))),
        path,
      ).replaceAll('\\', '/');
      if (isGeneratedOrExternalPath(`${normalizedPath}/`)) {
        continue;
      }
      paths.push(...(await walk(path)));
    } else if (entry.isFile()) {
      paths.push(path);
    }
  }

  return paths;
}

export function classifyRepositoryPathViolation(repositoryPath) {
  const normalizedPath = repositoryPath.replaceAll('\\', '/');

  if (isGeneratedOrExternalPath(normalizedPath)) {
    return undefined;
  }

  if (
    reservedNonRuntimeDirectories.some((directory) => normalizedPath.startsWith(`${directory}/`))
  ) {
    if (!reservedDocumentation.has(normalizedPath)) {
      return `reserved repository boundary contains unapproved implementation: ${normalizedPath}`;
    }
    return undefined;
  }

  if (normalizedPath === 'apps/README.md') {
    return undefined;
  }

  if (!normalizedPath.startsWith('apps/')) {
    return undefined;
  }

  if (normalizedPath.startsWith('apps/web/')) {
    if (approvedFrontendFiles.has(normalizedPath)) {
      return undefined;
    }
    return `unapproved frontend artifact: ${normalizedPath}`;
  }

  if (!normalizedPath.startsWith('apps/backend/')) {
    return `unapproved application boundary: ${normalizedPath}`;
  }

  if (approvedBackendFiles.has(normalizedPath)) {
    return undefined;
  }

  if (approvedBackendJavaRoots.some((root) => normalizedPath.startsWith(root))) {
    if (!normalizedPath.endsWith('.java')) {
      return `approved backend Java roots may contain only Java source: ${normalizedPath}`;
    }
    const layer = boundedContextLayer(normalizedPath);
    if (!layer) {
      return `backend bounded-context source must be inside an approved layer: ${normalizedPath}`;
    }
    return undefined;
  }

  if (approvedBackendResourceRoots.some((root) => normalizedPath.startsWith(root))) {
    return undefined;
  }

  return `unapproved backend artifact: ${normalizedPath}`;
}

function isGeneratedOrExternalPath(normalizedPath) {
  return generatedOrExternalDirectories.some(
    (directory) => normalizedPath.startsWith(directory) || normalizedPath.includes(`/${directory}`),
  );
}

export function classifyP1Violation(repositoryPath) {
  return classifyRepositoryPathViolation(repositoryPath);
}

function boundedContextName(normalizedPath) {
  const marker = '/com/newland/erp/';
  const markerIndex = normalizedPath.indexOf(marker);
  if (markerIndex === -1) {
    return undefined;
  }
  return normalizedPath.slice(markerIndex + marker.length).split('/')[0];
}

function boundedContextLayer(normalizedPath) {
  const context = boundedContextName(normalizedPath);
  if (!context) {
    return undefined;
  }
  const activeMarker = `/com/newland/erp/${context}/`;
  const markerIndex = normalizedPath.indexOf(activeMarker);
  const afterMarker = normalizedPath.slice(markerIndex + activeMarker.length);
  const layer = afterMarker.split('/')[0];
  return approvedBoundedContextLayers.has(layer) ? layer : undefined;
}

export function classifyJavaBoundaryViolation(repositoryPath, source) {
  const normalizedPath = repositoryPath.replaceAll('\\', '/');
  const isTestSource = normalizedPath.includes('/src/test/');
  if (!normalizedPath.endsWith('.java')) {
    return undefined;
  }

  const importsAnotherBoundedContext = source
    .split('\n')
    .some(
      (line) =>
        line.startsWith('import com.newland.erp.') &&
        !line.startsWith(`import com.newland.erp.${boundedContextName(normalizedPath)}.`) &&
        !line.startsWith('import com.newland.erp.NewlandErpApplication;'),
    );

  if (importsAnotherBoundedContext) {
    return `backend bounded context must not depend on another bounded context: ${normalizedPath}`;
  }

  const layer = boundedContextLayer(normalizedPath);
  const context = boundedContextName(normalizedPath);
  if (layer === 'domain') {
    if (
      source.match(
        new RegExp(
          `import (com\\.newland\\.erp\\.${context}\\.(api|application|infrastructure)|jakarta\\.|org\\.jooq\\.|org\\.springframework\\.)`,
        ),
      )
    ) {
      return `domain layer must not depend on application, API, infrastructure, or framework code: ${normalizedPath}`;
    }
  }

  if (layer === 'application') {
    if (
      source.match(
        new RegExp(
          `import (com\\.newland\\.erp\\.${context}\\.(api|infrastructure)|org\\.jooq\\.)`,
        ),
      )
    ) {
      return `application layer must not depend on API, infrastructure, or persistence code: ${normalizedPath}`;
    }
  }

  if (layer === 'api') {
    if (
      source.match(
        new RegExp(`import (com\\.newland\\.erp\\.${context}\\.infrastructure|org\\.jooq\\.)`),
      )
    ) {
      return `API layer must not depend on infrastructure or persistence code: ${normalizedPath}`;
    }
    if (!isTestSource && source.match(/(EnterpriseStructureRepository|IdentityRepository)/)) {
      return `API layer must use application services instead of repositories: ${normalizedPath}`;
    }
  }

  if (layer === 'infrastructure' && source.includes(`import com.newland.erp.${context}.api.`)) {
    return `infrastructure layer must not depend on API code: ${normalizedPath}`;
  }

  return undefined;
}

function classifySqlBoundaryViolation(repositoryPath, source) {
  const normalizedPath = repositoryPath.replaceAll('\\', '/');
  if (!normalizedPath.endsWith('.sql')) {
    return undefined;
  }

  const tablePattern =
    /\b(?:CREATE|ALTER)\s+TABLE\s+(?:IF\s+(?:NOT\s+)?EXISTS\s+)?(?:"?([a-z][a-z0-9_]*)"?)/giu;
  for (const match of source.matchAll(tablePattern)) {
    const tableName = match[1];
    if (
      !approvedEnterpriseTables.has(tableName) &&
      !approvedIdentityTables.has(tableName) &&
      !approvedPlatformTables.has(tableName) &&
      !approvedMasterDataTables.has(tableName)
    ) {
      return `ERP migrations may only define approved P3.1/P3.2/P3.2.5/P3.3 tables: ${normalizedPath} (${tableName})`;
    }
  }

  return undefined;
}

export async function inspectFoundation(repositoryRoot) {
  const issues = [];

  for (const requiredFile of requiredFoundationFiles) {
    try {
      await readFile(resolve(repositoryRoot, requiredFile));
    } catch {
      issues.push(`required foundation file is missing: ${requiredFile}`);
    }
  }

  for (const directory of ['apps', ...reservedNonRuntimeDirectories]) {
    const files = await walk(resolve(repositoryRoot, directory));
    for (const file of files) {
      const repositoryPath = relative(repositoryRoot, file);
      const pathViolation = classifyRepositoryPathViolation(repositoryPath);
      if (pathViolation) {
        issues.push(pathViolation);
        continue;
      }
      const source = await readFile(file, 'utf8');
      const sourceViolation =
        classifyJavaBoundaryViolation(repositoryPath, source) ??
        classifySqlBoundaryViolation(repositoryPath, source);
      if (sourceViolation) {
        issues.push(sourceViolation);
      }
    }
  }

  return issues.sort();
}

async function main() {
  const repositoryRoot = resolve(fileURLToPath(new URL('../..', import.meta.url)));
  const issues = await inspectFoundation(repositoryRoot);

  if (issues.length > 0) {
    console.error('Architecture verification failed:');
    for (const issue of issues) {
      console.error(`- ${issue}`);
    }
    process.exitCode = 1;
    return;
  }

  console.log(
    'Architecture verification passed: approved P3.1/P3.2/P3.2.5/P3.3 bounded-context boundaries are intact.',
  );
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  await main();
}
