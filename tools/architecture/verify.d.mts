export const requiredFoundationFiles: readonly string[];

export function classifyP1Violation(repositoryPath: string): string | undefined;

export function inspectFoundation(repositoryRoot: string): Promise<string[]>;
