package com.cooperativa.cuidado.web.servicio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ServicioForm {

    @NotNull(message = "Seleccioná el tipo de servicio")
    private Long tipoServicioId;

    @NotNull(message = "Ingresá el valor hora cliente (día de semana, menos de 12 hs)")
    @DecimalMin(value = "0.00", inclusive = false, message = "El valor por hora debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El valor por hora admite hasta dos decimales")
    private BigDecimal valorHora;

    @DecimalMin(value = "0.00", inclusive = false, message = "El valor por hora debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El valor por hora admite hasta dos decimales")
    private BigDecimal valorHoraClienteSemanaMas12;

    @DecimalMin(value = "0.00", inclusive = false, message = "El valor por hora debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El valor por hora admite hasta dos decimales")
    private BigDecimal valorHoraClienteFindeMenos12;

    @DecimalMin(value = "0.00", inclusive = false, message = "El valor por hora debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El valor por hora admite hasta dos decimales")
    private BigDecimal valorHoraClienteFindeMas12;

    @DecimalMin(value = "0.00", inclusive = false, message = "El valor por hora debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El valor por hora admite hasta dos decimales")
    private BigDecimal valorHoraClienteFeriadoMenos12;

    @DecimalMin(value = "0.00", inclusive = false, message = "El valor por hora debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El valor por hora admite hasta dos decimales")
    private BigDecimal valorHoraClienteFeriadoMas12;

    @NotNull(message = "Ingresá lo que se le paga al socio por hora (día de semana, menos de 12 hs)")
    @DecimalMin(value = "0.00", inclusive = false, message = "El pago al socio debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El pago al socio admite hasta dos decimales")
    private BigDecimal valorHoraSocio;

    @DecimalMin(value = "0.00", inclusive = false, message = "El pago al socio debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El pago al socio admite hasta dos decimales")
    private BigDecimal valorHoraSocioSemanaMas12;

    @DecimalMin(value = "0.00", inclusive = false, message = "El pago al socio debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El pago al socio admite hasta dos decimales")
    private BigDecimal valorHoraSocioFindeMenos12;

    @DecimalMin(value = "0.00", inclusive = false, message = "El pago al socio debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El pago al socio admite hasta dos decimales")
    private BigDecimal valorHoraSocioFindeMas12;

    @DecimalMin(value = "0.00", inclusive = false, message = "El pago al socio debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El pago al socio admite hasta dos decimales")
    private BigDecimal valorHoraSocioFeriadoMenos12;

    @DecimalMin(value = "0.00", inclusive = false, message = "El pago al socio debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El pago al socio admite hasta dos decimales")
    private BigDecimal valorHoraSocioFeriadoMas12;

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

    public BigDecimal getValorHoraClienteSemanaMas12() {
        return valorHoraClienteSemanaMas12;
    }

    public void setValorHoraClienteSemanaMas12(BigDecimal valorHoraClienteSemanaMas12) {
        this.valorHoraClienteSemanaMas12 = valorHoraClienteSemanaMas12;
    }

    public BigDecimal getValorHoraClienteFindeMenos12() {
        return valorHoraClienteFindeMenos12;
    }

    public void setValorHoraClienteFindeMenos12(BigDecimal valorHoraClienteFindeMenos12) {
        this.valorHoraClienteFindeMenos12 = valorHoraClienteFindeMenos12;
    }

    public BigDecimal getValorHoraClienteFindeMas12() {
        return valorHoraClienteFindeMas12;
    }

    public void setValorHoraClienteFindeMas12(BigDecimal valorHoraClienteFindeMas12) {
        this.valorHoraClienteFindeMas12 = valorHoraClienteFindeMas12;
    }

    public BigDecimal getValorHoraClienteFeriadoMenos12() {
        return valorHoraClienteFeriadoMenos12;
    }

    public void setValorHoraClienteFeriadoMenos12(BigDecimal valorHoraClienteFeriadoMenos12) {
        this.valorHoraClienteFeriadoMenos12 = valorHoraClienteFeriadoMenos12;
    }

    public BigDecimal getValorHoraClienteFeriadoMas12() {
        return valorHoraClienteFeriadoMas12;
    }

    public void setValorHoraClienteFeriadoMas12(BigDecimal valorHoraClienteFeriadoMas12) {
        this.valorHoraClienteFeriadoMas12 = valorHoraClienteFeriadoMas12;
    }

    public BigDecimal getValorHoraSocio() {
        return valorHoraSocio;
    }

    public void setValorHoraSocio(BigDecimal valorHoraSocio) {
        this.valorHoraSocio = valorHoraSocio;
    }

    public BigDecimal getValorHoraSocioSemanaMas12() {
        return valorHoraSocioSemanaMas12;
    }

    public void setValorHoraSocioSemanaMas12(BigDecimal valorHoraSocioSemanaMas12) {
        this.valorHoraSocioSemanaMas12 = valorHoraSocioSemanaMas12;
    }

    public BigDecimal getValorHoraSocioFindeMenos12() {
        return valorHoraSocioFindeMenos12;
    }

    public void setValorHoraSocioFindeMenos12(BigDecimal valorHoraSocioFindeMenos12) {
        this.valorHoraSocioFindeMenos12 = valorHoraSocioFindeMenos12;
    }

    public BigDecimal getValorHoraSocioFindeMas12() {
        return valorHoraSocioFindeMas12;
    }

    public void setValorHoraSocioFindeMas12(BigDecimal valorHoraSocioFindeMas12) {
        this.valorHoraSocioFindeMas12 = valorHoraSocioFindeMas12;
    }

    public BigDecimal getValorHoraSocioFeriadoMenos12() {
        return valorHoraSocioFeriadoMenos12;
    }

    public void setValorHoraSocioFeriadoMenos12(BigDecimal valorHoraSocioFeriadoMenos12) {
        this.valorHoraSocioFeriadoMenos12 = valorHoraSocioFeriadoMenos12;
    }

    public BigDecimal getValorHoraSocioFeriadoMas12() {
        return valorHoraSocioFeriadoMas12;
    }

    public void setValorHoraSocioFeriadoMas12(BigDecimal valorHoraSocioFeriadoMas12) {
        this.valorHoraSocioFeriadoMas12 = valorHoraSocioFeriadoMas12;
    }
}
