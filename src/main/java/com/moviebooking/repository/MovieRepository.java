package com.moviebooking.repository;

import com.moviebooking.domain.movie.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    @Query("SELECT DISTINCT m FROM Show s JOIN s.movie m JOIN s.screen sc JOIN sc.theater t JOIN t.city c " +
           "WHERE (:cityId IS NULL OR c.id = :cityId) " +
           "AND (cast(:startTime as timestamp) IS NULL OR s.startTime >= :startTime) " +
           "AND (cast(:endTime as timestamp) IS NULL OR s.startTime <= :endTime) " +
           "AND (:language IS NULL OR LOWER(m.language) = LOWER(:language)) " +
           "AND (:genre IS NULL OR LOWER(m.genre) = LOWER(:genre))")
    List<Movie> findMoviesWithFilters(
            @Param("cityId") Long cityId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("language") String language,
            @Param("genre") String genre
    );
}
