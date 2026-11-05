package com.cooperativa.cuidado.recibo.service;

import com.cooperativa.cuidado.recibo.ItemRetencionRecibo;
import com.cooperativa.cuidado.recibo.ReciboRetribucion;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class ReciboExportService {

    private static final String RUTA_PLANTILLA_XLSX = "plantillas/recibo_retribucion_plantilla.xlsx";
    private static final String RUTA_LOGO = "plantillas/logo_grupo_azul.png";

    private static final Color AZUL_SECCION = new Color(74, 134, 232);
    private static final Color AZUL_MARCA = new Color(60, 120, 216);
    private static final Color GRIS_SUBCABECERA = new Color(183, 183, 183);
    private static final Color GRIS_SUAVE = new Color(242, 244, 248);
    private static final Color GRIS_TEXTO_BANNER = new Color(102, 102, 102);
    private static final Color BORDE_OSCURO = new Color(45, 55, 72);

    private static final Font F_BANNER_SUB = fuente(10, Font.NORMAL, GRIS_TEXTO_BANNER);
    private static final Font F_BANNER_TIT = fuente(19, Font.BOLD, Color.BLACK);
    private static final Font F_BANNER_AZUL = fuente(19, Font.BOLD, AZUL_MARCA);
    private static final Font F_TITULO_DOC = fuente(10, Font.BOLD, Color.BLACK);
    private static final Font F_SECCION_BLANCA = fuente(8.5f, Font.BOLD, Color.WHITE);
    private static final Font F_CABECERA_TABLA = fuente(7.5f, Font.BOLD, Color.BLACK);
    private static final Font F_ETIQUETA = fuente(7.8f, Font.BOLD, Color.BLACK);
    private static final Font F_DATO = fuente(7.8f, Font.NORMAL, Color.BLACK);
    private static final Font F_DATO_GRANDE = fuente(9f, Font.BOLD, Color.BLACK);
    private static final Font F_NETO_BLANCO = fuente(10f, Font.BOLD, Color.WHITE);
    private static final Font F_FIRMA = fuente(7.5f, Font.BOLD, Color.BLACK);
    private static final Font F_ORIGINAL = fuente(7f, Font.NORMAL, GRIS_TEXTO_BANNER);

    private final ReciboRetribucionService reciboService;

    public ReciboExportService(ReciboRetribucionService reciboService) {
        this.reciboService = reciboService;
    }

    @Transactional(readOnly = true)
    public byte[] generarExcel(Long reciboId) {
        ReciboRetribucion recibo = reciboService.obtener(reciboId);
        return generarExcelDesdeEntidad(recibo);
    }

    public byte[] generarExcelDesdeEntidad(ReciboRetribucion recibo) {
        ClassPathResource plantilla = new ClassPathResource(RUTA_PLANTILLA_XLSX);
        try (InputStream in = plantilla.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(in);
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.getSheetAt(0);

            // Cabecera derecha: N° recibo y Fecha
            escribirTexto(sheet, 7, 9, recibo.getNumeroFormateado());
            escribirFecha(sheet, 8, 9, recibo.getFechaEmision());

            // Datos del socio
            escribirTexto(sheet, 14, 1, recibo.getNombreApellidoSocio());
            escribirTexto(sheet, 14, 8, recibo.getPeriodoTexto());
            escribirTexto(sheet, 15, 1, recibo.getCuilSocio());
            escribirFecha(sheet, 15, 8, recibo.getFechaPago());
            escribirTexto(sheet, 16, 1, recibo.getDomicilioSocio());
            escribirTexto(sheet, 16, 8, recibo.getCbuSocio());
            escribirTexto(sheet, 17, 1, recibo.getLocalidadSocio());
            escribirCpRico(workbook, sheet, 17, 4, recibo.getCpSocio());
            escribirTexto(sheet, 17, 8, recibo.getComprobanteTransferencia());

            // Cuidados domiciliarios
            escribirNumero(sheet, 22, 3, recibo.getHorasCuidados());
            escribirNumero(sheet, 22, 5, BigDecimal.valueOf(recibo.getUnidadesSanatorio()));
            escribirNumero(sheet, 22, 6, BigDecimal.valueOf(recibo.getUnidadesViaticos()));
            escribirNumero(sheet, 22, 8, recibo.getHorasCuidados());

            escribirNumero(sheet, 23, 3, recibo.getValorHoraCuidados());
            escribirNumero(sheet, 23, 5, recibo.getValorSanatorio());
            escribirNumero(sheet, 23, 6, recibo.getValorViatico());
            escribirTexto(sheet, 23, 8, "-");

            escribirNumero(sheet, 24, 3, recibo.getSubtotalHoras());
            escribirNumero(sheet, 24, 5, recibo.getSubtotalSanatorio());
            escribirNumero(sheet, 24, 6, recibo.getSubtotalViaticos());
            escribirNumero(sheet, 24, 8, recibo.getSubtotalCuidados());

            // Otros conceptos y Total de la retribución
            escribirTexto(sheet, 26, 0, recibo.getOtrosConcepto());
            escribirTexto(sheet, 26, 2, recibo.getOtrosDetalle());
            escribirNumero(sheet, 26, 8, recibo.getTotalOtrosConceptos());
            escribirNumero(sheet, 27, 9, recibo.getTotalRetribucion());

            // Datos de retiro de excedentes
            escribirTexto(sheet, 31, 0, recibo.getExcedenteConcepto());
            escribirTexto(sheet, 31, 2, recibo.getExcedenteDetalle());
            escribirNumero(sheet, 31, 5, recibo.getExcedenteUnidad());
            escribirNumero(sheet, 31, 6, recibo.getExcedenteValorUnidad());
            escribirNumero(sheet, 31, 8, recibo.getTotalExcedente());

            // Retenciones (0 = Monotributo, 1 = Préstamo, 2..N = Otras Retenciones)
            List<ItemRetencionRecibo> items = recibo.getItemsRetencion();
            ItemRetencionRecibo mono = items.size() > 0 ? items.get(0)
                    : new ItemRetencionRecibo("RETENCION MONOTRIBUTO", "Retencion Pago Monotributo",
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            ItemRetencionRecibo prestamo = items.size() > 1 ? items.get(1)
                    : new ItemRetencionRecibo("PRÉSTAMO COOPERATIVA", "Prestamo Personal Asociado",
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            List<ItemRetencionRecibo> otras = items.size() > 2
                    ? items.subList(2, items.size())
                    : List.of(new ItemRetencionRecibo("OTRAS RETENCIONES:",
                    "Retención por Incumplimiento Articulo 5 Reglamento Interno",
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

            escribirFilaRetencion(sheet, 35, mono);
            escribirFilaRetencion(sheet, 36, prestamo);

            int extra = Math.max(0, otras.size() - 1);
            if (extra > 0) {
                CellStyle[] estilosIntermedios = capturarEstilosFila(sheet.getRow(36), 12);
                CellStyle[] estilosFinales = capturarEstilosFila(sheet.getRow(37), 12);
                short alturaFila = sheet.getRow(37) != null ? sheet.getRow(37).getHeight() : (short) -1;

                sheet.shiftRows(38, sheet.getLastRowNum(), extra, true, false);
                aplicarEstilosFila(sheet, 37, estilosIntermedios, alturaFila);

                for (int k = 1; k <= extra; k++) {
                    int rIdx = 37 + k;
                    CellStyle[] estilos = (k == extra) ? estilosFinales : estilosIntermedios;
                    aplicarEstilosFila(sheet, rIdx, estilos, alturaFila);
                    sheet.addMergedRegion(new CellRangeAddress(rIdx, rIdx, 0, 1));
                    sheet.addMergedRegion(new CellRangeAddress(rIdx, rIdx, 2, 4));
                    sheet.addMergedRegion(new CellRangeAddress(rIdx, rIdx, 6, 7));
                    sheet.addMergedRegion(new CellRangeAddress(rIdx, rIdx, 8, 10));
                }
            }

            for (int k = 0; k < otras.size(); k++) {
                escribirFilaRetencion(sheet, 37 + k, otras.get(k));
            }

            escribirNumero(sheet, 38 + extra, 9, recibo.getTotalRetenciones());
            escribirNumero(sheet, 40 + extra, 8, recibo.getRetribucionNeta());

            workbook.setPrintArea(0, 0, 10, 0, 51 + extra);
            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el archivo Excel del recibo de retribución", ex);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generarPdf(Long reciboId) {
        ReciboRetribucion recibo = reciboService.obtener(reciboId);
        return generarPdfDesdeEntidad(recibo);
    }

    public byte[] generarPdfDesdeEntidad(ReciboRetribucion recibo) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 24, 24, 20, 20);
            PdfWriter.getInstance(doc, salida);
            doc.open();

            // 1. Encabezado institucional con logo GRUPO AZUL LTDA
            PdfPTable banner = new PdfPTable(new float[]{1.8f, 8.2f});
            banner.setWidthPercentage(100);

            PdfPCell celdaLogo = new PdfPCell();
            celdaLogo.setBorder(Rectangle.TOP | Rectangle.LEFT | Rectangle.BOTTOM);
            celdaLogo.setBorderColor(BORDE_OSCURO);
            celdaLogo.setBorderWidth(1f);
            celdaLogo.setPadding(6);
            celdaLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
            celdaLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Image logo = cargarLogo();
            if (logo != null) {
                logo.scaleToFit(58, 58);
                logo.setAlignment(Element.ALIGN_CENTER);
                celdaLogo.addElement(logo);
            }
            banner.addCell(celdaLogo);

            PdfPCell celdaMarca = new PdfPCell();
            celdaMarca.setBorder(Rectangle.TOP | Rectangle.RIGHT | Rectangle.BOTTOM);
            celdaMarca.setBorderColor(BORDE_OSCURO);
            celdaMarca.setBorderWidth(1f);
            celdaMarca.setPadding(8);
            celdaMarca.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Paragraph pSub = new Paragraph("CUIDADO INTEGRAL DE PERSONAS", F_BANNER_SUB);
            pSub.setAlignment(Element.ALIGN_CENTER);
            Paragraph pMarca = new Paragraph();
            pMarca.setAlignment(Element.ALIGN_CENTER);
            pMarca.add(new Chunk("GRUPO ", F_BANNER_TIT));
            pMarca.add(new Chunk("AZUL ", F_BANNER_AZUL));
            pMarca.add(new Chunk("LTDA", F_BANNER_TIT));
            celdaMarca.addElement(pSub);
            celdaMarca.addElement(pMarca);
            banner.addCell(celdaMarca);
            doc.add(banner);

            // 2. Título central del recibo
            PdfPTable tablaTitulo = new PdfPTable(1);
            tablaTitulo.setWidthPercentage(100);
            PdfPCell cTit = new PdfPCell(new Phrase("RECIBO DE RETRIBUCIÓN / ANTICIPO DE RETORNOS / RETENCIONES", F_TITULO_DOC));
            cTit.setHorizontalAlignment(Element.ALIGN_CENTER);
            cTit.setPadding(5);
            cTit.setBackgroundColor(GRIS_SUAVE);
            cTit.setBorderColor(BORDE_OSCURO);
            cTit.setBorderWidth(1f);
            tablaTitulo.addCell(cTit);
            doc.add(tablaTitulo);

            // 3. Datos de la Cooperativa + N° Recibo y Fecha
            PdfPTable coop = new PdfPTable(new float[]{6.4f, 2.2f, 1.4f});
            coop.setWidthPercentage(100);
            coop.setSpacingBefore(3);

            coop.addCell(celdaParEtiquetaValor("RAZÓN SOCIAL: ", "COOPERATIVA DE TRABAJO GRUPO AZUL LIMITADA.", Rectangle.TOP | Rectangle.LEFT));
            coop.addCell(celdaTexto("RECIBO DE RETRIBUCIÓN", F_ETIQUETA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            coop.addCell(celdaTexto(recibo.getNumeroFormateado(), F_DATO_GRANDE, Element.ALIGN_CENTER, Color.WHITE, Rectangle.BOX));

            coop.addCell(celdaParEtiquetaValor("CUIT: ", "30-71745352-9", Rectangle.LEFT));
            coop.addCell(celdaTexto("FECHA", F_ETIQUETA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            coop.addCell(celdaTexto(recibo.getFechaEmision().toString(), F_DATO, Element.ALIGN_CENTER, Color.WHITE, Rectangle.BOX));

            PdfPCell matCell = celdaParEtiquetaValor("MATRÍCULAS: ", "Nacional N° 60848 / Provincial N° 4124", Rectangle.LEFT | Rectangle.RIGHT);
            matCell.setColspan(3);
            coop.addCell(matCell);

            PdfPCell domCoopCell = celdaParEtiquetaValor("DOMICILIO: ",
                    "Amenabar 3427. CP 3000. Santa Fe Capital. Provincia de Santa Fé",
                    Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
            domCoopCell.setColspan(3);
            coop.addCell(domCoopCell);
            doc.add(coop);

            // 4. DATOS DEL SOCIO
            doc.add(barraSeccion("DATOS DEL SOCIO"));
            PdfPTable tSocio = new PdfPTable(new float[]{1.9f, 2.3f, 1.0f, 2.7f, 2.1f});
            tSocio.setWidthPercentage(100);

            tSocio.addCell(celdaTexto("NOMBRE Y APELLIDO:", F_ETIQUETA, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX));
            PdfPCell cNom = celdaTexto(recibo.getNombreApellidoSocio(), F_DATO, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX);
            cNom.setColspan(2);
            tSocio.addCell(cNom);
            tSocio.addCell(celdaTexto("PERIODO DE RECIBO DE RETRIBUCIÓN:", F_ETIQUETA, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX));
            tSocio.addCell(celdaTexto(recibo.getPeriodoTexto(), F_DATO, Element.ALIGN_CENTER, Color.WHITE, Rectangle.BOX));

            tSocio.addCell(celdaTexto("CUIL:", F_ETIQUETA, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX));
            PdfPCell cCuil = celdaTexto(recibo.getCuilSocio(), F_DATO, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX);
            cCuil.setColspan(2);
            tSocio.addCell(cCuil);
            tSocio.addCell(celdaTexto("FECHA DE PAGO:", F_ETIQUETA, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX));
            tSocio.addCell(celdaTexto(recibo.getFechaPago().toString(), F_DATO, Element.ALIGN_CENTER, Color.WHITE, Rectangle.BOX));

            tSocio.addCell(celdaTexto("DOMICILIO:", F_ETIQUETA, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX));
            PdfPCell cDom = celdaTexto(recibo.getDomicilioSocio(), F_DATO, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX);
            cDom.setColspan(2);
            tSocio.addCell(cDom);
            tSocio.addCell(celdaTexto("CBU / CVU:", F_ETIQUETA, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX));
            tSocio.addCell(celdaTexto(recibo.getCbuSocio(), F_DATO, Element.ALIGN_CENTER, Color.WHITE, Rectangle.BOX));

            tSocio.addCell(celdaTexto("LOCALIDAD:", F_ETIQUETA, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX));
            tSocio.addCell(celdaTexto(recibo.getLocalidadSocio(), F_DATO, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX));
            tSocio.addCell(celdaParEtiquetaValor("CP: ", recibo.getCpSocio(), Rectangle.BOX));
            tSocio.addCell(celdaTexto("COMPROBANTE TRANSFERENCIA N°:", F_ETIQUETA, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX));
            tSocio.addCell(celdaTexto(recibo.getComprobanteTransferencia(), F_DATO, Element.ALIGN_CENTER, Color.WHITE, Rectangle.BOX));
            doc.add(tSocio);

            // 5. DATOS DE LA RETRIBUCIÓN / ANTICIPO DE RETORNOS
            doc.add(barraSeccion("DATOS DE LA RETRIBUCIÓN / ANTICIPO DE RETORNOS"));
            PdfPTable tRetrib = new PdfPTable(new float[]{2.2f, 1.5f, 1.5f, 1.4f, 1.4f, 2.0f});
            tRetrib.setWidthPercentage(100);

            tRetrib.addCell(celdaTexto("CONCEPTOS", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            tRetrib.addCell(celdaTexto("DETALLE", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            tRetrib.addCell(celdaTexto("HORAS", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            tRetrib.addCell(celdaTexto("SANATORIO", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            tRetrib.addCell(celdaTexto("VIÁTICOS", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            tRetrib.addCell(celdaTexto("TOTAL DE HORAS y RETRIBUCION", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));

            PdfPCell cCuidados = celdaTexto("CUIDADOS DOMICILIARIOS", F_ETIQUETA, Element.ALIGN_CENTER, Color.WHITE, Rectangle.BOX);
            cCuidados.setRowspan(3);
            cCuidados.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tRetrib.addCell(cCuidados);

            tRetrib.addCell(celdaTexto("UNIDAD / HORAS", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUAVE, Rectangle.BOX));
            tRetrib.addCell(celdaTexto(fmtCantidad(recibo.getHorasCuidados()), F_DATO, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
            tRetrib.addCell(celdaTexto(String.valueOf(recibo.getUnidadesSanatorio()), F_DATO, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
            tRetrib.addCell(celdaTexto(String.valueOf(recibo.getUnidadesViaticos()), F_DATO, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
            tRetrib.addCell(celdaTexto(fmtCantidad(recibo.getHorasCuidados()), F_ETIQUETA, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));

            tRetrib.addCell(celdaTexto("VALOR", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUAVE, Rectangle.BOX));
            tRetrib.addCell(celdaTexto(fmtMoneda(recibo.getValorHoraCuidados()), F_DATO, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
            tRetrib.addCell(celdaTexto(fmtMoneda(recibo.getValorSanatorio()), F_DATO, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
            tRetrib.addCell(celdaTexto(fmtMoneda(recibo.getValorViatico()), F_DATO, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
            tRetrib.addCell(celdaTexto("-", F_DATO, Element.ALIGN_CENTER, Color.WHITE, Rectangle.BOX));

            tRetrib.addCell(celdaTexto("SUBTOTALES", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUAVE, Rectangle.BOX));
            tRetrib.addCell(celdaTexto(fmtMoneda(recibo.getSubtotalHoras()), F_ETIQUETA, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
            tRetrib.addCell(celdaTexto(fmtMoneda(recibo.getSubtotalSanatorio()), F_ETIQUETA, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
            tRetrib.addCell(celdaTexto(fmtMoneda(recibo.getSubtotalViaticos()), F_ETIQUETA, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
            tRetrib.addCell(celdaTexto(fmtMoneda(recibo.getSubtotalCuidados()), F_ETIQUETA, Element.ALIGN_RIGHT, GRIS_SUAVE, Rectangle.BOX));

            // Otros conceptos
            tRetrib.addCell(celdaTexto("OTROS CONCEPTOS", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            PdfPCell cDetOtrosHdr = celdaTexto("DETALLE", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX);
            cDetOtrosHdr.setColspan(4);
            tRetrib.addCell(cDetOtrosHdr);
            tRetrib.addCell(celdaTexto("TOTAL", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));

            tRetrib.addCell(celdaTexto(recibo.getOtrosConcepto(), F_DATO, Element.ALIGN_CENTER, Color.WHITE, Rectangle.BOX));
            PdfPCell cDetOtrosVal = celdaTexto(recibo.getOtrosDetalle(), F_DATO, Element.ALIGN_CENTER, Color.WHITE, Rectangle.BOX);
            cDetOtrosVal.setColspan(4);
            tRetrib.addCell(cDetOtrosVal);
            tRetrib.addCell(celdaTexto(fmtMoneda(recibo.getTotalOtrosConceptos()), F_DATO, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));

            PdfPCell cTotRetLbl = celdaTexto("TOTAL DE LA RETRIBUCIÓN", F_ETIQUETA, Element.ALIGN_RIGHT, GRIS_SUBCABECERA, Rectangle.BOX);
            cTotRetLbl.setColspan(5);
            tRetrib.addCell(cTotRetLbl);
            tRetrib.addCell(celdaTexto(fmtMoneda(recibo.getTotalRetribucion()), F_DATO_GRANDE, Element.ALIGN_RIGHT, GRIS_SUBCABECERA, Rectangle.BOX));
            doc.add(tRetrib);

            // 6. DATOS DE RETIRO DE EXCEDENTES
            doc.add(barraSeccion("DATOS DE RETIRO DE EXCEDENTES"));
            PdfPTable tExc = new PdfPTable(new float[]{2.4f, 3.6f, 1.0f, 1.4f, 1.6f});
            tExc.setWidthPercentage(100);
            tExc.addCell(celdaTexto("CONCEPTOS", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            tExc.addCell(celdaTexto("DETALLE", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            tExc.addCell(celdaTexto("UNIDAD", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            tExc.addCell(celdaTexto("VALOR UNIDAD", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            tExc.addCell(celdaTexto("TOTAL EXCEDENTE", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));

            tExc.addCell(celdaTexto(recibo.getExcedenteConcepto(), F_DATO, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX));
            tExc.addCell(celdaTexto(recibo.getExcedenteDetalle(), F_DATO, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX));
            tExc.addCell(celdaTexto(fmtCantidad(recibo.getExcedenteUnidad()), F_DATO, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
            tExc.addCell(celdaTexto(fmtMoneda(recibo.getExcedenteValorUnidad()), F_DATO, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
            tExc.addCell(celdaTexto(fmtMoneda(recibo.getTotalExcedente()), F_ETIQUETA, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
            doc.add(tExc);

            // 7. RETENCIONES
            doc.add(barraSeccion("RETENCIONES"));
            PdfPTable tRet = new PdfPTable(new float[]{2.4f, 3.6f, 1.0f, 1.4f, 1.6f});
            tRet.setWidthPercentage(100);
            tRet.addCell(celdaTexto("CONCEPTOS", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            tRet.addCell(celdaTexto("DETALLE", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            tRet.addCell(celdaTexto("UNIDAD", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            tRet.addCell(celdaTexto("VALOR UNIDAD", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));
            tRet.addCell(celdaTexto("TOTAL RETENCIONES", F_CABECERA_TABLA, Element.ALIGN_CENTER, GRIS_SUBCABECERA, Rectangle.BOX));

            for (ItemRetencionRecibo item : recibo.getItemsRetencion()) {
                tRet.addCell(celdaTexto(item.getConcepto(), F_ETIQUETA, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX));
                tRet.addCell(celdaTexto(item.getDetalle(), F_DATO, Element.ALIGN_LEFT, Color.WHITE, Rectangle.BOX));
                tRet.addCell(celdaTexto(fmtCantidad(item.getUnidad()), F_DATO, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
                tRet.addCell(celdaTexto(fmtMoneda(item.getValorUnidad()), F_DATO, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
                tRet.addCell(celdaTexto(fmtMoneda(item.getTotal()), F_ETIQUETA, Element.ALIGN_RIGHT, Color.WHITE, Rectangle.BOX));
            }

            PdfPCell cTotRetencionesLbl = celdaTexto("TOTAL DE RETENCIONES", F_ETIQUETA, Element.ALIGN_RIGHT, GRIS_SUBCABECERA, Rectangle.BOX);
            cTotRetencionesLbl.setColspan(4);
            tRet.addCell(cTotRetencionesLbl);
            tRet.addCell(celdaTexto(fmtMoneda(recibo.getTotalRetenciones()), F_DATO_GRANDE, Element.ALIGN_RIGHT, GRIS_SUBCABECERA, Rectangle.BOX));
            doc.add(tRet);

            // 8. RETRIBUCIÓN NETA
            PdfPTable tNeto = new PdfPTable(new float[]{8.4f, 1.6f});
            tNeto.setWidthPercentage(100);
            tNeto.setSpacingBefore(4);
            PdfPCell cNetoLbl = celdaTexto("RETRIBUCIÓN NETA", F_NETO_BLANCO, Element.ALIGN_LEFT, AZUL_SECCION, Rectangle.BOX);
            cNetoLbl.setPadding(6);
            PdfPCell cNetoVal = celdaTexto(fmtMoneda(recibo.getRetribucionNeta()), F_NETO_BLANCO, Element.ALIGN_RIGHT, AZUL_SECCION, Rectangle.BOX);
            cNetoVal.setPadding(6);
            tNeto.addCell(cNetoLbl);
            tNeto.addCell(cNetoVal);
            doc.add(tNeto);

            // 9. Firmas y línea ORIGINAL
            PdfPTable tFirmas = new PdfPTable(new float[]{5f, 5f});
            tFirmas.setWidthPercentage(100);
            tFirmas.setSpacingBefore(22);

            PdfPCell fTesorero = new PdfPCell();
            fTesorero.setBorder(Rectangle.BOX);
            fTesorero.setBorderColor(BORDE_OSCURO);
            fTesorero.setMinimumHeight(46);
            fTesorero.setVerticalAlignment(Element.ALIGN_BOTTOM);
            fTesorero.setHorizontalAlignment(Element.ALIGN_CENTER);
            fTesorero.setPadding(5);
            Paragraph pTes = new Paragraph("_______________________________\nFIRMA TESORERO", F_FIRMA);
            pTes.setAlignment(Element.ALIGN_CENTER);
            fTesorero.addElement(pTes);

            PdfPCell fAsociado = new PdfPCell();
            fAsociado.setBorder(Rectangle.BOX);
            fAsociado.setBorderColor(BORDE_OSCURO);
            fAsociado.setMinimumHeight(46);
            fAsociado.setVerticalAlignment(Element.ALIGN_BOTTOM);
            fAsociado.setHorizontalAlignment(Element.ALIGN_CENTER);
            fAsociado.setPadding(5);
            Paragraph pAso = new Paragraph("_______________________________\nFIRMA ASOCIADO", F_FIRMA);
            pAso.setAlignment(Element.ALIGN_CENTER);
            fAsociado.addElement(pAso);

            tFirmas.addCell(fTesorero);
            tFirmas.addCell(fAsociado);
            doc.add(tFirmas);

            Paragraph pOrig = new Paragraph(
                    "ORIGINAL ------------------------------------------------------------------------------------------------------------------------------------------",
                    F_ORIGINAL);
            pOrig.setSpacingBefore(6);
            doc.add(pOrig);

            doc.close();
            return salida.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("No se pudo generar el documento PDF del recibo de retribución", ex);
        }
    }

    private static void escribirFilaRetencion(Sheet sheet, int rowIdx, ItemRetencionRecibo item) {
        escribirTexto(sheet, rowIdx, 0, item.getConcepto());
        escribirTexto(sheet, rowIdx, 2, item.getDetalle());
        escribirNumero(sheet, rowIdx, 5, item.getUnidad());
        escribirNumero(sheet, rowIdx, 6, item.getValorUnidad());
        escribirNumero(sheet, rowIdx, 8, item.getTotal());
    }

    private static Cell obtenerOCrearCelda(Sheet sheet, int rowIdx, int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }
        if (cell.getCellType() == CellType.FORMULA) {
            cell.removeFormula();
        }
        return cell;
    }

    private static void escribirTexto(Sheet sheet, int rowIdx, int colIdx, String valor) {
        Cell cell = obtenerOCrearCelda(sheet, rowIdx, colIdx);
        cell.setCellValue(valor != null ? valor : "-");
    }

    private static void escribirFecha(Sheet sheet, int rowIdx, int colIdx, LocalDate fecha) {
        Cell cell = obtenerOCrearCelda(sheet, rowIdx, colIdx);
        if (fecha != null) {
            cell.setCellValue(fecha);
        } else {
            cell.setCellValue("-");
        }
    }

    private static void escribirNumero(Sheet sheet, int rowIdx, int colIdx, BigDecimal valor) {
        Cell cell = obtenerOCrearCelda(sheet, rowIdx, colIdx);
        double num = valor != null ? valor.setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0d;
        cell.setCellValue(num);
    }

    private static void escribirCpRico(XSSFWorkbook workbook, Sheet sheet, int rowIdx, int colIdx, String cp) {
        Cell cell = obtenerOCrearCelda(sheet, rowIdx, colIdx);
        String prefijo = "CP: ";
        String valorCp = cp != null && !cp.isBlank() ? cp.trim() : "-";
        XSSFRichTextString rich = new XSSFRichTextString(prefijo + valorCp);

        XSSFFont boldFont = workbook.createFont();
        boldFont.setFontName("Nunito");
        boldFont.setFontHeightInPoints((short) 10);
        boldFont.setBold(true);

        XSSFFont normalFont = workbook.createFont();
        normalFont.setFontName("Nunito");
        normalFont.setFontHeightInPoints((short) 10);
        normalFont.setBold(false);

        rich.applyFont(0, prefijo.length(), boldFont);
        rich.applyFont(prefijo.length(), prefijo.length() + valorCp.length(), normalFont);
        cell.setCellValue(rich);
    }

    private static CellStyle[] capturarEstilosFila(Row row, int columnas) {
        CellStyle[] estilos = new CellStyle[columnas];
        if (row == null) {
            return estilos;
        }
        for (int c = 0; c < columnas; c++) {
            Cell cell = row.getCell(c);
            if (cell != null) {
                estilos[c] = cell.getCellStyle();
            }
        }
        return estilos;
    }

    private static void aplicarEstilosFila(Sheet sheet, int rowIdx, CellStyle[] estilos, short altura) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        if (altura > 0) {
            row.setHeight(altura);
        }
        for (int c = 0; c < estilos.length; c++) {
            Cell cell = row.getCell(c);
            if (cell == null) {
                cell = row.createCell(c);
            }
            if (estilos[c] != null) {
                cell.setCellStyle(estilos[c]);
            }
        }
    }

    private static Image cargarLogo() {
        try (InputStream is = new ClassPathResource(RUTA_LOGO).getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return Image.getInstance(bytes);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static PdfPTable barraSeccion(String titulo) {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(3);
        PdfPCell c = new PdfPCell(new Phrase(titulo, F_SECCION_BLANCA));
        c.setBackgroundColor(AZUL_SECCION);
        c.setBorderColor(BORDE_OSCURO);
        c.setBorderWidth(0.8f);
        c.setPadding(3.5f);
        tabla.addCell(c);
        return tabla;
    }

    private static PdfPCell celdaTexto(String texto, Font font, int align, Color bg, int border) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "-", font));
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(bg);
        c.setBorder(border);
        c.setBorderColor(BORDE_OSCURO);
        c.setBorderWidth(0.6f);
        c.setPadding(3.2f);
        return c;
    }

    private static PdfPCell celdaParEtiquetaValor(String etiqueta, String valor, int border) {
        Phrase p = new Phrase();
        p.add(new Chunk(etiqueta, F_ETIQUETA));
        p.add(new Chunk(valor != null ? valor : "-", F_DATO));
        PdfPCell c = new PdfPCell(p);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorder(border);
        c.setBorderColor(BORDE_OSCURO);
        c.setBorderWidth(0.6f);
        c.setPadding(3.2f);
        return c;
    }

    private static String fmtMoneda(BigDecimal valor) {
        BigDecimal v = valor != null ? valor.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.forLanguageTag("es-AR"));
        sym.setGroupingSeparator('.');
        sym.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0.00", sym);
        return "$ " + df.format(v);
    }

    private static String fmtCantidad(BigDecimal valor) {
        if (valor == null) {
            return "0";
        }
        BigDecimal limpio = valor.stripTrailingZeros();
        if (limpio.scale() <= 0) {
            return limpio.toPlainString();
        }
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.forLanguageTag("es-AR"));
        sym.setGroupingSeparator('.');
        sym.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.##", sym).format(valor);
    }

    private static Font fuente(float size, int style, Color color) {
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            return new Font(bf, size, style, color);
        } catch (DocumentException | IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}

