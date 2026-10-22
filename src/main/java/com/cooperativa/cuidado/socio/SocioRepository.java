package com.cooperativa.cuidado.socio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SocioRepository extends JpaRepository<Socio, Long> {

    Optional<Socio> findByDni(String dni);

    boolean existsByCbu(String cbu);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByCbuAndIdNot(String cbu, Long id);

    List<Socio> findAllByEstado(EstadoSocio estado);

    long countByEstado(EstadoSocio estado);

    @Query("""
            select sum(a.horasTotales)
            from Asistencia a
            where a.socio.id = :socioId
              and a.fecha between :desde and :hasta
            """)
    BigDecimal sumarHorasTrabajadas(
            @Param("socioId") Long socioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Socio s where s.id = :id")
    Optional<Socio> findByIdForUpdate(@Param("id") Long id);
}
