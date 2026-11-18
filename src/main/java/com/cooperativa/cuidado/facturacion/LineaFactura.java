package com.cooperativa.cuidado.facturacion;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class LineaFactura {

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad = BigDecimal.ONE;

    @Column(length = 30)
    private String unidadMedida = "horas";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeIva = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    public LineaFactura() {
    }

    public LineaFactura(String descripcion, BigDecimal cantidad, String unidadMedida, BigDecimal precioUnitario, BigDecimal porcentajeIva, BigDecimal subtotal) {
        this.descripcion = descripcion;
        this.cantidad = cantidad != null ? cantidad : BigDecimal.ONE;
        this.unidadMedida = unidadMedida != null ? unidadMedida : "horas";
        this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
        this.porcentajeIva = porcentajeIva != null ? porcentajeIva : BigDecimal.ZERO;
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getPorcentajeIva() {
        return porcentajeIva;
    }

    public void setPorcentajeIva(BigDecimal porcentajeIva) {
        this.porcentajeIva = porcentajeIva;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
