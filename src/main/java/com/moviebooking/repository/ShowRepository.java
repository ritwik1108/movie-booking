package com.moviebooking.repository;

import com.moviebooking.domain.show.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("SELECT s FROM Show s JOIN s.screen sc JOIN sc.theater t JOIN t.city c " +
           "WHERE (:movieId IS NULL OR s.movie.id = :movieId) " +
           "AND (:cityId IS NULL OR c.id = :cityId) " +
           "AND (cast(:startTime as timestamp) IS NULL OR s.startTime >= :startTime) " +
           "AND (cast(:endTime as timestamp) IS NULL OR s.startTime <= :endTime)")
    List<Show> findShows(
            @Param("movieId") Long movieId,
            @Param("cityId") Long cityId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );
}
