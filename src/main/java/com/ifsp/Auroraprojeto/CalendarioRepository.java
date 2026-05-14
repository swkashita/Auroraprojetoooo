package com.ifsp.Auroraprojeto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarioRepository extends JpaRepository<Calendario, Long> {
    // Aqui você já ganha métodos como save(), findAll(), deleteById(), etc.
}