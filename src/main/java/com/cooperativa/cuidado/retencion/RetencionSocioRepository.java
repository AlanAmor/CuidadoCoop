package com.cooperativa.cuidado.retencion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RetencionSocioRepository extends JpaRepository<RetencionSocio, Long> {

    List<RetencionSocio> findAllByOrderByPeriodoDescIdDesc();

    List<RetencionSocio> findAllBySocioIdOrderByPeriodoDescIdDesc(Long socioId);

    List<RetencionSocio> findAllByPeriodoOrderByIdDesc(String periodo);

    List<RetencionSocio> findAllBySocioIdAndPeriodoOrderByIdAsc(Long socioId, String periodo);

    void deleteAllBySocioId(Long socioId);
}

