export const requiredFoundationFiles: readonly string[];

export function classifyP1Violation(repositoryPath: string): string | undefined;

export function classifyRepositoryPathViolation(repositoryPath: string): string | undefined;

export function classifyJavaBoundaryViolation(
  repositoryPath: string,
  source: string,
): string | undefined;

export function inspectFoundation(repositoryRoot: string): Promise<string[]>;
