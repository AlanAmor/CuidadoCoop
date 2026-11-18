package com.cooperativa.cuidado.facturacion;

public enum TipoDocumentoReceptor {
    CUIT("80", "CUIT"),
    DNI("96", "DNI"),
    CUIL("86", "CUIL"),
    CONSUMIDOR_FINAL("99", "Consumidor Final");

    private final String codigoAfip;
    private final String descripcion;

    TipoDocumentoReceptor(String codigoAfip, String descripcion) {
        this.codigoAfip = codigoAfip;
        this.descripcion = descripcion;
    }

    public String getCodigoAfip() {
        return codigoAfip;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
