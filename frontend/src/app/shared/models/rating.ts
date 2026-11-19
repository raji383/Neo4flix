
export interface RatedMovie {
  id: string;
  title: string;
  director: string;
  description: string;
  releaseYear: number;
  genres: string[];
  imageUrl: string | null;
  averageRating: number | null;
  ratingCount: number;
  myRating: number;
}
