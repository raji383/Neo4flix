import { expect, test } from '@playwright/test';
import { registerUser } from '../helpers/api';

test('user can request personalized recommendations', async ({ request }) => {
  const user = await registerUser(request);
  const response = await request.get('/api/recommendations', {
    headers: { Authorization: `Bearer ${user.token}` },
  });

  expect(response.status()).toBe(200);
  const recommendations = await response.json();
  expect(Array.isArray(recommendations)).toBe(true);
  expect(recommendations.length).toBeLessThanOrEqual(10);
});
