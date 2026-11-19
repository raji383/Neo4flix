export interface Movie {
  id: string;
  title: string;
  director: string;
  description: string;
  releaseYear: number;
  genres: string[];
  imageUrl?: string | null;
  averageRating?: number | null;
  ratingCount: number;
}
export interface MovieRecommendation {
  movieId: string;
  title: string;
  hybridScore: number;
  director: string;
  description: string;
  releaseYear: number;
  genres: string[];
  imageUrl: string;
  averageRating: number;
  ratingCount: number;
}
export interface CreateMovie {
  title: string;
  releaseYear: number;
  director: string;
  description: string;
  genres: string[];
  file?: File;
}

export interface MovieSearchParams {
  q?: string;
  page?: number;
  size?: number;
  genres?: string[];
  releaseYear?: number;
}
