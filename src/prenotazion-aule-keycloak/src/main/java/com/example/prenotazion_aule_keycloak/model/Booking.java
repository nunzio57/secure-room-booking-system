package com.example.prenotazion_aule_keycloak.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @ManyToOne // Molte prenotazioni possono essere per una sola aula
    @JoinColumn(name = "room_id")
    private Room room;

    private LocalDate date;

    public Booking() {}

    public Booking(String username, Room room, LocalDate date) {
        this.username = username;
        this.room = room;
        this.date = date;
    }

    // Getters...
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public Room getRoom() { return room; }
    public LocalDate getDate() { return date; }

    // Aggiungi un setter per accettazione (se vuoi replicare la logica Admin)
    // private boolean accepted; ...
}