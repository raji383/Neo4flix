export interface Movie {
  id: string;
  title: string;
  director: string;
  description: string;
  releaseDate: number;
  genres: string[];
  imageUrl: string;
  averageRating?: number;
  ratingCount?: number;

}

export interface CreateMovie {
  title: string;
  releaseDate: number;
  director: string;
  description: string;
  genres: string[];
  file: File;
}



export interface MovieSearchParams {

  q?: string;
  page?: number;
  size?: number;
  genres?: string[];
  releaseDate?: number;
}
