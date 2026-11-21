import { expect, test } from '@playwright/test';
import { registerUser } from '../helpers/api';

test('user can register, log in, view, and update their profile', async ({ request }) => {
  const user = await registerUser(request);
  const loginResponse = await request.post('/api/auth/login', {
    data: { username: user.username, password: user.password },
  });
  expect(loginResponse.status()).toBe(200);

  const { jwtToken } = (await loginResponse.json()) as { jwtToken: string };
  const headers = { Authorization: `Bearer ${jwtToken}` };

  const profileResponse = await request.get('/api/users/me', { headers });
  expect(profileResponse.status()).toBe(200);
  expect(await profileResponse.json()).toMatchObject({
    username: user.username,
    email: user.email,
    role: 'USER',
  });

  const updatedUsername = `updated-${user.username}`;
  const updatedEmail = `updated-${user.email}`;
  const updateResponse = await request.put('/api/users/me', {
    headers,
    data: { username: updatedUsername, email: updatedEmail },
  });
  expect(updateResponse.status()).toBe(200);
  expect(await updateResponse.json()).toMatchObject({
    username: updatedUsername,
    email: updatedEmail,
  });

  const updatedProfileResponse = await request.get('/api/users/me', { headers });
  expect(await updatedProfileResponse.json()).toMatchObject({
    username: updatedUsername,
    email: updatedEmail,
  });
});
