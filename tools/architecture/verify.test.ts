import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

import { classifyP1Violation, inspectFoundation } from './verify.mjs';

describe('Phase P1 architecture verification', () => {
  it('accepts the checked-in repository foundation', async () => {
    await expect(inspectFoundation(resolve(import.meta.dirname, '../..'))).resolves.toEqual([]);
  });

  it.each([
    ['apps/api/src/Main.java', 'reserved P1 boundary contains implementation'],
    ['contracts/openapi.yaml', 'reserved P1 boundary contains implementation'],
    ['platform/compose.yml', 'reserved P1 boundary contains implementation'],
    ['libs/ui/src/page.tsx', 'reserved P1 boundary contains implementation'],
  ])('rejects %s', (path, message) => {
    expect(classifyP1Violation(path)).toContain(message);
  });

  it('allows only boundary documentation in reserved directories', () => {
    expect(classifyP1Violation('apps/README.md')).toBeUndefined();
  });
});
