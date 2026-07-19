import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      reportsDirectory: 'coverage',
    },
    environment: 'node',
    include: ['tools/**/*.test.ts'],
    passWithNoTests: false,
    reporters: ['default'],
  },
});
