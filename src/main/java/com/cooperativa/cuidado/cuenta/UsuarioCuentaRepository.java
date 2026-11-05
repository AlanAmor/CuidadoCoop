package com.cooperativa.cuidado.cuenta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioCuentaRepository extends JpaRepository<UsuarioCuenta, Long> {

    Optional<UsuarioCuenta> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);

    long countByRolId(Long rolId);

    List<UsuarioCuenta> findAllByOrderByApellidoAscNombreAsc();
}

