package com.cooperativa.cuidado.facturacion;

public enum ConceptoFactura {
    PRODUCTOS(1, "Productos"),
    SERVICIOS(2, "Servicios"),
    PRODUCTOS_Y_SERVICIOS(3, "Productos y Servicios");

    private final int codigo;
    private final String descripcion;

    ConceptoFactura(int codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
