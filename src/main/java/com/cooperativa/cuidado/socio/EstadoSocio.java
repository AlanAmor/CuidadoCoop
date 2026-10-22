package com.cooperativa.cuidado.socio;

public enum EstadoSocio {
    ACTIVO_EN_SERVICIO("En servicio"),
    DISPONIBLE_REEMPLAZO("Disponible para reemplazo"),
    INACTIVO("Inactivo");

    private final String etiqueta;

    EstadoSocio(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
