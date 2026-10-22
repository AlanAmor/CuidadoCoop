package com.cooperativa.cuidado.socio;

public enum Especialidad {
    CUIDADOR("Cuidador/a"),
    ENFERMERO("Enfermero/a");

    private final String etiqueta;

    Especialidad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
