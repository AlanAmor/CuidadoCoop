package com.cooperativa.cuidado.cuenta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolUsuarioRepository extends JpaRepository<RolUsuario, Long> {

    Optional<RolUsuario> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<RolUsuario> findAllByOrderByIdAsc();
}

