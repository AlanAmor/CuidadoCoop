package com.cooperativa.cuidado.servicio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoServicioRepository extends JpaRepository<TipoServicio, Long> {

    List<TipoServicio> findAllByOrderByNombreAsc();

    Optional<TipoServicio> findByNombreIgnoreCase(String nombre);
}
