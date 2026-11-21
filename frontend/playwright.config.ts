import { defineConfig, devices } from '@playwright/test';
import process from 'node:process';

const baseURL = process.env.E2E_BASE_URL ?? 'https://localhost:8443';

export default defineConfig({
  globalSetup: './e2e/global-setup.ts',
  testDir: './e2e',
  workers: 1,
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['list'],
    ['junit', { outputFile: 'test-results/e2e-junit.xml' }],
    ['html', { open: 'never' }],
  ],
  use: {
    baseURL,
    ignoreHTTPSErrors: true,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], browserName: 'chromium' },
    },
  ],
});
