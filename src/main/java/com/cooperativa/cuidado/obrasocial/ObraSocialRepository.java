package com.cooperativa.cuidado.obrasocial;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ObraSocialRepository extends JpaRepository<ObraSocial, Long> {

    List<ObraSocial> findAllByOrderByNombreAsc();

    Optional<ObraSocial> findByCuit(String cuit);

    boolean existsByCuit(String cuit);
}
