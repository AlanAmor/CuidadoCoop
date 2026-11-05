package com.cooperativa.cuidado.liquidacionfamilia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiquidacionFamiliaRepository extends JpaRepository<LiquidacionFamilia, Long> {

    List<LiquidacionFamilia> findAllByOrderByIdDesc();

    void deleteAllByClienteId(Long clienteId);
}

