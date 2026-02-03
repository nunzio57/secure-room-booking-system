package com.example.prenotazion_aule_keycloak.service;

import com.example.prenotazion_aule_keycloak.model.Booking;
import com.example.prenotazion_aule_keycloak.model.Room;
import com.example.prenotazion_aule_keycloak.repository.BookingRepository;
import com.example.prenotazion_aule_keycloak.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {

    // 1. Inizializzazione del Logger per l'Audit Trail (NIST AU-2)
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public BookingService(RoomRepository roomRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    @PostConstruct
    public void initRooms() {
        if (roomRepository.count() == 0) {
            logger.info("SYSTEM: Initializing default rooms database...");
            roomRepository.save(new Room("Aula 1 - System Security", 40));
            roomRepository.save(new Room("Aula 2 - Network Security", 30));
            roomRepository.save(new Room("Aula 3 - Software Security", 25));
            roomRepository.save(new Room("Aula 4 - Cloud Infrastructures", 40));
            roomRepository.save(new Room("Aula 5 - DevOps", 30));
        }
    }

    public List<Room> getAllRooms() { return roomRepository.findAll(); }
    public List<Booking> getAllBookings() { return bookingRepository.findAll(); }
    public List<Booking> getBookingsByUser(String username) { return bookingRepository.findByUsername(username); }

    public Booking createBooking(String username, Long roomId, LocalDate date) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> {
                    logger.warn("SECURITY: User '{}' attempted to book non-existent room ID {}", username, roomId);
                    return new IllegalArgumentException("Aula non trovata");
                });

        boolean occupata = bookingRepository.findAll().stream()
                .anyMatch(b -> b.getRoom().getId().equals(roomId) && b.getDate().equals(date));

        if (occupata) {
            logger.warn("BUSINESS: User '{}' failed to book Room {} on {} (Already Occupied)", username, room.getName(), date);
            throw new IllegalStateException("Aula già occupata!");
        }

        Booking savedBooking = bookingRepository.save(new Booking(username, room, date));

        // 2. LOG DI AUDIT (Successo): Traccia CHI ha fatto COSA e QUANDO
        logger.info("AUDIT: User '{}' SUCCESSFULLY created booking ID {} for Room '{}' on {}",
                username, savedBooking.getId(), room.getName(), date);

        return savedBooking;
    }

    // Cancellazione da parte dell'Admin
    public void deleteBooking(Long id) {
        if (bookingRepository.existsById(id)) {
            bookingRepository.deleteById(id);
            logger.info("AUDIT: ADMIN deleted booking ID {}", id);
        } else {
            logger.warn("AUDIT: ADMIN tried to delete non-existent booking ID {}", id);
        }
    }

    // Cancellazione da parte dell'Utente (con controllo proprietà)
    public void deleteBookingIfOwner(Long bookingId, String username) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Prenotazione non trovata"));

        if (!booking.getUsername().equals(username)) {
            // Log di sicurezza critico: tentativo di cancellare dati altrui (IDOR attempt)
            logger.error("SECURITY ALERT: User '{}' attempted to delete booking ID {} owned by '{}'",
                    username, bookingId, booking.getUsername());
            throw new IllegalArgumentException("Non puoi cancellare questa prenotazione.");
        }

        bookingRepository.delete(booking);

        // Log di Audit standard
        logger.info("AUDIT: User '{}' deleted their own booking ID {}", username, bookingId);
    }

    @Transactional
    public void deleteUserData(String username) {
        List<Booking> userBookings = bookingRepository.findByUsername(username);
        int count = userBookings.size();

        if (!userBookings.isEmpty()) {
            bookingRepository.deleteAll(userBookings);
        }

        // Log per conformità GDPR (Diritto all'Oblio)
        logger.info("GDPR AUDIT: Full account deletion requested for user '{}'. Removed {} bookings associated with this identity.", username, count);
    }
}