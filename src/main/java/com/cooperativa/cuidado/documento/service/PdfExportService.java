package com.cooperativa.cuidado.documento.service;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.documento.AlicuotaIva;
import com.cooperativa.cuidado.documento.CalculoPresupuesto;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.liquidacion.dto.LiquidacionMensualSocio;
import com.cooperativa.cuidado.liquidacion.dto.ResumenDomicilioLiquidacion;
import com.cooperativa.cuidado.liquidacion.service.LiquidacionService;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class PdfExportService {

    private static final String NOMBRE_COOPERATIVA = "Cooperativa de Cuidados";
    private static final Font TITULO = crearFuente(18, Font.BOLD);
    private static final Font SUBTITULO = crearFuente(12, Font.BOLD);
    private static final Font NORMAL = crearFuente(10, Font.NORMAL);
    private static final Font PEQUENA = crearFuente(8, Font.NORMAL);

    private final LiquidacionService liquidacionService;
    private final SocioRepository socioRepository;
    private final ServicioDomicilioRepository servicioRepository;
    private final ClienteRepository clienteRepository;
    private final AsistenciaRepository asistenciaRepository;

    public PdfExportService(
            LiquidacionService liquidacionService,
            SocioRepository socioRepository,
            ServicioDomicilioRepository servicioRepository,
            ClienteRepository clienteRepository,
            AsistenciaRepository asistenciaRepository) {
        this.liquidacionService = liquidacionService;
        this.socioRepository = socioRepository;
        this.servicioRepository = servicioRepository;
        this.clienteRepository = clienteRepository;
        this.asistenciaRepository = asistenciaRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generarComprobanteRetorno(Long socioId, YearMonth periodo) {
        Socio socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el socio con ID " + socioId));
        LiquidacionMensualSocio liquidacion = liquidacionService.calcularRetornoMensual(socioId, periodo);
        return generarPdf(document -> {
            agregarCabecera(document, "Comprobante de retorno mensual");
            agregarLinea(document, "Período: " + periodo);
            agregarLinea(document, "Socio: " + socio.getApellido() + ", " + socio.getNombre());
            agregarLinea(document, "DNI: " + socio.getDni());
            agregarLinea(document, "CBU: " + socio.getCbu());
            document.add(Chunk.NEWLINE);
            agregarTablaRetorno(document, liquidacion);
            document.add(Chunk.NEWLINE);
            Paragraph total = new Paragraph(
                    "Total de horas: " + formato(liquidacion.totalHoras())
                            + "     Total retorno: " + moneda(liquidacion.totalRetorno()),
                    SUBTITULO);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);
            document.add(Chunk.NEWLINE);
            agregarNotaOrientativa(document,
                    "Modelo orientativo de liquidación. No constituye una plantilla oficial ni acredita homologación INAES.");
            agregarLinea(document, "Firma del socio: ____________________     Firma de la cooperativa: ____________________");
        });
    }

    @Transactional(readOnly = true)
    public byte[] generarPresupuesto(Long clienteId, Long servicioId, BigDecimal horasEstimadas) {
        return generarPresupuesto(clienteId, servicioId, horasEstimadas, AlicuotaIva.SIN_IVA, false);
    }

    @Transactional(readOnly = true)
    public byte[] generarPresupuesto(
            Long clienteId, Long servicioId, BigDecimal horasEstimadas, AlicuotaIva alicuota, boolean pagoEfectivo) {
        ServicioDomicilio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el servicio con ID " + servicioId));
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el cliente con ID " + clienteId));
        CalculoPresupuesto calculo = CalculoPresupuesto.calcular(
                horasEstimadas, servicio.getValorHora(), alicuota, pagoEfectivo);
        BigDecimal total = calculo.total();
        LocalDate fechaEmision = LocalDate.now();

        return generarPdf(document -> {
            agregarCabecera(document, "Presupuesto de servicio de cuidado");
            agregarLinea(document, "Presupuesto: PRES-" + servicioId + "-" + fechaEmision);
            agregarLinea(document, "Fecha de emisión: " + fechaEmision.format(DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy", Locale.forLanguageTag("es-AR"))));
            agregarLinea(document, "Cliente / familiar: " + cliente.getNombreFamiliar());
            agregarLinea(document, "Paciente: " + cliente.getNombrePaciente());
            agregarLinea(document, "Domicilio: " + cliente.getDomicilio());
            agregarLinea(document, "Tipo de servicio: " + servicio.getTipoServicio().getEtiqueta());
            agregarLinea(document, "Forma de pago: " + (pagoEfectivo
                    ? "Efectivo (10% de descuento)" : "Otros medios (sin descuento)"));
            document.add(Chunk.NEWLINE);

            PdfPTable tabla = crearTabla(new float[]{4, 1.3f, 1.8f, 1.8f});
            agregarEncabezado(tabla, "Concepto");
            agregarEncabezado(tabla, "Horas");
            agregarEncabezado(tabla, "Valor hora");
            agregarEncabezado(tabla, "Importe");
            agregarCelda(tabla, "Servicio de cuidado domiciliario", false);
            agregarCelda(tabla, formato(horasEstimadas), false);
            agregarCelda(tabla, moneda(servicio.getValorHora()), false);
            agregarCelda(tabla, moneda(calculo.subtotal()), false);
            document.add(tabla);

            PdfPTable desglose = crearTabla(new float[]{6, 2});
            agregarFilaDesglose(desglose, "Subtotal", moneda(calculo.subtotal()));
            if (pagoEfectivo) {
                agregarFilaDesglose(desglose, "Descuento por pago en efectivo (10%)",
                        "- " + moneda(calculo.descuento()));
                agregarFilaDesglose(desglose, "Subtotal con descuento", moneda(calculo.neto()));
            }
            if (alicuota != null && alicuota != AlicuotaIva.SIN_IVA) {
                agregarFilaDesglose(desglose, alicuota.getEtiqueta(), moneda(calculo.iva()));
            }
            document.add(desglose);

            Paragraph totalPresupuesto = new Paragraph("Total estimado: " + moneda(total), SUBTITULO);
            totalPresupuesto.setAlignment(Element.ALIGN_RIGHT);
            totalPresupuesto.setSpacingBefore(8);
            document.add(totalPresupuesto);
            agregarLinea(document, "Validez del presupuesto: 15 días corridos desde la fecha de emisión.");
            document.add(Chunk.NEWLINE);
            agregarTroquel(document, servicio, cliente, horasEstimadas, total, fechaEmision);
            agregarNotaOrientativa(document,
                    "Presupuesto orientativo sujeto a confirmación de disponibilidad y condiciones del servicio. No es un comprobante fiscal.");
        });
    }

    @Transactional(readOnly = true)
    public byte[] exportarHistoricoAsistenciasPdf(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return historicoPdf("Histórico de asistencias", null, desde, hasta, asistenciaRepository
                .findAllByFechaBetweenOrderByFechaAscHoraInicioAsc(desde, hasta));
    }

    @Transactional(readOnly = true)
    public byte[] exportarHistoricoSocioPdf(Long socioId, LocalDate desde, LocalDate hasta) {
        Socio socio = buscarSocio(socioId);
        LocalDate d = desdeOTodo(desde);
        LocalDate h = hastaOTodo(hasta);
        validarRango(d, h);
        return historicoPdf("Histórico de asistencias del socio",
                "Socio: " + socio.getApellido() + ", " + socio.getNombre() + " (DNI " + socio.getDni() + ")",
                desde == null && hasta == null ? null : d, h,
                asistenciaRepository.findAllBySocioIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(socioId, d, h));
    }

    @Transactional(readOnly = true)
    public byte[] exportarHistoricoClientePdf(Long clienteId, LocalDate desde, LocalDate hasta) {
        Cliente cliente = buscarCliente(clienteId);
        LocalDate d = desdeOTodo(desde);
        LocalDate h = hastaOTodo(hasta);
        validarRango(d, h);
        return historicoPdf("Histórico de servicios del cliente",
                "Paciente: " + cliente.getNombrePaciente() + " | Familiar: " + cliente.getNombreFamiliar()
                        + " | Domicilio: " + cliente.getDomicilio(),
                desde == null && hasta == null ? null : d, h,
                asistenciaRepository.findAllByClienteIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(clienteId, d, h));
    }

    @Transactional(readOnly = true)
    public byte[] exportarHistoricoSocioExcel(Long socioId, LocalDate desde, LocalDate hasta) {
        Socio socio = buscarSocio(socioId);
        LocalDate d = desdeOTodo(desde);
        LocalDate h = hastaOTodo(hasta);
        validarRango(d, h);
        return historicoExcel("Histórico de asistencias del socio " + socio.getApellido() + ", " + socio.getNombre(),
                d, h, asistenciaRepository.findAllBySocioIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(socioId, d, h));
    }

    @Transactional(readOnly = true)
    public byte[] exportarHistoricoClienteExcel(Long clienteId, LocalDate desde, LocalDate hasta) {
        Cliente cliente = buscarCliente(clienteId);
        LocalDate d = desdeOTodo(desde);
        LocalDate h = hastaOTodo(hasta);
        validarRango(d, h);
        return historicoExcel("Histórico de servicios del cliente " + cliente.getNombrePaciente(),
                d, h, asistenciaRepository.findAllByClienteIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(clienteId, d, h));
    }

    private Socio buscarSocio(Long id) {
        return socioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(
                "El socio seleccionado ya no existe. Actualizá la lista e intentá nuevamente."));
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(
                "El cliente seleccionado ya no existe. Actualizá la lista e intentá nuevamente."));
    }

    private static LocalDate desdeOTodo(LocalDate desde) {
        return desde != null ? desde : LocalDate.of(2000, 1, 1);
    }

    private static LocalDate hastaOTodo(LocalDate hasta) {
        return hasta != null ? hasta : LocalDate.of(2999, 12, 31);
    }

    private byte[] historicoPdf(
            String titulo, String detalle, LocalDate desde, LocalDate hasta, List<Asistencia> asistencias) {
        return generarPdf(document -> {
            agregarCabecera(document, titulo);
            if (detalle != null) {
                agregarLinea(document, detalle);
            }
            agregarLinea(document, desde == null && hasta.getYear() == 2999
                    ? "Período: todo el historial"
                    : "Período: " + (desde != null ? desde : "inicio") + " al "
                            + (hasta.getYear() == 2999 ? "hoy y futuro" : hasta));
            agregarLinea(document, "Registros: " + asistencias.size() + " | Horas totales: "
                    + formato(asistencias.stream().map(Asistencia::getHorasTotales)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)));
            document.add(Chunk.NEWLINE);

            PdfPTable tabla = crearTabla(new float[]{1.3f, 2.2f, 1.2f, 2.1f, 1.6f, 1.1f, 1.4f});
            agregarEncabezado(tabla, "Fecha");
            agregarEncabezado(tabla, "Socio");
            agregarEncabezado(tabla, "DNI");
            agregarEncabezado(tabla, "Paciente / domicilio");
            agregarEncabezado(tabla, "Turno");
            agregarEncabezado(tabla, "Horas");
            agregarEncabezado(tabla, "Valor hora");
            for (Asistencia asistencia : asistencias) {
                agregarCelda(tabla, asistencia.getFecha().toString(), false);
                agregarCelda(tabla, asistencia.getSocio().getApellido() + ", "
                        + asistencia.getSocio().getNombre(), false);
                agregarCelda(tabla, asistencia.getSocio().getDni(), false);
                agregarCelda(tabla, asistencia.getCliente().getNombrePaciente()
                        + " / " + asistencia.getCliente().getDomicilio(), false);
                agregarCelda(tabla, asistencia.getHoraInicio() + " - " + asistencia.getHoraFin(), false);
                agregarCelda(tabla, formato(asistencia.getHorasTotales()), false);
                agregarCelda(tabla, moneda(asistencia.getValorHoraAplicado()), false);
            }
            document.add(tabla);
            agregarNotaOrientativa(document, "Documento de auditoría generado el " + LocalDate.now() + ".");
        });
    }

    @Transactional(readOnly = true)
    public byte[] exportarHistoricoAsistenciasExcel(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return historicoExcel("Histórico de asistencias", desde, hasta, asistenciaRepository
                .findAllByFechaBetweenOrderByFechaAscHoraInicioAsc(desde, hasta));
    }

    private byte[] historicoExcel(String tituloHistorico, LocalDate desde, LocalDate hasta, List<Asistencia> asistencias) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = workbook.createSheet("Histórico");
            Row titulo = hoja.createRow(0);
            titulo.createCell(0).setCellValue(tituloHistorico);
            Row periodo = hoja.createRow(1);
            periodo.createCell(0).setCellValue("Desde");
            periodo.createCell(1).setCellValue(desde.getYear() == 2000 ? "inicio" : desde.toString());
            periodo.createCell(2).setCellValue("Hasta");
            periodo.createCell(3).setCellValue(hasta.getYear() == 2999 ? "sin límite" : hasta.toString());

            String[] cabeceras = {
                    "Fecha", "Socio", "DNI", "Cliente/familiar", "Paciente",
                    "Domicilio", "Tipo de servicio", "Hora inicio", "Hora fin",
                    "Horas", "Valor hora aplicado", "Importe estimado"
            };
            Row filaCabecera = hoja.createRow(3);
            CellStyle estiloCabecera = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font fuenteCabecera = workbook.createFont();
            fuenteCabecera.setBold(true);
            estiloCabecera.setFont(fuenteCabecera);
            for (int columna = 0; columna < cabeceras.length; columna++) {
                Cell celda = filaCabecera.createCell(columna);
                celda.setCellValue(cabeceras[columna]);
                celda.setCellStyle(estiloCabecera);
            }

            int indiceFila = 4;
            for (Asistencia asistencia : asistencias) {
                Row fila = hoja.createRow(indiceFila++);
                fila.createCell(0).setCellValue(asistencia.getFecha().toString());
                fila.createCell(1).setCellValue(asistencia.getSocio().getApellido()
                        + ", " + asistencia.getSocio().getNombre());
                fila.createCell(2).setCellValue(asistencia.getSocio().getDni());
                fila.createCell(3).setCellValue(asistencia.getCliente().getNombreFamiliar());
                fila.createCell(4).setCellValue(asistencia.getCliente().getNombrePaciente());
                fila.createCell(5).setCellValue(asistencia.getCliente().getDomicilio());
                fila.createCell(6).setCellValue(asistencia.getServicioDomicilio().getTipoServicio().getNombre());
                fila.createCell(7).setCellValue(asistencia.getHoraInicio().toString());
                fila.createCell(8).setCellValue(asistencia.getHoraFin().toString());
                fila.createCell(9).setCellValue(asistencia.getHorasTotales().doubleValue());
                fila.createCell(10).setCellValue(asistencia.getValorHoraAplicado().doubleValue());
                fila.createCell(11).setCellValue(asistencia.getHorasTotales()
                        .multiply(asistencia.getValorHoraAplicado()).doubleValue());
            }
            for (int columna = 0; columna < cabeceras.length; columna++) {
                hoja.autoSizeColumn(columna);
            }
            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el archivo Excel del histórico", ex);
        }
    }

    private byte[] generarPdf(ContenidoPdf contenido) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document documento = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(documento, salida);
            documento.open();
            contenido.escribir(documento);
            documento.close();
            return salida.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("No se pudo generar el documento PDF", ex);
        }
    }

    private void agregarCabecera(Document documento, String titulo) throws DocumentException {
        Paragraph cooperativa = new Paragraph(NOMBRE_COOPERATIVA, TITULO);
        cooperativa.setAlignment(Element.ALIGN_CENTER);
        documento.add(cooperativa);
        Paragraph subtitulo = new Paragraph(titulo, SUBTITULO);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(subtitulo);
        documento.add(new Paragraph(" ", NORMAL));
    }

    private void agregarLinea(Document documento, String texto) throws DocumentException {
        documento.add(new Paragraph(texto, NORMAL));
    }

    private void agregarNotaOrientativa(Document documento, String texto) throws DocumentException {
        Paragraph nota = new Paragraph(texto, PEQUENA);
        nota.setSpacingBefore(14);
        documento.add(nota);
    }

    private void agregarTablaRetorno(Document documento, LiquidacionMensualSocio liquidacion)
            throws DocumentException {
        PdfPTable tabla = crearTabla(new float[]{3.5f, 1.2f, 1.8f});
        agregarEncabezado(tabla, "Servicio / domicilio");
        agregarEncabezado(tabla, "Horas");
        agregarEncabezado(tabla, "Importe");
        for (ResumenDomicilioLiquidacion domicilio : liquidacion.domicilios()) {
            agregarCelda(tabla, domicilio.tipoServicio() + " - " + domicilio.domicilio(), false);
            agregarCelda(tabla, formato(domicilio.horas()), false);
            agregarCelda(tabla, moneda(domicilio.importe()), false);
        }
        if (liquidacion.domicilios().isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase("Sin asistencias registradas en el período.", NORMAL));
            vacia.setColspan(3);
            vacia.setPadding(7);
            tabla.addCell(vacia);
        }
        documento.add(tabla);
    }

    private void agregarTroquel(
            Document documento,
            ServicioDomicilio servicio,
            Cliente cliente,
            BigDecimal horas,
            BigDecimal total,
            LocalDate fecha) throws DocumentException {
        PdfPTable tabla = crearTabla(new float[]{1});
        PdfPCell troquel = new PdfPCell();
        troquel.setBorder(PdfPCell.TOP);
        troquel.setBorderWidthTop(1);
        troquel.setBorderColorTop(new java.awt.Color(130, 145, 138));
        troquel.setPaddingTop(12);
        Paragraph titulo = new Paragraph("TROQUEL - COPIA PARA EL CLIENTE", SUBTITULO);
        titulo.setAlignment(Element.ALIGN_CENTER);
        troquel.addElement(titulo);
        troquel.addElement(new Paragraph(
                NOMBRE_COOPERATIVA + " | Presupuesto PRES-" + servicio.getId() + "-" + fecha
                        + " | " + cliente.getNombrePaciente(),
                NORMAL));
        troquel.addElement(new Paragraph(
                "Domicilio: " + cliente.getDomicilio()
                        + " | Horas estimadas: " + formato(horas)
                        + " | Total: " + moneda(total),
                NORMAL));
        tabla.addCell(troquel);
        tabla.setSpacingBefore(20);
        documento.add(tabla);
    }

    private PdfPTable crearTabla(float[] anchos) throws DocumentException {
        PdfPTable tabla = new PdfPTable(anchos);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(8);
        return tabla;
    }

    private void agregarEncabezado(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, SUBTITULO));
        celda.setPadding(7);
        celda.setBackgroundColor(new java.awt.Color(231, 243, 238));
        tabla.addCell(celda);
    }

    private void agregarCelda(PdfPTable tabla, String texto, boolean encabezado) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, encabezado ? SUBTITULO : NORMAL));
        celda.setPadding(6);
        tabla.addCell(celda);
    }

    private void agregarFilaDesglose(PdfPTable tabla, String concepto, String importe) {
        PdfPCell etiqueta = new PdfPCell(new Phrase(concepto, NORMAL));
        etiqueta.setBorder(PdfPCell.NO_BORDER);
        etiqueta.setHorizontalAlignment(Element.ALIGN_RIGHT);
        etiqueta.setPadding(3);
        PdfPCell valor = new PdfPCell(new Phrase(importe, NORMAL));
        valor.setBorder(PdfPCell.NO_BORDER);
        valor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valor.setPadding(3);
        tabla.addCell(etiqueta);
        tabla.addCell(valor);
    }

    private static Font crearFuente(float tamano, int estilo) {
        try {
            BaseFont baseFont = BaseFont.createFont(
                    BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            return new Font(baseFont, tamano, estilo);
        } catch (DocumentException | IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static String moneda(BigDecimal importe) {
        return "$ " + formato(importe);
    }

    private static String formato(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("El rango de fechas para exportar es inválido");
        }
    }

    @FunctionalInterface
    private interface ContenidoPdf {
        void escribir(Document documento) throws DocumentException;
    }
}
