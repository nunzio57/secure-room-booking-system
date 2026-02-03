package com.example.prenotazion_aule_keycloak.repository;

import com.example.prenotazion_aule_keycloak.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}