package org.informatika.icalf.moviedatabase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.informatika.icalf.moviedatabase.data.MovieContract;

import java.io.UnsupportedEncodingException;
import java.util.List;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.TmdbReviews;
import info.movito.themoviedbapi.model.MovieDb;
import info.movito.themoviedbapi.model.Reviews;
import info.movito.themoviedbapi.model.Video;
import info.movito.themoviedbapi.model.core.MovieResultsPage;

/**
 * Created by icalF on 5/5/2016.
 */
public class FetchMovieTask extends AsyncTask<String, Void, Void> {
  private String key;
  private final Context mContext;

  public FetchMovieTask(Context context) {
    mContext = context;

    try {
      byte[] b64 = Base64.decode(BuildConfig.TMDB_API_KEY, Base64.DEFAULT);
      key = new String(b64, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  private boolean DEBUG = true;

  @Override
  protected Void doInBackground(String... params) {
    TmdbMovies tmdbMovies = new TmdbApi(key).getMovies();
    MovieResultsPage movieResultsPage = tmdbMovies.getPopularMovies("en", 1);
    List<MovieDb> list = movieResultsPage.getResults();

    insertMovies(list);

    return null;
  }

  private void insertMovies(List<MovieDb> list) {
    for (MovieDb movie : list) {
      Cursor movieCursor = mContext.getContentResolver().query(
              MovieContract.MovieEntry.CONTENT_URI,
              new String[] { MovieContract.MovieEntry._ID },
              MovieContract.MovieEntry._ID+ " = ?",
              new String[] { Integer.toString(movie.getId()) },
              null);

      if (!movieCursor.moveToFirst()) {
        ContentValues movieValues = new ContentValues();

        movieValues.put(MovieContract.MovieEntry._ID, movie.getId());
        movieValues.put(MovieContract.MovieEntry.COLUMMN_POSTER_URL, movie.getPosterPath());
        movieValues.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, movie.getOverview());
        movieValues.put(MovieContract.MovieEntry.COLUMN_POPULARITY, movie.getPopularity());
        movieValues.put(MovieContract.MovieEntry.COLUMN_RUNTIME, movie.getRuntime());
        movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, movie.getTitle());
        movieValues.put(MovieContract.MovieEntry.COLUMN_VOTE, movie.getVoteAverage());
        movieValues.put(MovieContract.MovieEntry.COLUMN_YEAR, movie.getReleaseDate());

        mContext.getContentResolver().insert(
              MovieContract.MovieEntry.CONTENT_URI,
              movieValues
        );

        insertReviews(movie.getId());
        insertTrailers(movie.getId(), movie.getVideos());
        Log.d("insertReviews: ", movieValues.getAsString(MovieContract.MovieEntry.COLUMN_TITLE));
      }

      movieCursor.close();
    }
  }

  private void insertTrailers(int movId, List<Video> videos) {
    if (videos != null) {
      for (Video video : videos) {
        ContentValues trailerValues = new ContentValues();
        trailerValues.put(MovieContract.TrailerEntry.COLUMMN_MOV_ID, movId);
        trailerValues.put(MovieContract.TrailerEntry.COLUMN_URL, video.getKey());

        mContext.getContentResolver().insert(
                MovieContract.TrailerEntry.CONTENT_URI,
                trailerValues
        );
      }
    }
  }

  private void insertReviews(int id) {
    TmdbReviews tmdbReviews = new TmdbApi(key).getReviews();
    TmdbReviews.ReviewResultsPage reviewResultsPage = tmdbReviews.getReviews(id, "en", 1);
    List<Reviews> reviews = reviewResultsPage.getResults();

    if (reviews != null) {
      for (Reviews review : reviews) {
        ContentValues reviewValues = new ContentValues();
        reviewValues.put(MovieContract.ReviewEntry.COLUMMN_MOV_ID, id);
        reviewValues.put(MovieContract.ReviewEntry.COLUMN_AUTHOR, "'"+review.getAuthor()+"'");
        reviewValues.put(MovieContract.ReviewEntry.COLUMN_CONTENT, review.getContent());
        reviewValues.put(MovieContract.ReviewEntry.COLUMN_URL, review.getUrl());

        mContext.getContentResolver().insert(
                MovieContract.ReviewEntry.CONTENT_URI,
                reviewValues
        );
      }
    }
  }
}