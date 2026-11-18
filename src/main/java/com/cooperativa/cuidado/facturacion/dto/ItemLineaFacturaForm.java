package com.cooperativa.cuidado.facturacion.dto;

import java.math.BigDecimal;

public class ItemLineaFacturaForm {
    private String descripcion;
    private BigDecimal cantidad = BigDecimal.ONE;
    private String unidadMedida = "horas";
    private BigDecimal precioUnitario = BigDecimal.ZERO;
    private BigDecimal porcentajeIva = BigDecimal.ZERO;
    private BigDecimal subtotal = BigDecimal.ZERO;

    public ItemLineaFacturaForm() {
    }

    public ItemLineaFacturaForm(String descripcion, BigDecimal cantidad, String unidadMedida, BigDecimal precioUnitario, BigDecimal porcentajeIva, BigDecimal subtotal) {
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.unidadMedida = unidadMedida;
        this.precioUnitario = precioUnitario;
        this.porcentajeIva = porcentajeIva;
        this.subtotal = subtotal;
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
