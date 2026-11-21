import { request } from '@playwright/test';
import process from 'node:process';

export default async function globalSetup() {
  const baseURL = process.env.E2E_BASE_URL ?? 'https://localhost:8443';
  const timeoutMs = Number(process.env.E2E_STARTUP_TIMEOUT_MS ?? 60_000);
  const deadline = Date.now() + timeoutMs;
  const api = await request.newContext({
    baseURL,
    ignoreHTTPSErrors: true,
    timeout: 5_000,
  });
  let lastStatus = 'no response';

  try {
    while (Date.now() <= deadline) {
      try {
        const response = await api.get('/actuator/health');
        const body = (await response.json()) as { status?: string };
        lastStatus = `${response.status()} ${body.status ?? 'unknown'}`;
        if (response.ok() && body.status === 'UP') {
          console.log(`Ready: API gateway health is UP at ${baseURL}`);
          return;
        }
      } catch (error) {
        lastStatus = String(error);
      }

      await new Promise((resolve) => setTimeout(resolve, 1_000));
    }

    throw new Error(
      `API gateway did not become healthy within ${timeoutMs}ms at ${baseURL}: ${lastStatus}`,
    );
  } finally {
    await api.dispose();
  }
}
