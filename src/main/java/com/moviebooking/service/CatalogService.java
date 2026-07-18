package com.moviebooking.service;

import com.moviebooking.domain.city.City;
import com.moviebooking.domain.movie.Movie;
import com.moviebooking.domain.pricing.PricingRule;
import com.moviebooking.domain.screen.Screen;
import com.moviebooking.domain.seat.Seat;
import com.moviebooking.domain.show.Show;
import com.moviebooking.domain.showseat.ShowSeat;
import com.moviebooking.domain.showseat.ShowSeatStatus;
import com.moviebooking.domain.theater.Theater;
import com.moviebooking.dto.catalog.*;
import com.moviebooking.exception.BadRequestException;
import com.moviebooking.exception.NotFoundException;
import com.moviebooking.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CatalogService {

    private final CityRepository cityRepository;
    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final PricingRuleRepository pricingRuleRepository;

    public CatalogService(
            CityRepository cityRepository,
            TheaterRepository theaterRepository,
            ScreenRepository screenRepository,
            SeatRepository seatRepository,
            MovieRepository movieRepository,
            ShowRepository showRepository,
            ShowSeatRepository showSeatRepository,
            PricingRuleRepository pricingRuleRepository
    ) {
        this.cityRepository = cityRepository;
        this.theaterRepository = theaterRepository;
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.pricingRuleRepository = pricingRuleRepository;
    }

    // --- City ---
    public CityDto createCity(CityDto dto) {
        City city = City.builder().name(dto.getName()).build();
        City saved = cityRepository.save(city);
        return CityDto.builder().id(saved.getId()).name(saved.getName()).build();
    }

    public List<CityDto> getAllCities() {
        return cityRepository.findAll().stream()
                .map(c -> CityDto.builder().id(c.getId()).name(c.getName()).build())
                .collect(Collectors.toList());
    }

    public CityDto updateCity(Long id, CityDto dto) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CITY_NOT_FOUND", "City not found"));
        city.setName(dto.getName());
        City saved = cityRepository.save(city);
        return CityDto.builder().id(saved.getId()).name(saved.getName()).build();
    }

    public void deleteCity(Long id) {
        if (!cityRepository.existsById(id)) {
            throw new NotFoundException("CITY_NOT_FOUND", "City not found");
        }
        cityRepository.deleteById(id);
    }

    // --- Theater ---
    public TheaterDto createTheater(TheaterDto dto) {
        City city = cityRepository.findById(dto.getCityId())
                .orElseThrow(() -> new NotFoundException("CITY_NOT_FOUND", "City not found"));
        Theater theater = Theater.builder()
                .city(city)
                .name(dto.getName())
                .address(dto.getAddress())
                .build();
        Theater saved = theaterRepository.save(theater);
        return TheaterDto.builder()
                .id(saved.getId())
                .cityId(city.getId())
                .name(saved.getName())
                .address(saved.getAddress())
                .build();
    }

    public List<TheaterDto> getTheatersByCity(Long cityId) {
        return theaterRepository.findByCityId(cityId).stream()
                .map(t -> TheaterDto.builder()
                        .id(t.getId())
                        .cityId(t.getCity().getId())
                        .name(t.getName())
                        .address(t.getAddress())
                        .build())
                .collect(Collectors.toList());
    }

    public void deleteTheater(Long id) {
        if (!theaterRepository.existsById(id)) {
            throw new NotFoundException("THEATER_NOT_FOUND", "Theater not found");
        }
        theaterRepository.deleteById(id);
    }

    // --- Screen ---
    public ScreenDto createScreen(ScreenDto dto) {
        Theater theater = theaterRepository.findById(dto.getTheaterId())
                .orElseThrow(() -> new NotFoundException("THEATER_NOT_FOUND", "Theater not found"));
        Screen screen = Screen.builder()
                .theater(theater)
                .name(dto.getName())
                .build();
        Screen saved = screenRepository.save(screen);
        return ScreenDto.builder()
                .id(saved.getId())
                .theaterId(theater.getId())
                .name(saved.getName())
                .build();
    }

    public List<ScreenDto> getScreensByTheater(Long theaterId) {
        return screenRepository.findByTheaterId(theaterId).stream()
                .map(s -> ScreenDto.builder()
                        .id(s.getId())
                        .theaterId(s.getTheater().getId())
                        .name(s.getName())
                        .build())
                .collect(Collectors.toList());
    }

    public void deleteScreen(Long id) {
        if (!screenRepository.existsById(id)) {
            throw new NotFoundException("SCREEN_NOT_FOUND", "Screen not found");
        }
        screenRepository.deleteById(id);
    }

    // --- Bulk Seat Creation ---
    public void createSeats(Long screenId, BulkSeatCreationRequest request) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new NotFoundException("SCREEN_NOT_FOUND", "Screen not found"));

        List<Seat> seatsToSave = new ArrayList<>();
        for (BulkSeatCreationRequest.RowLayout layout : request.getLayouts()) {
            for (int i = 1; i <= layout.getNumSeats(); i++) {
                Seat seat = Seat.builder()
                        .screen(screen)
                        .rowLabel(layout.getRowLabel())
                        .seatNumber(i)
                        .tier(layout.getTier())
                        .build();
                seatsToSave.add(seat);
            }
        }
        seatRepository.saveAll(seatsToSave);
    }

    // --- Movie ---
    public MovieDto createMovie(MovieDto dto) {
        Movie movie = Movie.builder()
                .title(dto.getTitle())
                .durationMinutes(dto.getDurationMinutes())
                .language(dto.getLanguage())
                .genre(dto.getGenre())
                .certification(dto.getCertification())
                .build();
        Movie saved = movieRepository.save(movie);
        return toMovieDto(saved);
    }

    public MovieDto getMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("MOVIE_NOT_FOUND", "Movie not found"));
        return toMovieDto(movie);
    }

    public List<MovieDto> searchMovies(Long cityId, OffsetDateTime date, String language, String genre) {
        OffsetDateTime startTime = null;
        OffsetDateTime endTime = null;
        if (date != null) {
            startTime = date.toLocalDate().atStartOfDay().atOffset(date.getOffset());
            endTime = date.toLocalDate().plusDays(1).atStartOfDay().minusSeconds(1).atOffset(date.getOffset());
        }

        return movieRepository.findMoviesWithFilters(cityId, startTime, endTime, language, genre).stream()
                .map(this::toMovieDto)
                .collect(Collectors.toList());
    }

    public void deleteMovie(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new NotFoundException("MOVIE_NOT_FOUND", "Movie not found");
        }
        movieRepository.deleteById(id);
    }

    private MovieDto toMovieDto(Movie movie) {
        return MovieDto.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .durationMinutes(movie.getDurationMinutes())
                .language(movie.getLanguage())
                .genre(movie.getGenre())
                .certification(movie.getCertification())
                .build();
    }

    // --- Show ---
    public ShowDto createShow(ShowDto dto) {
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BadRequestException("INVALID_SHOW_TIMES", "Show end time must be after start time");
        }

        Movie movie = movieRepository.findById(dto.getMovieId())
                .orElseThrow(() -> new NotFoundException("MOVIE_NOT_FOUND", "Movie not found"));
        Screen screen = screenRepository.findById(dto.getScreenId())
                .orElseThrow(() -> new NotFoundException("SCREEN_NOT_FOUND", "Screen not found"));

        // Validate screen overlaps
        // Query existing shows on this screen
        List<Show> existingShows = showRepository.findShows(null, null, null, null);
        for (Show es : existingShows) {
            if (es.getScreen().getId().equals(screen.getId())) {
                if (dto.getStartTime().isBefore(es.getEndTime()) && dto.getEndTime().isAfter(es.getStartTime())) {
                    throw new BadRequestException("SHOW_OVERLAP", "Show schedule overlaps with an existing show on this screen");
                }
            }
        }

        Show show = Show.builder()
                .movie(movie)
                .screen(screen)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .build();

        Show saved = showRepository.save(show);

        // Auto-populate ShowSeats from the screen's seats
        List<Seat> screenSeats = seatRepository.findByScreenId(screen.getId());
        if (screenSeats.isEmpty()) {
            throw new BadRequestException("NO_SEATS_IN_SCREEN", "Cannot create a show on a screen with 0 seats");
        }

        List<ShowSeat> showSeats = screenSeats.stream()
                .map(seat -> ShowSeat.builder()
                        .show(saved)
                        .seat(seat)
                        .status(ShowSeatStatus.AVAILABLE)
                        .build())
                .collect(Collectors.toList());
        showSeatRepository.saveAll(showSeats);

        return toShowDto(saved);
    }

    public List<ShowDto> getShowsForMovie(Long movieId, Long cityId, OffsetDateTime date) {
        OffsetDateTime startTime = null;
        OffsetDateTime endTime = null;
        if (date != null) {
            startTime = date.toLocalDate().atStartOfDay().atOffset(date.getOffset());
            endTime = date.toLocalDate().plusDays(1).atStartOfDay().minusSeconds(1).atOffset(date.getOffset());
        }

        return showRepository.findShows(movieId, cityId, startTime, endTime).stream()
                .map(this::toShowDto)
                .collect(Collectors.toList());
    }

    public void deleteShow(Long id) {
        if (!showRepository.existsById(id)) {
            throw new NotFoundException("SHOW_NOT_FOUND", "Show not found");
        }
        showRepository.deleteById(id);
    }

    private ShowDto toShowDto(Show show) {
        return ShowDto.builder()
                .id(show.getId())
                .movieId(show.getMovie().getId())
                .screenId(show.getScreen().getId())
                .startTime(show.getStartTime())
                .endTime(show.getEndTime())
                .build();
    }

    // --- Pricing Rules ---
    public PricingRuleDto createPricingRule(PricingRuleDto dto) {
        PricingRule rule = PricingRule.builder()
                .scope(dto.getScope())
                .scopeRefId(dto.getScopeRefId())
                .seatTier(dto.getSeatTier())
                .basePrice(dto.getBasePrice())
                .weekendMultiplier(dto.getWeekendMultiplier())
                .build();
        PricingRule saved = pricingRuleRepository.save(rule);
        return toPricingRuleDto(saved);
    }

    private PricingRuleDto toPricingRuleDto(PricingRule rule) {
        return PricingRuleDto.builder()
                .id(rule.getId())
                .scope(rule.getScope())
                .scopeRefId(rule.getScopeRefId())
                .seatTier(rule.getSeatTier())
                .basePrice(rule.getBasePrice())
                .weekendMultiplier(rule.getWeekendMultiplier())
                .build();
    }
}
