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
            select s from Socio s
            where (:termino is null or :termino = ''
                or lower(s.apellido) like lower(concat('%', :termino, '%'))
                or lower(s.nombre) like lower(concat('%', :termino, '%'))
                or lower(concat(s.apellido, ' ', s.nombre)) like lower(concat('%', :termino, '%'))
                or lower(concat(s.nombre, ' ', s.apellido)) like lower(concat('%', :termino, '%'))
                or lower(s.dni) like lower(concat('%', :termino, '%'))
                or lower(coalesce(s.cuil, '')) like lower(concat('%', :termino, '%'))
                or lower(s.telefono) like lower(concat('%', :termino, '%'))
                or lower(coalesce(s.correo, '')) like lower(concat('%', :termino, '%'))
                or lower(s.direccion) like lower(concat('%', :termino, '%'))
                or lower(coalesce(s.localidad, '')) like lower(concat('%', :termino, '%')))
              and (:especialidad is null or s.especialidad = :especialidad)
              and (:estado is null or s.estado = :estado)
              and (:inicial is null or :inicial = '' or upper(s.apellido) like concat(upper(:inicial), '%'))
            order by s.apellido asc, s.nombre asc
            """)
    List<Socio> buscarIndexado(
            @Param("termino") String termino,
            @Param("especialidad") Especialidad especialidad,
            @Param("estado") EstadoSocio estado,
            @Param("inicial") String inicial);

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
