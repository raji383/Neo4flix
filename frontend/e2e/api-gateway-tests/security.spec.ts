import { expect, test } from '@playwright/test';

test('gateway rejects missing and invalid credentials for protected APIs', async ({ request }) => {
  for (const path of [
    '/api/movies',
    '/api/ratings/me',
    '/api/watchlist',
    '/api/recommendations',
  ]) {
    const response = await request.get(path);
    expect(response.status(), `${path} without a token`).toBe(401);
  }

  const invalidTokenResponse = await request.get('/api/movies', {
    headers: { Authorization: 'Bearer invalid-token' },
  });
  expect(invalidTokenResponse.status()).toBe(401);
});
