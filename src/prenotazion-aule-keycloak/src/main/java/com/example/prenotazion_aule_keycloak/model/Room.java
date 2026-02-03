package com.example.prenotazion_aule_keycloak.model;

import jakarta.persistence.*;

@Entity // Dice a Spring: "Questa è una tabella del database"
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int capacity;

    // Costruttore vuoto obbligatorio per JPA
    public Room() {}

    public Room(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    // Getters e Setters...
    public Long getId() { return id; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
}