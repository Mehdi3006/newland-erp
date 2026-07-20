import { defineConfig } from '@playwright/test';

export default defineConfig({
  forbidOnly: Boolean(process.env.CI),
  fullyParallel: true,
  outputDir: 'test-results',
  reporter: [['html', { open: 'never', outputFolder: 'playwright-report' }]],
  retries: process.env.CI ? 2 : 0,
  testDir: 'tests/e2e',
  use: {
    channel: 'chrome',
    screenshot: 'only-on-failure',
    trace: 'on-first-retry',
  },
});
