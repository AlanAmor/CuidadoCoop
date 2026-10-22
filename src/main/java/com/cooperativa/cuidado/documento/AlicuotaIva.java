package com.cooperativa.cuidado.documento;

import java.math.BigDecimal;

public enum AlicuotaIva {
    SIN_IVA("Sin IVA", "0"),
    IVA_10_5("IVA 10,5%", "10.5"),
    IVA_21("IVA 21%", "21"),
    IVA_27("IVA 27%", "27");

    private final String etiqueta;
    private final BigDecimal porcentaje;

    AlicuotaIva(String etiqueta, String porcentaje) {
        this.etiqueta = etiqueta;
        this.porcentaje = new BigDecimal(porcentaje);
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }
}
