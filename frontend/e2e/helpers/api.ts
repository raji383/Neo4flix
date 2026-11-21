import { randomUUID } from 'node:crypto';
import process from 'node:process';
import type { APIRequestContext } from '@playwright/test';

export interface TestUser {
  username: string;
  email: string;
  password: string;
  token: string;
}

export async function registerUser(request: APIRequestContext): Promise<TestUser> {
  const username = `e2e${randomUUID().replaceAll('-', '').slice(0, 12)}`;
  const email = `${username}@example.com`;
  const password = 'E2ePass123!';
  const response = await request.post('/api/auth/register', {
    data: {
      username,
      email,
      password,
      confirmPassword: password,
    },
  });

  if (response.status() !== 201) {
    throw new Error(
      `E2E user registration failed (${response.status()}): ${await response.text()}`,
    );
  }

  const body = (await response.json()) as { jwtToken?: string };
  if (!body.jwtToken) {
    throw new Error('E2E user registration returned no JWT token');
  }

  return { username, email, password, token: body.jwtToken };
}

export async function getCatalogMovieId(
  request: APIRequestContext,
  token: string,
): Promise<string | undefined> {
  const configuredMovieId = process.env.E2E_MOVIE_ID;
  if (configuredMovieId) {
    return configuredMovieId;
  }

  const response = await request.get('/api/movies?size=1', {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok()) {
    throw new Error(
      `Could not find an E2E catalog movie (${response.status()}): ${await response.text()}`,
    );
  }

  const page = (await response.json()) as {
    content?: Array<{ id?: string }>;
  };
  return page.content?.[0]?.id;
}
