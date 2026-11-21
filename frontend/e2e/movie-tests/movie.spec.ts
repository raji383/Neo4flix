import { expect, test } from '@playwright/test';
import { registerUser } from '../helpers/api';

test('user can browse movies and only admins can mutate them', async ({ request }) => {
  const user = await registerUser(request);
  const headers = { Authorization: `Bearer ${user.token}` };

  const moviesResponse = await request.get('/api/movies?size=2', { headers });
  expect(moviesResponse.status()).toBe(200);
  const moviesPage = (await moviesResponse.json()) as {
    content: Array<{ id: string }>;
    page: number;
    size: number;
  };
  expect(Array.isArray(moviesPage.content)).toBe(true);
  expect(moviesPage.page).toBe(0);
  expect(moviesPage.size).toBe(2);

  const popularResponse = await request.get('/api/movies/popular', { headers });
  expect(popularResponse.status()).toBe(200);
  expect(Array.isArray(await popularResponse.json())).toBe(true);

  const genresResponse = await request.get('/api/genres', { headers });
  expect(genresResponse.status()).toBe(200);
  expect(Array.isArray(await genresResponse.json())).toBe(true);

  const firstMovie = moviesPage.content[0];
  if (firstMovie) {
    const detailResponse = await request.get(`/api/movies/${encodeURIComponent(firstMovie.id)}`, {
      headers,
    });
    expect(detailResponse.status()).toBe(200);
    expect((await detailResponse.json()).id).toBe(firstMovie.id);
  }

  const movieForm = {
    title: 'E2E protected movie',
    director: 'E2E director',
    description: 'Movie mutation must require an administrator.',
    releaseYear: '2024',
    genres: 'Drama',
    file: {
      name: 'poster.png',
      mimeType: 'image/png',
      buffer: Buffer.from('not-uploaded'),
    },
  };

  const createResponse = await request.post('/api/movies/add', {
    headers,
    multipart: movieForm,
  });
  expect(createResponse.status()).toBe(403);

  const updateResponse = await request.put('/api/movies/movie-id-that-does-not-exist', {
    headers,
    multipart: movieForm,
  });
  expect(updateResponse.status()).toBe(403);

  const deleteResponse = await request.delete('/api/movies/movie-id-that-does-not-exist', {
    headers,
  });
  expect(deleteResponse.status()).toBe(403);
});
