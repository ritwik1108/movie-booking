package com.moviebooking.repository;

import com.moviebooking.domain.screen.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScreenRepository extends JpaRepository<Screen, Long> {
    List<Screen> findByTheaterId(Long theaterId);
}
