package com.cooperativa.cuidado.asistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    @Query("""
            select sum(a.horasTotales)
            from Asistencia a
            where a.fecha between :desde and :hasta
            """)
    java.math.BigDecimal sumarHorasCargadas(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @EntityGraph(attributePaths = {"servicioDomicilio", "cliente"})
    List<Asistencia> findAllBySocioIdAndFechaBetween(
            Long socioId,
            LocalDate desde,
            LocalDate hasta);

    @EntityGraph(attributePaths = {"socio", "servicioDomicilio", "cliente"})
    List<Asistencia> findAllByFechaBetweenOrderByFechaAscHoraInicioAsc(
            LocalDate desde,
            LocalDate hasta);

    @EntityGraph(attributePaths = {"socio", "servicioDomicilio", "cliente"})
    List<Asistencia> findAllBySocioIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(
            Long socioId,
            LocalDate desde,
            LocalDate hasta);

    @EntityGraph(attributePaths = {"socio", "servicioDomicilio", "cliente"})
    List<Asistencia> findAllByClienteIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(
            Long clienteId,
            LocalDate desde,
            LocalDate hasta);

    @EntityGraph(attributePaths = {"socio", "servicioDomicilio", "cliente"})
    List<Asistencia> findAllByFechaBetweenOrderByHoraInicioAsc(
            LocalDate desde,
            LocalDate hasta);

    long deleteAllBySocioId(Long socioId);

    long deleteAllByServicioDomicilioId(Long servicioId);

    long deleteAllByClienteId(Long clienteId);

    default boolean existeSolapamiento(Long socioId, LocalDateTime inicio, LocalDateTime fin, Long excluirId) {
        LocalDate desde = inicio.toLocalDate().minusDays(1);
        LocalDate hasta = fin.toLocalDate();

        return findAllBySocioIdAndFechaBetween(socioId, desde, hasta).stream()
                .filter(asistencia -> !Objects.equals(asistencia.getId(), excluirId))
                .anyMatch(asistencia -> {
                    LocalDateTime inicioExistente = LocalDateTime.of(
                            asistencia.getFecha(), asistencia.getHoraInicio());
                    LocalDate fechaFinExistente = asistencia.getHoraFin().isBefore(asistencia.getHoraInicio())
                            ? asistencia.getFecha().plusDays(1)
                            : asistencia.getFecha();
                    LocalDateTime finExistente = LocalDateTime.of(
                            fechaFinExistente, asistencia.getHoraFin());
                    return inicio.isBefore(finExistente) && fin.isAfter(inicioExistente);
                });
    }
}
