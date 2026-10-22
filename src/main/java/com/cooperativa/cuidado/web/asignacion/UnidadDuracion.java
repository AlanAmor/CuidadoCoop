package com.cooperativa.cuidado.web.asignacion;

public enum UnidadDuracion {
    DIAS("días", 366),
    SEMANAS("semanas", 52),
    MESES("meses", 12);

    private final String etiqueta;
    private final int maximo;

    UnidadDuracion(String etiqueta, int maximo) {
        this.etiqueta = etiqueta;
        this.maximo = maximo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Cantidad máxima admitida: en todos los casos equivale a un año como tope. */
    public int getMaximo() {
        return maximo;
    }
}
