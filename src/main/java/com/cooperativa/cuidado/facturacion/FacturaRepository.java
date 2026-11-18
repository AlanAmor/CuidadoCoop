package com.cooperativa.cuidado.facturacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    List<Factura> findAllByOrderByFechaEmisionDescIdDesc();

    Optional<Factura> findTopByPuntoVentaAndTipoComprobanteOrderByNumeroComprobanteDesc(
            String puntoVenta, TipoComprobante tipoComprobante);

    boolean existsByPuntoVentaAndTipoComprobanteAndNumeroComprobante(
            String puntoVenta, TipoComprobante tipoComprobante, String numeroComprobante);

    List<Factura> findByLiquidacionFamiliaId(Long liquidacionFamiliaId);

    List<Factura> findByClienteId(Long clienteId);

    @Query("SELECT f FROM Factura f WHERE " +
            "(:tipoReceptor IS NULL OR f.tipoReceptor = :tipoReceptor) AND " +
            "(:desde IS NULL OR f.fechaEmision >= :desde) AND " +
            "(:hasta IS NULL OR f.fechaEmision <= :hasta) AND " +
            "(:buscar IS NULL OR :buscar = '' OR " +
            " LOWER(f.denominacionReceptor) LIKE LOWER(CONCAT('%', :buscar, '%')) OR " +
            " LOWER(f.documentoReceptor) LIKE LOWER(CONCAT('%', :buscar, '%')) OR " +
            " LOWER(f.numeroComprobante) LIKE LOWER(CONCAT('%', :buscar, '%')) OR " +
            " LOWER(f.cae) LIKE LOWER(CONCAT('%', :buscar, '%')) OR " +
            " LOWER(f.obraSocialNombre) LIKE LOWER(CONCAT('%', :buscar, '%'))) " +
            "ORDER BY f.fechaEmision DESC, f.id DESC")
    List<Factura> buscarFiltradas(
            @Param("buscar") String buscar,
            @Param("tipoReceptor") TipoReceptor tipoReceptor,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
