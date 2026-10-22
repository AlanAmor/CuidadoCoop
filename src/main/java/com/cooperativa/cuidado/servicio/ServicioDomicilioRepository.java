package com.cooperativa.cuidado.servicio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicioDomicilioRepository extends JpaRepository<ServicioDomicilio, Long> {

    List<ServicioDomicilio> findAllByOrderByIdAsc();

    long countByTipoServicioId(Long tipoServicioId);
}
