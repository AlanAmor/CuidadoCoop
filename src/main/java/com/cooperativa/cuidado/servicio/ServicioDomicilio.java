package com.cooperativa.cuidado.servicio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "servicios_domicilio")
public class ServicioDomicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cliente · Día de semana (Lun a Vie) · Menos de 12 hs (< 12 hs) — valor base histórico */
    @NotNull
    @Positive
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorHora;

    /** Cliente · Día de semana (Lun a Vie) · 12 hs o más (>= 12 hs) */
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_hora_cliente_semana_mas12", precision = 12, scale = 2)
    private BigDecimal valorHoraClienteSemanaMas12;

    /** Cliente · Fin de semana (Sáb y Dom) · Menos de 12 hs (< 12 hs) */
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_hora_cliente_finde_menos12", precision = 12, scale = 2)
    private BigDecimal valorHoraClienteFindeMenos12;

    /** Cliente · Fin de semana (Sáb y Dom) · 12 hs o más (>= 12 hs) */
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_hora_cliente_finde_mas12", precision = 12, scale = 2)
    private BigDecimal valorHoraClienteFindeMas12;

    /** Cliente · Feriado nacional · Menos de 12 hs (< 12 hs) */
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_hora_cliente_feriado_menos12", precision = 12, scale = 2)
    private BigDecimal valorHoraClienteFeriadoMenos12;

    /** Cliente · Feriado nacional · 12 hs o más (>= 12 hs) */
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_hora_cliente_feriado_mas12", precision = 12, scale = 2)
    private BigDecimal valorHoraClienteFeriadoMas12;

    /** Socio · Día de semana (Lun a Vie) · Menos de 12 hs (< 12 hs) — valor base histórico */
    @NotNull
    @Positive
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(precision = 12, scale = 2)
    private BigDecimal valorHoraSocio;

    /** Socio · Día de semana (Lun a Vie) · 12 hs o más (>= 12 hs) */
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_hora_socio_semana_mas12", precision = 12, scale = 2)
    private BigDecimal valorHoraSocioSemanaMas12;

    /** Socio · Fin de semana (Sáb y Dom) · Menos de 12 hs (< 12 hs) */
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_hora_socio_finde_menos12", precision = 12, scale = 2)
    private BigDecimal valorHoraSocioFindeMenos12;

    /** Socio · Fin de semana (Sáb y Dom) · 12 hs o más (>= 12 hs) */
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_hora_socio_finde_mas12", precision = 12, scale = 2)
    private BigDecimal valorHoraSocioFindeMas12;

    /** Socio · Feriado nacional · Menos de 12 hs (< 12 hs) */
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_hora_socio_feriado_menos12", precision = 12, scale = 2)
    private BigDecimal valorHoraSocioFeriadoMenos12;

    /** Socio · Feriado nacional · 12 hs o más (>= 12 hs) */
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "valor_hora_socio_feriado_mas12", precision = 12, scale = 2)
    private BigDecimal valorHoraSocioFeriadoMas12;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "tipo_servicio_id", nullable = false)
    private TipoServicio tipoServicio;

    public ServicioDomicilio() {
    }

    public BigDecimal resolverValorHoraCliente(LocalDate fecha, BigDecimal horasTotales, boolean esFeriadoNacional) {
        return resolverValorHoraCliente(
                CategoriaDiaTarifa.desde(fecha, esFeriadoNacional),
                TramoHorarioTarifa.desdeHoras(horasTotales));
    }

    public BigDecimal resolverValorHoraCliente(CategoriaDiaTarifa categoriaDia, TramoHorarioTarifa tramo) {
        CategoriaDiaTarifa cat = categoriaDia != null ? categoriaDia : CategoriaDiaTarifa.DIA_SEMANA;
        TramoHorarioTarifa tr = tramo != null ? tramo : TramoHorarioTarifa.MENOS_DE_12_HS;
        boolean mas12 = tr == TramoHorarioTarifa.DOCE_HS_O_MAS;
        return switch (cat) {
            case DIA_SEMANA -> mas12
                    ? primeroNoNulo(valorHoraClienteSemanaMas12, valorHora)
                    : valorHora;
            case FIN_DE_SEMANA -> mas12
                    ? primeroNoNulo(valorHoraClienteFindeMas12, valorHoraClienteFindeMenos12, valorHoraClienteSemanaMas12, valorHora)
                    : primeroNoNulo(valorHoraClienteFindeMenos12, valorHora);
            case FERIADO_NACIONAL -> mas12
                    ? primeroNoNulo(valorHoraClienteFeriadoMas12, valorHoraClienteFeriadoMenos12, valorHoraClienteFindeMas12, valorHoraClienteSemanaMas12, valorHora)
                    : primeroNoNulo(valorHoraClienteFeriadoMenos12, valorHoraClienteFindeMenos12, valorHora);
        };
    }

    public BigDecimal resolverValorHoraSocio(LocalDate fecha, BigDecimal horasTotales, boolean esFeriadoNacional) {
        return resolverValorHoraSocio(
                CategoriaDiaTarifa.desde(fecha, esFeriadoNacional),
                TramoHorarioTarifa.desdeHoras(horasTotales));
    }

    public BigDecimal resolverValorHoraSocio(CategoriaDiaTarifa categoriaDia, TramoHorarioTarifa tramo) {
        CategoriaDiaTarifa cat = categoriaDia != null ? categoriaDia : CategoriaDiaTarifa.DIA_SEMANA;
        TramoHorarioTarifa tr = tramo != null ? tramo : TramoHorarioTarifa.MENOS_DE_12_HS;
        boolean mas12 = tr == TramoHorarioTarifa.DOCE_HS_O_MAS;
        BigDecimal baseSocio = primeroNoNulo(valorHoraSocio, valorHora);
        return switch (cat) {
            case DIA_SEMANA -> mas12
                    ? primeroNoNulo(valorHoraSocioSemanaMas12, baseSocio)
                    : baseSocio;
            case FIN_DE_SEMANA -> mas12
                    ? primeroNoNulo(valorHoraSocioFindeMas12, valorHoraSocioFindeMenos12, valorHoraSocioSemanaMas12, baseSocio)
                    : primeroNoNulo(valorHoraSocioFindeMenos12, baseSocio);
            case FERIADO_NACIONAL -> mas12
                    ? primeroNoNulo(valorHoraSocioFeriadoMas12, valorHoraSocioFeriadoMenos12, valorHoraSocioFindeMas12, valorHoraSocioSemanaMas12, baseSocio)
                    : primeroNoNulo(valorHoraSocioFeriadoMenos12, valorHoraSocioFindeMenos12, baseSocio);
        };
    }

    public BigDecimal getEfectivoClienteSemanaMas12() {
        return resolverValorHoraCliente(CategoriaDiaTarifa.DIA_SEMANA, TramoHorarioTarifa.DOCE_HS_O_MAS);
    }

    public BigDecimal getEfectivoClienteFindeMenos12() {
        return resolverValorHoraCliente(CategoriaDiaTarifa.FIN_DE_SEMANA, TramoHorarioTarifa.MENOS_DE_12_HS);
    }

    public BigDecimal getEfectivoClienteFindeMas12() {
        return resolverValorHoraCliente(CategoriaDiaTarifa.FIN_DE_SEMANA, TramoHorarioTarifa.DOCE_HS_O_MAS);
    }

    public BigDecimal getEfectivoClienteFeriadoMenos12() {
        return resolverValorHoraCliente(CategoriaDiaTarifa.FERIADO_NACIONAL, TramoHorarioTarifa.MENOS_DE_12_HS);
    }

    public BigDecimal getEfectivoClienteFeriadoMas12() {
        return resolverValorHoraCliente(CategoriaDiaTarifa.FERIADO_NACIONAL, TramoHorarioTarifa.DOCE_HS_O_MAS);
    }

    public BigDecimal getEfectivoSocioSemanaMenos12() {
        return resolverValorHoraSocio(CategoriaDiaTarifa.DIA_SEMANA, TramoHorarioTarifa.MENOS_DE_12_HS);
    }

    public BigDecimal getEfectivoSocioSemanaMas12() {
        return resolverValorHoraSocio(CategoriaDiaTarifa.DIA_SEMANA, TramoHorarioTarifa.DOCE_HS_O_MAS);
    }

    public BigDecimal getEfectivoSocioFindeMenos12() {
        return resolverValorHoraSocio(CategoriaDiaTarifa.FIN_DE_SEMANA, TramoHorarioTarifa.MENOS_DE_12_HS);
    }

    public BigDecimal getEfectivoSocioFindeMas12() {
        return resolverValorHoraSocio(CategoriaDiaTarifa.FIN_DE_SEMANA, TramoHorarioTarifa.DOCE_HS_O_MAS);
    }

    public BigDecimal getEfectivoSocioFeriadoMenos12() {
        return resolverValorHoraSocio(CategoriaDiaTarifa.FERIADO_NACIONAL, TramoHorarioTarifa.MENOS_DE_12_HS);
    }

    public BigDecimal getEfectivoSocioFeriadoMas12() {
        return resolverValorHoraSocio(CategoriaDiaTarifa.FERIADO_NACIONAL, TramoHorarioTarifa.DOCE_HS_O_MAS);
    }

    private static BigDecimal primeroNoNulo(BigDecimal... valores) {
        for (BigDecimal v : valores) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public TipoServicio getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(TipoServicio tipoServicio) {
        this.tipoServicio = tipoServicio;
    }
}
