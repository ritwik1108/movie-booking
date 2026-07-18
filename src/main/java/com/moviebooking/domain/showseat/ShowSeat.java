package com.moviebooking.domain.showseat;

import com.moviebooking.domain.show.Show;
import com.moviebooking.domain.seat.Seat;
import com.moviebooking.domain.seathold.SeatHold;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "show_seats", uniqueConstraints = {
    @UniqueConstraint(name = "uniq_show_seat", columnNames = {"show_id", "seat_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShowSeatStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_by_hold_id")
    private SeatHold heldByHold;

    @Version
    @Column(nullable = false)
    private Long version;
}
