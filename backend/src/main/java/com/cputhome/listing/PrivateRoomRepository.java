package com.cputhome.listing;

import org.springframework.data.jpa.repository.JpaRepository;

/* persistence only, subtype rows are written at creation */
public interface PrivateRoomRepository extends JpaRepository<PrivateRoom, Long> {}
