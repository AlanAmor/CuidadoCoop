package com.cooperativa.cuidado.recibo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReciboRetribucionRepository extends JpaRepository<ReciboRetribucion, Long> {

    @Query("select coalesce(max(r.numeroRecibo), 0) from ReciboRetribucion r")
    Long obtenerUltimoNumero();

    List<ReciboRetribucion> findAllByOrderByNumeroReciboDesc();

    void deleteAllBySocioId(Long socioId);
}

