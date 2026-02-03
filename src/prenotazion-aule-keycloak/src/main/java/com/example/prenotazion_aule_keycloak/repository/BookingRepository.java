package com.example.prenotazion_aule_keycloak.repository;

import com.example.prenotazion_aule_keycloak.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    // Trova tutte le prenotazioni di un certo utente
    List<Booking> findByUsername(String username);
}