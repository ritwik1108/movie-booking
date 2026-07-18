package com.moviebooking.repository;

import com.moviebooking.domain.showseat.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowId(Long showId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowSeat ss JOIN FETCH ss.seat s WHERE ss.show.id = :showId AND s.id IN :seatIds ORDER BY s.id ASC")
    List<ShowSeat> findByShowIdAndSeatIdInWithLock(
            @Param("showId") Long showId,
            @Param("seatIds") List<Long> seatIds
    );
}
