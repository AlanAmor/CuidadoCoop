package com.cooperativa.cuidado.correo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracion_correo")
public class ConfiguracionCorreo {

    public static final Long ID_UNICO = 1L;

    public static final String ASUNTO_SOCIO_DEFECTO =
            "Recibo de Retribución {numero} - {periodo} | Cooperativa Grupo Azul Ltda.";
    public static final String CUERPO_SOCIO_DEFECTO =
            "Hola {nombre},\n\n"
            + "Te enviamos adjunto tu Recibo de Retribución {numero} correspondiente al período {periodo} "
            + "por un importe neto a cobrar de $ {monto}.\n\n"
            + "Saludos cordiales,\n"
            + "Cooperativa de Trabajo Grupo Azul Limitada.";

    public static final String ASUNTO_CLIENTE_DEFECTO =
            "Liquidación Mensual de Servicio - {paciente} ({periodo}) | Grupo Azul Ltda.";
    public static final String CUERPO_CLIENTE_DEFECTO =
            "Estimada familia de {paciente},\n\n"
            + "Adjuntamos la liquidación mensual del servicio de cuidado domiciliario correspondiente a {periodo} "
            + "(Saldo a transferir: $ {monto}).\n\n"
            + "Datos para transferencia:\n"
            + "- Banco: BICA\n"
            + "- Titular: COOPERATIVA DE TRABAJO GRUPO AZUL LIMITADA\n"
            + "- CBU: 4260010100200097013013\n"
            + "- CUIT: 30-71745352-9\n\n"
            + "¡Muchas gracias por su confianza!\n"
            + "Cooperativa de Trabajo Grupo Azul Limitada.";

    @Id
    private Long id = ID_UNICO;

    @Column(nullable = false, length = 150)
    private String host = "smtp.gmail.com";

    @Column(nullable = false)
    private Integer puerto = 587;

    @Column(nullable = false, length = 200)
    private String username = "";

    @Column(nullable = false, length = 250)
    private String password = "";

    @Column(nullable = false, length = 160)
    private String remitenteNombre = "Cooperativa de Trabajo Grupo Azul Ltda.";

    @Column(nullable = false, length = 200)
    private String remitenteEmail = "";

    @Column(nullable = false)
    private boolean usarTls = true;

    @Column(nullable = false)
    private boolean usarSsl = false;

    @Column(nullable = false, length = 250)
    private String asuntoSocioPlantilla = ASUNTO_SOCIO_DEFECTO;

    @Column(nullable = false, length = 2000)
    private String cuerpoSocioPlantilla = CUERPO_SOCIO_DEFECTO;

    @Column(nullable = false, length = 250)
    private String asuntoClientePlantilla = ASUNTO_CLIENTE_DEFECTO;

    @Column(nullable = false, length = 2000)
    private String cuerpoClientePlantilla = CUERPO_CLIENTE_DEFECTO;

    public ConfiguracionCorreo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPuerto() {
        return puerto;
    }

    public void setPuerto(Integer puerto) {
        this.puerto = puerto;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRemitenteNombre() {
        return remitenteNombre;
    }

    public void setRemitenteNombre(String remitenteNombre) {
        this.remitenteNombre = remitenteNombre;
    }

    public String getRemitenteEmail() {
        return remitenteEmail;
    }

    public void setRemitenteEmail(String remitenteEmail) {
        this.remitenteEmail = remitenteEmail;
    }

    public boolean isUsarTls() {
        return usarTls;
    }

    public void setUsarTls(boolean usarTls) {
        this.usarTls = usarTls;
    }

    public boolean isUsarSsl() {
        return usarSsl;
    }

    public void setUsarSsl(boolean usarSsl) {
        this.usarSsl = usarSsl;
    }

    public String getAsuntoSocioPlantilla() {
        return asuntoSocioPlantilla;
    }

    public void setAsuntoSocioPlantilla(String asuntoSocioPlantilla) {
        this.asuntoSocioPlantilla = asuntoSocioPlantilla;
    }

    public String getCuerpoSocioPlantilla() {
        return cuerpoSocioPlantilla;
    }

    public void setCuerpoSocioPlantilla(String cuerpoSocioPlantilla) {
        this.cuerpoSocioPlantilla = cuerpoSocioPlantilla;
    }

    public String getAsuntoClientePlantilla() {
        return asuntoClientePlantilla;
    }

    public void setAsuntoClientePlantilla(String asuntoClientePlantilla) {
        this.asuntoClientePlantilla = asuntoClientePlantilla;
    }

    public String getCuerpoClientePlantilla() {
        return cuerpoClientePlantilla;
    }

    public void setCuerpoClientePlantilla(String cuerpoClientePlantilla) {
        this.cuerpoClientePlantilla = cuerpoClientePlantilla;
    }

    public boolean tieneCredencialesCompletas() {
        return host != null && !host.isBlank()
                && puerto != null && puerto > 0
                && username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }
}
