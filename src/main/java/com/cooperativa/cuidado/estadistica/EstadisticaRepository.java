package com.cooperativa.cuidado.estadistica;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.socio.Socio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EstadisticaRepository extends JpaRepository<Asistencia, Long> {

    @Query("""
            select a from Asistencia a
            join fetch a.socio
            join fetch a.cliente
            join fetch a.servicioDomicilio
            where a.fecha between :desde and :hasta
            """)
    List<Asistencia> asistenciasEntre(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("""
            select s from Socio s
            where s.estado <> com.cooperativa.cuidado.socio.EstadoSocio.INACTIVO
            order by s.apellido, s.nombre
            """)
    List<Socio> sociosActivos();
}
