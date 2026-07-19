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

const reservedP1Directories = ['apps', 'contracts', 'libs', 'platform'];
const reservedDocumentation = new Set(
  reservedP1Directories.map((directory) => `${directory}/README.md`),
);
const forbiddenExtensions = new Set(['.java', '.jsx', '.kt', '.kts', '.sql', '.tsx']);
const forbiddenNames = new Set([
  'Dockerfile',
  'compose.yml',
  'compose.yaml',
  'docker-compose.yml',
  'docker-compose.yaml',
  'openapi.json',
  'openapi.yaml',
  'openapi.yml',
]);

async function walk(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const paths = [];

  for (const entry of entries) {
    const path = resolve(directory, entry.name);
    if (entry.isDirectory()) {
      paths.push(...(await walk(path)));
    } else if (entry.isFile()) {
      paths.push(path);
    }
  }

  return paths;
}

export function classifyP1Violation(repositoryPath) {
  const normalizedPath = repositoryPath.replaceAll('\\', '/');
  const fileName = normalizedPath.split('/').at(-1) ?? '';
  const extension = fileName.includes('.') ? `.${fileName.split('.').at(-1)}` : '';

  if (reservedP1Directories.some((directory) => normalizedPath.startsWith(`${directory}/`))) {
    if (!reservedDocumentation.has(normalizedPath)) {
      return `reserved P1 boundary contains implementation: ${normalizedPath}`;
    }
  }

  if (forbiddenExtensions.has(extension) || forbiddenNames.has(fileName)) {
    return `runtime or business artifact is forbidden in P1: ${normalizedPath}`;
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

  for (const directory of reservedP1Directories) {
    const files = await walk(resolve(repositoryRoot, directory));
    for (const file of files) {
      const repositoryPath = relative(repositoryRoot, file);
      const violation = classifyP1Violation(repositoryPath);
      if (violation) {
        issues.push(violation);
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

  console.log('Architecture verification passed: Phase P1 boundaries are intact.');
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  await main();
}
