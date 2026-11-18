package com.cooperativa.cuidado.facturacion;

public enum TipoComprobante {
    FACTURA_C("011", "Factura C", "C"),
    FACTURA_B("006", "Factura B", "B"),
    NOTA_CREDITO_C("013", "Nota de Crédito C", "NC-C"),
    NOTA_CREDITO_B("008", "Nota de Crédito B", "NC-B");

    private final String codigoAfip;
    private final String descripcion;
    private final String letra;

    TipoComprobante(String codigoAfip, String descripcion, String letra) {
        this.codigoAfip = codigoAfip;
        this.descripcion = descripcion;
        this.letra = letra;
    }

    public String getCodigoAfip() {
        return codigoAfip;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getLetra() {
        return letra;
    }
}
