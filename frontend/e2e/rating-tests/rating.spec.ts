import { expect, test } from '@playwright/test';
import { getCatalogMovieId, registerUser } from '../helpers/api';

test('user can rate a movie and manage their watchlist', async ({ request }) => {
  const user = await registerUser(request);
  const headers = { Authorization: `Bearer ${user.token}` };
  const movieId = await getCatalogMovieId(request, user.token);

  if (!movieId) {
    test.skip(true, 'Requires a catalog movie; seed movies or set E2E_MOVIE_ID.');
    return;
  }

  const ratingUrl = `/api/ratings/${encodeURIComponent(movieId)}`;
  const createRatingResponse = await request.post(ratingUrl, {
    headers,
    data: { value: 4 },
  });
  expect(createRatingResponse.status()).toBe(201);

  const ratingResponse = await request.get(ratingUrl, { headers });
  expect(ratingResponse.status()).toBe(200);
  expect(await ratingResponse.json()).toBe(4);

  const updateRatingResponse = await request.put(ratingUrl, {
    headers,
    data: { value: 5 },
  });
  expect(updateRatingResponse.status()).toBe(200);

  const ratedMoviesResponse = await request.get('/api/ratings/me', { headers });
  expect(ratedMoviesResponse.status()).toBe(200);
  expect(
    (await ratedMoviesResponse.json()).some(
      (movie: { id: string; myRating: number }) =>
        movie.id === movieId && movie.myRating === 5,
    ),
  ).toBe(true);

  const deleteRatingResponse = await request.delete(ratingUrl, { headers });
  expect(deleteRatingResponse.status()).toBe(204);

  const watchlistUrl = `/api/watchlist/${encodeURIComponent(movieId)}`;
  const addWatchlistResponse = await request.post(watchlistUrl, { headers });
  expect(addWatchlistResponse.status()).toBe(201);

  const containsResponse = await request.get(watchlistUrl, { headers });
  expect(containsResponse.status()).toBe(200);
  expect(await containsResponse.json()).toEqual({ inWatchlist: true });

  const watchlistResponse = await request.get('/api/watchlist', { headers });
  expect(watchlistResponse.status()).toBe(200);
  expect((await watchlistResponse.json()).some((movie: { id: string }) => movie.id === movieId))
    .toBe(true);

  const removeWatchlistResponse = await request.delete(watchlistUrl, { headers });
  expect(removeWatchlistResponse.status()).toBe(204);

  const removedContainsResponse = await request.get(watchlistUrl, { headers });
  expect(await removedContainsResponse.json()).toEqual({ inWatchlist: false });
});
