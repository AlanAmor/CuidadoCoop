package com.cooperativa.cuidado.facturacion;

public enum EstadoFactura {
    EMITIDA("Emitida"),
    ANULADA("Anulada");

    private final String descripcion;

    EstadoFactura(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
