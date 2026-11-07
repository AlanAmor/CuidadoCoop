package com.cooperativa.cuidado.feriado;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeriadoNacionalRepository extends JpaRepository<FeriadoNacional, Long> {

    boolean existsByFecha(LocalDate fecha);

    Optional<FeriadoNacional> findByFecha(LocalDate fecha);

    List<FeriadoNacional> findAllByOrderByFechaAsc();

    List<FeriadoNacional> findAllByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);
}

