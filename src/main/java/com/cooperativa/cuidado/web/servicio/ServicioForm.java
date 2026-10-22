package com.cooperativa.cuidado.web.servicio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ServicioForm {

    @NotNull(message = "Seleccioná el tipo de servicio")
    private Long tipoServicioId;

    @NotNull(message = "Ingresá el valor por hora")
    @DecimalMin(value = "0.00", inclusive = false, message = "El valor por hora debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El valor por hora admite hasta dos decimales")
    private BigDecimal valorHora;

    @NotNull(message = "Ingresá lo que se le paga al socio por hora")
    @DecimalMin(value = "0.00", inclusive = false, message = "El pago al socio debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El pago al socio admite hasta dos decimales")
    private BigDecimal valorHoraSocio;

    public BigDecimal getValorHoraSocio() {
        return valorHoraSocio;
    }

    public void setValorHoraSocio(BigDecimal valorHoraSocio) {
        this.valorHoraSocio = valorHoraSocio;
    }

    public Long getTipoServicioId() {
        return tipoServicioId;
    }

    public void setTipoServicioId(Long tipoServicioId) {
        this.tipoServicioId = tipoServicioId;
    }

    public BigDecimal getValorHora() {
        return valorHora;
    }

    public void setValorHora(BigDecimal valorHora) {
        this.valorHora = valorHora;
    }
}
