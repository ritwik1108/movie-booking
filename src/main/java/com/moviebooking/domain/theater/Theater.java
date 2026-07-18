package com.moviebooking.domain.theater;

import com.moviebooking.domain.city.City;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "theaters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Theater {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;
}
