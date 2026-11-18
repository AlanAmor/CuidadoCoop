package com.cooperativa.cuidado.facturacion;

public enum CondicionIva {
    CONSUMIDOR_FINAL("Consumidor Final"),
    IVA_EXENTO("IVA Exento"),
    IVA_RESPONSABLE_INSCRIPTO("IVA Responsable Inscripto"),
    MONOTRIBUTO("Responsable Monotributo");

    private final String descripcion;

    CondicionIva(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
