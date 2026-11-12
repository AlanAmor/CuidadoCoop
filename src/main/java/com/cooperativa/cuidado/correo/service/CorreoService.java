package com.cooperativa.cuidado.correo.service;

import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.correo.ConfiguracionCorreo;
import com.cooperativa.cuidado.correo.ConfiguracionCorreoRepository;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import com.cooperativa.cuidado.liquidacionfamilia.LiquidacionFamilia;
import com.cooperativa.cuidado.liquidacionfamilia.service.LiquidacionFamiliaExportService;
import com.cooperativa.cuidado.recibo.ReciboRetribucion;
import com.cooperativa.cuidado.recibo.service.ReciboExportService;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Properties;

@Service
public class CorreoService {

    private final ConfiguracionCorreoRepository configRepository;
    private final ReciboExportService reciboExportService;
    private final LiquidacionFamiliaExportService familiaExportService;
    private final SocioRepository socioRepository;
    private final ClienteRepository clienteRepository;

    public CorreoService(
            ConfiguracionCorreoRepository configRepository,
            ReciboExportService reciboExportService,
            LiquidacionFamiliaExportService familiaExportService,
            SocioRepository socioRepository,
            ClienteRepository clienteRepository) {
        this.configRepository = configRepository;
        this.reciboExportService = reciboExportService;
        this.familiaExportService = familiaExportService;
        this.socioRepository = socioRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public ConfiguracionCorreo obtenerConfiguracion() {
        return configRepository.findById(ConfiguracionCorreo.ID_UNICO)
                .orElseGet(ConfiguracionCorreo::new);
    }

    @Transactional(readOnly = true)
    public boolean estaConfigurado() {
        return obtenerConfiguracion().tieneCredencialesCompletas();
    }

    @Transactional
    public ConfiguracionCorreo guardarConfiguracion(
            String host,
            Integer puerto,
            String username,
            String password,
            String remitenteNombre,
            String remitenteEmail,
            boolean usarTls,
            boolean usarSsl,
            String asuntoSocio,
            String cuerpoSocio,
            String asuntoCliente,
            String cuerpoCliente) {
        if (host == null || host.trim().isEmpty()) {
            throw new DatosInvalidosException("Ingresá el servidor SMTP (por ejemplo smtp.gmail.com).");
        }
        if (puerto == null || puerto <= 0 || puerto > 65535) {
            throw new DatosInvalidosException("Ingresá un puerto SMTP válido (por ejemplo 587 o 465).");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new DatosInvalidosException("Ingresá el correo o usuario de la cuenta emisora.");
        }

        ConfiguracionCorreo cfg = configRepository.findById(ConfiguracionCorreo.ID_UNICO)
                .orElseGet(ConfiguracionCorreo::new);
        cfg.setId(ConfiguracionCorreo.ID_UNICO);
        cfg.setHost(host.trim());
        cfg.setPuerto(puerto);
        cfg.setUsername(username.trim());

        if (password != null && !password.trim().isEmpty()) {
            cfg.setPassword(password.trim().replace(" ", ""));
        } else if (cfg.getPassword() == null || cfg.getPassword().isBlank()) {
            throw new DatosInvalidosException("Ingresá la contraseña o contraseña de aplicación del correo.");
        }

        cfg.setRemitenteNombre(textoODefecto(remitenteNombre, "Cooperativa de Trabajo Grupo Azul Ltda."));
        cfg.setRemitenteEmail(remitenteEmail != null && !remitenteEmail.trim().isEmpty()
                ? remitenteEmail.trim()
                : username.trim());
        cfg.setUsarTls(usarTls);
        cfg.setUsarSsl(usarSsl);
        cfg.setAsuntoSocioPlantilla(textoODefecto(asuntoSocio, ConfiguracionCorreo.ASUNTO_SOCIO_DEFECTO));
        cfg.setCuerpoSocioPlantilla(textoODefecto(cuerpoSocio, ConfiguracionCorreo.CUERPO_SOCIO_DEFECTO));
        cfg.setAsuntoClientePlantilla(textoODefecto(asuntoCliente, ConfiguracionCorreo.ASUNTO_CLIENTE_DEFECTO));
        cfg.setCuerpoClientePlantilla(textoODefecto(cuerpoCliente, ConfiguracionCorreo.CUERPO_CLIENTE_DEFECTO));

        return configRepository.save(cfg);
    }

    public void enviarCorreoPrueba(String destinatario) {
        String destino = validarCorreoDestino(destinatario);
        ConfiguracionCorreo cfg = exigirConfiguracionLista();
        enviarMensaje(
                cfg,
                destino,
                "Correo de prueba - CuidadoCoop",
                "¡Hola!\n\nEste es un correo de prueba enviado desde CuidadoCoop.\n"
                        + "La configuración SMTP (" + cfg.getHost() + ":" + cfg.getPuerto()
                        + " · " + cfg.getUsername() + ") funciona correctamente.",
                null,
                null,
                null,
                null);
    }

    @Transactional
    public String enviarReciboSocio(ReciboRetribucion recibo, String correoExplicito, String formatoAdjunto) {
        if (recibo == null) {
            throw new DatosInvalidosException("No se encontró el recibo a enviar.");
        }
        ConfiguracionCorreo cfg = exigirConfiguracionLista();

        String correoSocioActual = recibo.getSocio() != null ? recibo.getSocio().getCorreo() : null;
        String destino = (correoExplicito != null && !correoExplicito.trim().isEmpty())
                ? validarCorreoDestino(correoExplicito)
                : validarCorreoDestino(correoSocioActual);

        if (recibo.getSocio() != null && (correoSocioActual == null || correoSocioActual.isBlank())) {
            Socio s = recibo.getSocio();
            s.setCorreo(destino);
            socioRepository.save(s);
        }

        String montoTexto = formatearMoneda(recibo.getRetribucionNeta());
        String asunto = reemplazarVariablesSocio(cfg.getAsuntoSocioPlantilla(), recibo, montoTexto);
        String cuerpo = reemplazarVariablesSocio(cfg.getCuerpoSocioPlantilla(), recibo, montoTexto);

        String fmt = normalizarFormato(formatoAdjunto);
        byte[] pdfBytes = ("PDF".equals(fmt) || "AMBOS".equals(fmt))
                ? reciboExportService.generarPdfDesdeEntidad(recibo)
                : null;
        String pdfNombre = "recibo-retribucion-" + recibo.getNumeroCeros() + ".pdf";

        byte[] excelBytes = ("EXCEL".equals(fmt) || "AMBOS".equals(fmt))
                ? reciboExportService.generarExcelDesdeEntidad(recibo)
                : null;
        String excelNombre = "recibo-retribucion-" + recibo.getNumeroCeros() + ".xlsx";

        enviarMensaje(cfg, destino, asunto, cuerpo, pdfNombre, pdfBytes, excelNombre, excelBytes);
        return destino;
    }

    @Transactional
    public String enviarLiquidacionFamilia(LiquidacionFamilia liq, String correoExplicito, String formatoAdjunto) {
        if (liq == null) {
            throw new DatosInvalidosException("No se encontró la liquidación familiar a enviar.");
        }
        ConfiguracionCorreo cfg = exigirConfiguracionLista();

        String correoClienteActual = liq.getCliente() != null ? liq.getCliente().getCorreo() : null;
        String destino = (correoExplicito != null && !correoExplicito.trim().isEmpty())
                ? validarCorreoDestino(correoExplicito)
                : validarCorreoDestino(correoClienteActual);

        if (liq.getCliente() != null && (correoClienteActual == null || correoClienteActual.isBlank())) {
            Cliente c = liq.getCliente();
            c.setCorreo(destino);
            clienteRepository.save(c);
        }

        String montoTexto = formatearMoneda(liq.getSaldoATransferir());
        String asunto = reemplazarVariablesCliente(cfg.getAsuntoClientePlantilla(), liq, montoTexto);
        String cuerpo = reemplazarVariablesCliente(cfg.getCuerpoClientePlantilla(), liq, montoTexto);

        String fmt = normalizarFormato(formatoAdjunto);
        String baseArchivo = nombreBaseLiquidacionFamilia(liq);
        byte[] pdfBytes = ("PDF".equals(fmt) || "AMBOS".equals(fmt))
                ? familiaExportService.generarPdfDesdeEntidad(liq)
                : null;
        String pdfNombre = baseArchivo + ".pdf";

        byte[] excelBytes = ("EXCEL".equals(fmt) || "AMBOS".equals(fmt))
                ? familiaExportService.generarExcelDesdeEntidad(liq)
                : null;
        String excelNombre = baseArchivo + ".xlsx";

        enviarMensaje(cfg, destino, asunto, cuerpo, pdfNombre, pdfBytes, excelNombre, excelBytes);
        return destino;
    }

    private void enviarMensaje(
            ConfiguracionCorreo cfg,
            String destinatario,
            String asunto,
            String cuerpoTexto,
            String nombrePdf,
            byte[] bytesPdf,
            String nombreExcel,
            byte[] bytesExcel) {
        try {
            JavaMailSenderImpl sender = construirMailSender(cfg);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            String fromEmail = (cfg.getRemitenteEmail() != null && !cfg.getRemitenteEmail().isBlank())
                    ? cfg.getRemitenteEmail().trim()
                    : cfg.getUsername().trim();
            String fromName = (cfg.getRemitenteNombre() != null && !cfg.getRemitenteNombre().isBlank())
                    ? cfg.getRemitenteNombre().trim()
                    : fromEmail;

            helper.setFrom(new InternetAddress(fromEmail, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoTexto, false);

            if (bytesPdf != null && nombrePdf != null) {
                helper.addAttachment(nombrePdf, new ByteArrayResource(bytesPdf), "application/pdf");
            }
            if (bytesExcel != null && nombreExcel != null) {
                helper.addAttachment(
                        nombreExcel,
                        new ByteArrayResource(bytesExcel),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            }

            sender.send(message);
        } catch (Exception ex) {
            String detalle = ex.getMessage() != null ? ex.getMessage() : "Error desconocido del servidor SMTP";
            throw new ReglaNegocioException(
                    "No se pudo enviar el correo a " + destinatario
                            + ". Verificá los datos en Configurar Correo (si usás Gmail recordá usar una Contraseña de Aplicación de 16 letras). Detalle: "
                            + detalle);
        }
    }

    private JavaMailSenderImpl construirMailSender(ConfiguracionCorreo cfg) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(cfg.getHost().trim());
        sender.setPort(cfg.getPuerto());
        sender.setUsername(cfg.getUsername().trim());
        sender.setPassword(cfg.getPassword());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(cfg.isUsarTls()));
        props.put("mail.smtp.starttls.required", String.valueOf(cfg.isUsarTls()));
        props.put("mail.smtp.ssl.enable", String.valueOf(cfg.isUsarSsl()));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");
        return sender;
    }

    private ConfiguracionCorreo exigirConfiguracionLista() {
        ConfiguracionCorreo cfg = obtenerConfiguracion();
        if (!cfg.tieneCredencialesCompletas()) {
            throw new ReglaNegocioException(
                    "El envío de correos todavía no está configurado. Configuralo primero desde el botón \"Configurar Correo\".");
        }
        return cfg;
    }

    private static String validarCorreoDestino(String correo) {
        if (correo == null || correo.trim().isEmpty()) {
            throw new DatosInvalidosException(
                    "Ingresá una dirección de correo electrónico de destino (o cargala en la ficha del socio/cliente).");
        }
        String limpio = correo.trim();
        if (!limpio.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new DatosInvalidosException("La dirección de correo \"" + limpio + "\" no tiene un formato válido.");
        }
        return limpio;
    }

    private static String normalizarFormato(String formato) {
        if (formato == null || formato.isBlank()) {
            return "PDF";
        }
        String mayus = formato.trim().toUpperCase(Locale.ROOT);
        return switch (mayus) {
            case "EXCEL", "XLSX" -> "EXCEL";
            case "AMBOS", "PDF_EXCEL" -> "AMBOS";
            default -> "PDF";
        };
    }

    private static String reemplazarVariablesSocio(String plantilla, ReciboRetribucion r, String monto) {
        String base = plantilla != null ? plantilla : "";
        return base
                .replace("{nombre}", r.getNombreApellidoSocio() != null ? r.getNombreApellidoSocio() : "")
                .replace("{numero}", r.getNumeroFormateado() != null ? r.getNumeroFormateado() : "")
                .replace("{periodo}", r.getPeriodoTexto() != null ? r.getPeriodoTexto() : "")
                .replace("{monto}", monto);
    }

    private static String reemplazarVariablesCliente(String plantilla, LiquidacionFamilia l, String monto) {
        String base = plantilla != null ? plantilla : "";
        String familiar = (l.getCliente() != null && l.getCliente().getNombreFamiliar() != null)
                ? l.getCliente().getNombreFamiliar()
                : "";
        return base
                .replace("{paciente}", l.getNombrePaciente() != null ? l.getNombrePaciente() : "")
                .replace("{familiar}", familiar)
                .replace("{periodo}", l.getPeriodoEtiqueta() != null ? l.getPeriodoEtiqueta() : "")
                .replace("{monto}", monto);
    }

    private static String formatearMoneda(BigDecimal valor) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.forLanguageTag("es-AR"));
        sym.setGroupingSeparator('.');
        sym.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0.00", sym);
        return df.format(valor != null ? valor : BigDecimal.ZERO);
    }

    private static String nombreBaseLiquidacionFamilia(LiquidacionFamilia liq) {
        String basePaciente = liq.getNombrePaciente() != null ? liq.getNombrePaciente() : "paciente";
        String limpio = Normalizer.normalize(basePaciente, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (limpio.isEmpty()) {
            limpio = "paciente";
        }
        return "liquidacion-familia-" + limpio + "-" + liq.getPeriodo();
    }

    private static String textoODefecto(String valor, String defecto) {
        return (valor == null || valor.trim().isEmpty()) ? defecto : valor.trim();
    }
}
