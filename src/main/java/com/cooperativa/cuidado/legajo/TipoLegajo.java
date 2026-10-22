package com.cooperativa.cuidado.legajo;

public enum TipoLegajo {
    SOCIO,
    CLIENTE;

    /** Convierte el segmento de URL ("socios" o "clientes") en el tipo de legajo. */
    public static TipoLegajo desdeRuta(String segmento) {
        if ("socios".equalsIgnoreCase(segmento)) {
            return SOCIO;
        }
        if ("clientes".equalsIgnoreCase(segmento)) {
            return CLIENTE;
        }
        return null;
    }
}
