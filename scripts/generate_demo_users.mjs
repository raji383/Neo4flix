#!/usr/bin/env node

import http from "node:http";
import https from "node:https";

const DEFAULTS = {
  api: "https://localhost:8443",
  count: 100,
  seed: 42,
  prefix: "demo-user",
  minRatings: 8,
  maxRatings: 40,
};

function usage() {
  console.log(`Generate synthetic users and ratings through the Neo4flix API Gateway.

Usage:
  SEED_USER_PASSWORD='Demo-only-Change-123!' node scripts/generate_demo_users.mjs [options]

Options:
  --api URL               API Gateway base URL (default: ${DEFAULTS.api})
  --count N               Number of users (default: ${DEFAULTS.count})
  --seed N                Random seed for reproducible profiles and ratings (default: ${DEFAULTS.seed})
  --prefix NAME           Username/email prefix (default: ${DEFAULTS.prefix})
  --min-ratings N         Minimum movies rated per user (default: ${DEFAULTS.minRatings})
  --max-ratings N         Maximum movies rated per user (default: ${DEFAULTS.maxRatings})
  --allow-self-signed     Accept the local API Gateway's self-signed TLS certificate
  --help                  Show this help
`);
}

function parseArgs(args) {
  const options = { ...DEFAULTS, allowSelfSigned: false };
  const flags = new Map([
    ["--api", "api"],
    ["--count", "count"],
    ["--seed", "seed"],
    ["--prefix", "prefix"],
    ["--min-ratings", "minRatings"],
    ["--max-ratings", "maxRatings"],
  ]);

  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index];
    if (arg === "--help") {
      options.help = true;
      continue;
    }
    if (arg === "--allow-self-signed") {
      options.allowSelfSigned = true;
      continue;
    }
    const key = flags.get(arg);
    if (!key || args[index + 1] === undefined || args[index + 1].startsWith("--")) {
      throw new Error(`Unknown option or missing value: ${arg}`);
    }
    const value = args[++index];
    options[key] = ["count", "seed", "minRatings", "maxRatings"].includes(key)
      ? Number(value)
      : value;
  }

  if (options.help) return options;
  if (!Number.isSafeInteger(options.count) || options.count < 1) {
    throw new Error("--count must be a positive integer");
  }
  if (!Number.isSafeInteger(options.seed)) {
    throw new Error("--seed must be an integer");
  }
  if (!Number.isInteger(options.minRatings) || options.minRatings < 1
      || !Number.isInteger(options.maxRatings) || options.maxRatings < options.minRatings) {
    throw new Error("Rating bounds must be positive integers, with max-ratings >= min-ratings");
  }
  if (options.prefix.length < 3 || options.prefix.length > 24
      || !/^[a-zA-Z0-9._-]+$/.test(options.prefix)) {
    throw new Error("--prefix must be 3-24 letters, numbers, dots, underscores, or hyphens");
  }
  if (`${options.prefix}-${String(options.count).padStart(3, "0")}`.length > 30) {
    throw new Error("--prefix is too long for the requested number of users");
  }
  options.api = options.api.replace(/\/+$/, "");
  const apiUrl = new URL(options.api);
  if (!["http:", "https:"].includes(apiUrl.protocol)) {
    throw new Error("--api must use http or https");
  }
  return options;
}

function createRandom(seed) {
  let state = seed >>> 0;
  return () => {
    state += 0x6d2b79f5;
    let value = state;
    value = Math.imul(value ^ (value >>> 15), value | 1);
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
  };
}

function choose(items, random) {
  return items[Math.floor(random() * items.length)];
}

function sampleWithoutReplacement(items, weights, count, random) {
  const pool = items.map((item, index) => ({ item, weight: weights[index] }));
  const selected = [];
  while (selected.length < count) {
    const totalWeight = pool.reduce((sum, entry) => sum + entry.weight, 0);
    let target = random() * totalWeight;
    let chosenIndex = pool.length - 1;
    for (let index = 0; index < pool.length; index += 1) {
      target -= pool[index].weight;
      if (target < 0) {
        chosenIndex = index;
        break;
      }
    }
    selected.push(pool.splice(chosenIndex, 1)[0].item);
  }
  return selected;
}

function drawRating(isFavoriteGenre, random) {
  const ratings = isFavoriteGenre ? [3, 4, 5] : [2, 3, 4];
  const weights = isFavoriteGenre ? [0.18, 0.48, 0.34] : [0.24, 0.48, 0.28];
  const roll = random();
  let cumulative = 0;
  for (let index = 0; index < ratings.length; index += 1) {
    cumulative += weights[index];
    if (roll < cumulative) return ratings[index];
  }
  return ratings.at(-1);
}

function request(api, path, { method = "GET", token, body, allowSelfSigned = false } = {}) {
  const url = new URL(path, `${api}/`);
  const transport = url.protocol === "https:" ? https : http;
  const payload = body === undefined ? undefined : JSON.stringify(body);

  return new Promise((resolve, reject) => {
    const req = transport.request(url, {
      method,
      rejectUnauthorized: !allowSelfSigned,
      headers: {
        Accept: "application/json",
        ...(payload ? { "Content-Type": "application/json", "Content-Length": Buffer.byteLength(payload) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      timeout: 20_000,
    }, (res) => {
      let response = "";
      res.setEncoding("utf8");
      res.on("data", (chunk) => { response += chunk; });
      res.on("end", () => {
        let data;
        try {
          data = response ? JSON.parse(response) : undefined;
        } catch {
          data = response;
        }
        resolve({ status: res.statusCode ?? 0, data });
      });
    });
    req.on("timeout", () => req.destroy(new Error(`Request timed out: ${method} ${url}`)));
    req.on("error", reject);
    if (payload) req.write(payload);
    req.end();
  });
}

async function apiRequest(api, path, options = {}, expectedStatuses = [200]) {
  const result = await request(api, path, options);
  if (!expectedStatuses.includes(result.status)) {
    const details = typeof result.data === "string" ? result.data : JSON.stringify(result.data);
    throw new Error(`${options.method ?? "GET"} ${path} failed (${result.status}): ${details ?? "empty response"}`);
  }
  return result;
}

async function loadMovies(api, token, allowSelfSigned) {
  const movies = [];
  let totalPages = 1;
  for (let page = 0; page < totalPages; page += 1) {
    const { data } = await apiRequest(
      api,
      `/api/movies?page=${page}&size=50`,
      { token, allowSelfSigned },
    );
    if (!Array.isArray(data?.content) || !Number.isInteger(data.totalPages)) {
      throw new Error("Movie API returned an unexpected page response");
    }
    movies.push(...data.content);
    totalPages = data.totalPages;
  }
  return [...new Map(movies.filter((movie) => movie.id).map((movie) => [movie.id, movie])).values()];
}

async function authenticate(api, username, email, password, allowSelfSigned) {
  const registration = await request(api, "/api/auth/register", {
    method: "POST",
    body: { username, email, password, confirmPassword: password },
    allowSelfSigned,
  });
  let user;
  if (registration.status === 201) {
    user = registration.data;
  } else if (registration.status === 409) {
    ({ data: user } = await apiRequest(api, "/api/auth/login", {
      method: "POST",
      body: { username, password },
      allowSelfSigned,
    }));
  } else {
    const details = typeof registration.data === "string"
      ? registration.data
      : JSON.stringify(registration.data);
    throw new Error(`Could not register ${username} (${registration.status}): ${details ?? "empty response"}`);
  }
  if (!user?.jwtToken) throw new Error(`Authentication returned no JWT for ${username}`);
  return user.jwtToken;
}

async function saveRating(api, token, movieId, value, allowSelfSigned) {
  const path = `/api/ratings/${encodeURIComponent(movieId)}`;
  const options = { method: "POST", token, body: { value }, allowSelfSigned };
  const result = await request(api, path, options);
  if (result.status === 201) return;
  if (result.status === 409) {
    await apiRequest(api, path, { ...options, method: "PUT" }, [200]);
    return;
  }
  const details = typeof result.data === "string" ? result.data : JSON.stringify(result.data);
  throw new Error(`Could not save rating for movie ${movieId} (${result.status}): ${details ?? "empty response"}`);
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    usage();
    return;
  }
  const password = process.env.SEED_USER_PASSWORD;
  if (!password || password.length < 8 || password.length > 72
      || !/[a-z]/.test(password) || !/[A-Z]/.test(password)
      || !/\d/.test(password) || !/[^A-Za-z0-9]/.test(password)) {
    throw new Error("Set SEED_USER_PASSWORD to an 8-72 character password with uppercase, lowercase, a number, and a special character");
  }

  const random = createRandom(options.seed);
  const firstSuffix = String(1).padStart(3, "0");
  const firstUsername = `${options.prefix}-${firstSuffix}`;
  const firstToken = await authenticate(
    options.api,
    firstUsername,
    `${options.prefix}-${firstSuffix}@example.test`,
    password,
    options.allowSelfSigned,
  );
  const [movies, genresResponse, popularResponse] = await Promise.all([
    loadMovies(options.api, firstToken, options.allowSelfSigned),
    apiRequest(options.api, "/api/genres", {
      token: firstToken,
      allowSelfSigned: options.allowSelfSigned,
    }),
    apiRequest(options.api, "/api/movies/popular?limit=20", {
      token: firstToken,
      allowSelfSigned: options.allowSelfSigned,
    }),
  ]);
  const genres = genresResponse.data;
  const popular = popularResponse.data;
  if (!Array.isArray(genres) || genres.length === 0) {
    throw new Error("No genres found; import or create genres before generating demo users");
  }
  if (!Array.isArray(popular)) throw new Error("Popular movies API returned an unexpected response");
  if (movies.length < options.minRatings) {
    throw new Error(`Need at least ${options.minRatings} movies, but the catalogue has ${movies.length}`);
  }

  const popularRanks = new Map(popular.map((movie, index) => [movie.id, index]));
  const movieWeights = movies.map((movie) => {
    const rank = popularRanks.get(movie.id);
    return rank === undefined ? 0.01 : 0.05 + 1 / Math.pow(rank + 1, 0.85);
  });
  console.log(`Loaded ${movies.length} movies and ${genres.length} genres. Generating ${options.count} users (seed ${options.seed}).`);

  let ratingCount = 0;
  for (let userIndex = 1; userIndex <= options.count; userIndex += 1) {
    const suffix = String(userIndex).padStart(3, "0");
    const username = `${options.prefix}-${suffix}`;
    const email = `${options.prefix}-${suffix}@example.test`;
    const token = userIndex === 1
      ? firstToken
      : await authenticate(options.api, username, email, password, options.allowSelfSigned);

    const favoriteCount = Math.min(genres.length, 2 + Math.floor(random() * 2));
    const favoriteGenres = new Set(sampleWithoutReplacement(
      genres,
      genres.map(() => 1),
      favoriteCount,
      random,
    ));
    const count = Math.min(
      movies.length,
      options.minRatings + Math.floor(random() * (options.maxRatings - options.minRatings + 1)),
    );
    const ratedMovies = sampleWithoutReplacement(movies, movieWeights, count, random);

    for (const movie of ratedMovies) {
      const movieGenres = Array.isArray(movie.genres) ? movie.genres : [];
      const isFavorite = movieGenres.some((genre) => favoriteGenres.has(genre));
      const value = drawRating(isFavorite, random);
      await saveRating(options.api, token, movie.id, value, options.allowSelfSigned);
      ratingCount += 1;
    }
    console.log(`[${userIndex}/${options.count}] ${username}: ${count} ratings; favorite genres: ${[...favoriteGenres].join(", ")}`);
  }

  console.log(`Done: ${options.count} users and ${ratingCount} ratings generated.`);
}

main().catch((error) => {
  console.error(`Generation failed: ${error.message}`);
  process.exitCode = 1;
});
