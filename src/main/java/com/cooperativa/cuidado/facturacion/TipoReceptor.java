package com.cooperativa.cuidado.facturacion;

public enum TipoReceptor {
    PACIENTE("Paciente / Particular"),
    OBRA_SOCIAL("Obra Social / Prepaga");

    private final String descripcion;

    TipoReceptor(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
