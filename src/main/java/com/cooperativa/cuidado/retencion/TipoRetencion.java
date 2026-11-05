package com.cooperativa.cuidado.retencion;

public enum TipoRetencion {

    RETENCION_MONOTRIBUTO("RETENCION MONOTRIBUTO", "Retencion Pago Monotributo"),
    PRESTAMO_COOPERATIVA("PRÉSTAMO COOPERATIVA", "Prestamo Personal Asociado"),
    OTRAS_RETENCIONES("OTRAS RETENCIONES:", "Retención por Incumplimiento Articulo 5 Reglamento Interno");

    private final String etiquetaRecibo;
    private final String detallePorDefecto;

    TipoRetencion(String etiquetaRecibo, String detallePorDefecto) {
        this.etiquetaRecibo = etiquetaRecibo;
        this.detallePorDefecto = detallePorDefecto;
    }

    public String getEtiquetaRecibo() {
        return etiquetaRecibo;
    }

    public String getDetallePorDefecto() {
        return detallePorDefecto;
    }
}

