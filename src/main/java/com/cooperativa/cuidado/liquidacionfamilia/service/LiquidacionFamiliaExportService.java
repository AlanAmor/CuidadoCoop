package com.cooperativa.cuidado.liquidacionfamilia.service;

import com.cooperativa.cuidado.liquidacionfamilia.LineaCuidadorFamilia;
import com.cooperativa.cuidado.liquidacionfamilia.LiquidacionFamilia;
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
import org.apache.poi.xssf.usermodel.XSSFDrawing;
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
import java.util.List;
import java.util.Locale;

@Service
public class LiquidacionFamiliaExportService {

    private static final String RUTA_PLANTILLA_XLSX = "plantillas/liquidacion_familia_plantilla.xlsx";
    private static final String RUTA_LOGO_GRUPO_AZUL = "plantillas/logo_grupo_azul.png";
    private static final String RUTA_LOGO_INAES = "plantillas/logo_inaes.png";
    private static final String RUTA_LOGO_SANTAFE = "plantillas/logo_santafe.png";

    private static final Color PETROLEO_CABECERA = new Color(0, 87, 108);
    private static final Color PETROLEO_SUBTITULO = new Color(0, 130, 164);
    private static final Color AZUL_MARCA = new Color(60, 120, 216);
    private static final Color CELESTE_CEBRA = new Color(219, 232, 244);
    private static final Color GRIS_FILA_IMPAR = new Color(243, 243, 243);
    private static final Color GRIS_RESUMEN_1 = new Color(239, 239, 239);
    private static final Color GRIS_RESUMEN_2 = new Color(217, 217, 217);
    private static final Color GRIS_RESUMEN_3 = new Color(204, 204, 204);
    private static final Color TEXTO_GRIS = new Color(66, 66, 66);
    private static final Color TEXTO_BANNER = new Color(102, 102, 102);
    private static final Color BORDE_SUAVE = new Color(210, 218, 226);

    private static final Font F_BANNER_SUB = fuente(9.5f, Font.NORMAL, TEXTO_BANNER);
    private static final Font F_BANNER_TIT = fuente(18f, Font.BOLD, Color.BLACK);
    private static final Font F_BANNER_AZUL = fuente(18f, Font.BOLD, AZUL_MARCA);
    private static final Font F_BANNER_MAT = fuente(7.5f, Font.NORMAL, TEXTO_GRIS);
    private static final Font F_UCD = fuente(9.5f, Font.BOLD, PETROLEO_CABECERA);
    private static final Font F_PACIENTE = fuente(9f, Font.NORMAL, TEXTO_GRIS);
    private static final Font F_PACIENTE_BOLD = fuente(9.5f, Font.BOLD, TEXTO_GRIS);
    private static final Font F_PERIODO = fuente(8.8f, Font.BOLD, TEXTO_GRIS);
    private static final Font F_TABLA_HDR = fuente(8.5f, Font.BOLD, Color.WHITE);
    private static final Font F_CELDA = fuente(8.8f, Font.NORMAL, Color.BLACK);
    private static final Font F_RESUMEN_LBL = fuente(8.5f, Font.BOLD, Color.BLACK);
    private static final Font F_RESUMEN_VAL = fuente(9f, Font.NORMAL, Color.BLACK);
    private static final Font F_RESUMEN_TOTAL = fuente(9.5f, Font.BOLD, Color.BLACK);
    private static final Font F_SECCION_TEAL = fuente(9f, Font.BOLD, PETROLEO_SUBTITULO);
    private static final Font F_DETALLE = fuente(8.5f, Font.NORMAL, Color.BLACK);
    private static final Font F_GRACIAS = fuente(9.5f, Font.BOLD, PETROLEO_CABECERA);
    private static final Font F_PIE_COOP = fuente(8.5f, Font.NORMAL, TEXTO_GRIS);

    private final LiquidacionFamiliaService liquidacionService;

    public LiquidacionFamiliaExportService(LiquidacionFamiliaService liquidacionService) {
        this.liquidacionService = liquidacionService;
    }

    @Transactional(readOnly = true)
    public byte[] generarExcel(Long id) {
        return generarExcelDesdeEntidad(liquidacionService.obtener(id));
    }

    public byte[] generarExcelDesdeEntidad(LiquidacionFamilia liq) {
        ClassPathResource plantilla = new ClassPathResource(RUTA_PLANTILLA_XLSX);
        try (InputStream in = plantilla.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(in);
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            while (workbook.getNumberOfSheets() > 1) {
                workbook.removeSheetAt(1);
            }

            Sheet sheet = workbook.getSheetAt(0);

            // Datos del paciente y período (Filas 3..8 -> índices 2..7)
            escribirTexto(sheet, 2, 1, "UNIDAD DE CUIDADO DOMICILIARIO");
            escribirTexto(sheet, 3, 1, liq.getNombrePaciente());
            String dniTexto = liq.getDniPaciente() != null && !liq.getDniPaciente().isBlank()
                    ? "DNI " + liq.getDniPaciente().trim()
                    : "DNI ";
            escribirTexto(sheet, 4, 1, dniTexto);
            escribirTexto(sheet, 5, 1, liq.getDireccion());
            escribirTexto(sheet, 6, 1, liq.getLineaUbicacion());
            escribirTexto(sheet, 7, 1, "Contacto: " + (liq.getTelefonoContacto() != null && !liq.getTelefonoContacto().isBlank()
                    ? liq.getTelefonoContacto().trim() : "-"));
            escribirTexto(sheet, 7, 3, " " + liq.getPeriodoEtiqueta());

            // Tabla de cuidadores (la plantilla trae 4 filas: índices 10, 11, 12, 13)
            List<LineaCuidadorFamilia> lineas = liq.getLineas().isEmpty()
                    ? List.of(new LineaCuidadorFamilia("-", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                    : liq.getLineas();

            int n = lineas.size();
            int delta = n - 4;
            CellStyle[] estilosImpar = capturarEstilosFila(sheet.getRow(10), 6);
            CellStyle[] estilosPar = capturarEstilosFila(sheet.getRow(11), 6);
            short alturaFila = sheet.getRow(10) != null ? sheet.getRow(10).getHeight() : (short) -1;

            if (delta > 0) {
                sheet.shiftRows(14, sheet.getLastRowNum(), delta, true, false);
            } else if (delta < 0) {
                for (int r = 10 + n; r <= 13; r++) {
                    Row obsoleta = sheet.getRow(r);
                    if (obsoleta != null) {
                        sheet.removeRow(obsoleta);
                    }
                }
                int ultimo = sheet.getLastRowNum();
                sheet.shiftRows(14, ultimo, delta, true, false);
            }

            if (delta != 0 && sheet.getDrawingPatriarch() instanceof XSSFDrawing drawing) {
                drawing.getCTDrawing().getOneCellAnchorList().forEach(anchor -> {
                    if (anchor.getFrom().getRow() >= 14) {
                        anchor.getFrom().setRow(anchor.getFrom().getRow() + delta);
                    }
                });
            }

            for (int i = 0; i < n; i++) {
                int rIdx = 10 + i;
                aplicarEstilosFila(sheet, rIdx, (i % 2 == 0) ? estilosImpar : estilosPar, alturaFila);
                LineaCuidadorFamilia l = lineas.get(i);
                escribirTexto(sheet, rIdx, 1, l.getNombreCuidador());
                escribirNumero(sheet, rIdx, 2, l.getHoras());
                escribirNumero(sheet, rIdx, 3, l.getValorHoraCliente());
                escribirNumero(sheet, rIdx, 4, l.getTotalLinea());
            }

            int base = 14 + delta;
            escribirNumero(sheet, base, 4, liq.getSubtotal());
            escribirNumero(sheet, base + 1, 4, liq.getImporteIva());
            escribirNumero(sheet, base + 2, 4, liq.getImporteImpuestos());
            escribirNumero(sheet, base + 3, 4, liq.getTotalMensual());
            escribirNumero(sheet, base + 4, 4, liq.getSaldoATransferir());

            escribirTexto(sheet, base + 7, 1, "Variacion de IPC Salud: " + fmtPorcentaje(liq.getVariacionIpcSalud()) + "%");
            escribirTexto(sheet, base + 8, 1, "Transporte Publico: " + fmtPorcentaje(liq.getTransportePublico()) + "%");
            escribirTexto(sheet, base + 9, 1, "Aumento por Paritaria: " + fmtPorcentaje(liq.getAumentoParitaria()) + "%");

            workbook.setPrintArea(0, 0, 5, 0, Math.max(41 + delta, base + 26));
            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el archivo Excel de Liquidación Familia", ex);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generarPdf(Long id) {
        return generarPdfDesdeEntidad(liquidacionService.obtener(id));
    }

    public byte[] generarPdfDesdeEntidad(LiquidacionFamilia liq) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 34, 34, 22, 20);
            PdfWriter.getInstance(doc, salida);
            doc.open();

            // 1. Encabezado institucional Grupo Azul Ltda
            PdfPTable banner = new PdfPTable(new float[]{1.8f, 8.2f});
            banner.setWidthPercentage(88);
            banner.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell cLogo = new PdfPCell();
            cLogo.setBorder(Rectangle.NO_BORDER);
            cLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
            Image logo = cargarImagen(RUTA_LOGO_GRUPO_AZUL);
            if (logo != null) {
                logo.scaleToFit(54, 54);
                logo.setAlignment(Element.ALIGN_CENTER);
                cLogo.addElement(logo);
            }
            banner.addCell(cLogo);

            PdfPCell cMarca = new PdfPCell();
            cMarca.setBorder(Rectangle.NO_BORDER);
            cMarca.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Paragraph pSub = new Paragraph("CUIDADO INTEGRAL DE PERSONAS", F_BANNER_SUB);
            pSub.setAlignment(Element.ALIGN_CENTER);
            Paragraph pTit = new Paragraph();
            pTit.setAlignment(Element.ALIGN_CENTER);
            pTit.add(new Chunk("GRUPO ", F_BANNER_TIT));
            pTit.add(new Chunk("AZUL ", F_BANNER_AZUL));
            pTit.add(new Chunk("LTDA", F_BANNER_TIT));
            Paragraph pMat = new Paragraph("Mat. Nacional N° 60848\nMat. Provincial N° 4124", F_BANNER_MAT);
            pMat.setAlignment(Element.ALIGN_CENTER);
            cMarca.addElement(pSub);
            cMarca.addElement(pTit);
            cMarca.addElement(pMat);
            banner.addCell(cMarca);
            doc.add(banner);

            // 2. Bloque de datos del paciente y período
            PdfPTable tPaciente = new PdfPTable(new float[]{6.2f, 3.8f});
            tPaciente.setWidthPercentage(88);
            tPaciente.setHorizontalAlignment(Element.ALIGN_CENTER);
            tPaciente.setSpacingBefore(8);

            PdfPCell cUcd = celdaSinBorde("UNIDAD DE CUIDADO DOMICILIARIO", F_UCD, Element.ALIGN_LEFT);
            cUcd.setColspan(2);
            cUcd.setPaddingBottom(3);
            tPaciente.addCell(cUcd);

            PdfPCell cNom = celdaSinBorde(liq.getNombrePaciente(), F_PACIENTE_BOLD, Element.ALIGN_LEFT);
            cNom.setColspan(2);
            tPaciente.addCell(cNom);

            String dniTxt = liq.getDniPaciente() != null && !liq.getDniPaciente().isBlank()
                    ? "DNI " + liq.getDniPaciente().trim()
                    : "DNI ";
            PdfPCell cDni = celdaSinBorde(dniTxt, F_PACIENTE, Element.ALIGN_LEFT);
            cDni.setColspan(2);
            tPaciente.addCell(cDni);

            PdfPCell cDir = celdaSinBorde(liq.getDireccion(), F_PACIENTE, Element.ALIGN_LEFT);
            cDir.setColspan(2);
            tPaciente.addCell(cDir);

            PdfPCell cUbi = celdaSinBorde(liq.getLineaUbicacion(), F_PACIENTE, Element.ALIGN_LEFT);
            cUbi.setColspan(2);
            tPaciente.addCell(cUbi);

            String contactoTxt = "Contacto: " + (liq.getTelefonoContacto() != null && !liq.getTelefonoContacto().isBlank()
                    ? liq.getTelefonoContacto().trim() : "-");
            tPaciente.addCell(celdaSinBorde(contactoTxt, F_PACIENTE, Element.ALIGN_LEFT));
            tPaciente.addCell(celdaSinBorde(liq.getPeriodoEtiqueta(), F_PERIODO, Element.ALIGN_RIGHT));
            doc.add(tPaciente);

            // 3. Tabla de Cuidadores y Totales
            PdfPTable tabla = new PdfPTable(new float[]{3.8f, 1.7f, 2.0f, 2.5f});
            tabla.setWidthPercentage(88);
            tabla.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.setSpacingBefore(8);

            tabla.addCell(celdaTabla("CUIDADOR / A", F_TABLA_HDR, Element.ALIGN_CENTER, PETROLEO_CABECERA, 5f));
            tabla.addCell(celdaTabla("HORAS", F_TABLA_HDR, Element.ALIGN_CENTER, PETROLEO_CABECERA, 5f));
            tabla.addCell(celdaTabla("VALOR HORA", F_TABLA_HDR, Element.ALIGN_CENTER, PETROLEO_CABECERA, 5f));
            tabla.addCell(celdaTabla("TOTAL DE LA LINEA", F_TABLA_HDR, Element.ALIGN_CENTER, PETROLEO_CABECERA, 5f));

            List<LineaCuidadorFamilia> lineas = liq.getLineas();
            for (int i = 0; i < lineas.size(); i++) {
                LineaCuidadorFamilia l = lineas.get(i);
                Color fondo = (i % 2 == 0) ? GRIS_FILA_IMPAR : CELESTE_CEBRA;
                tabla.addCell(celdaTabla(l.getNombreCuidador(), F_CELDA, Element.ALIGN_CENTER, fondo, 4.2f));
                tabla.addCell(celdaTabla(fmtCantidad(l.getHoras()), F_CELDA, Element.ALIGN_CENTER, fondo, 4.2f));
                tabla.addCell(celdaTabla(fmtMoneda(l.getValorHoraCliente()), F_CELDA, Element.ALIGN_CENTER, fondo, 4.2f));
                tabla.addCell(celdaTabla(fmtMoneda(l.getTotalLinea()), F_CELDA, Element.ALIGN_CENTER, fondo, 4.2f));
            }

            agregarFilaResumen(tabla, "SUBTOTAL", fmtMoneda(liq.getSubtotal()), GRIS_RESUMEN_1, false);
            agregarFilaResumen(tabla, "IVA ", fmtMoneda(liq.getImporteIva()), GRIS_RESUMEN_1, false);
            agregarFilaResumen(tabla, "Impuestos", fmtMoneda(liq.getImporteImpuestos()), GRIS_RESUMEN_2, false);
            agregarFilaResumen(tabla, "TOTAL MENSUAL del SERVICIO ", fmtMoneda(liq.getTotalMensual()), GRIS_RESUMEN_3, false);
            agregarFilaResumen(tabla, "SALDO A TRANSFERIR", fmtMoneda(liq.getSaldoATransferir()), GRIS_RESUMEN_3, true);
            doc.add(tabla);

            // 4. Criterios de Valores de Aumento del Servicio + Datos Bancarios
            PdfPTable tInfo = new PdfPTable(1);
            tInfo.setWidthPercentage(88);
            tInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
            tInfo.setSpacingBefore(10);

            tInfo.addCell(celdaSinBorde("Criterios de Valores de Aumento del Servicio:", F_SECCION_TEAL, Element.ALIGN_LEFT));
            tInfo.addCell(celdaSinBorde("Variacion de IPC Salud: " + fmtPorcentaje(liq.getVariacionIpcSalud()) + "%", F_DETALLE, Element.ALIGN_LEFT));
            tInfo.addCell(celdaSinBorde("Transporte Publico: " + fmtPorcentaje(liq.getTransportePublico()) + "%", F_DETALLE, Element.ALIGN_LEFT));
            tInfo.addCell(celdaSinBorde("Aumento por Paritaria: " + fmtPorcentaje(liq.getAumentoParitaria()) + "%", F_DETALLE, Element.ALIGN_LEFT));

            PdfPCell esp = celdaSinBorde(" ", F_DETALLE, Element.ALIGN_LEFT);
            esp.setFixedHeight(6f);
            tInfo.addCell(esp);

            tInfo.addCell(celdaSinBorde("DATOS PARA TRANSFERENCIA BANCARIA", F_SECCION_TEAL, Element.ALIGN_LEFT));
            tInfo.addCell(celdaSinBorde("Banco: BICA ", F_DETALLE, Element.ALIGN_LEFT));
            tInfo.addCell(celdaSinBorde("Titular: COOPERATIVA DE TRABAJO GRUPO AZUL LIMITADA", F_DETALLE, Element.ALIGN_LEFT));
            tInfo.addCell(celdaSinBorde("CBU: 4260010100200097013013", F_DETALLE, Element.ALIGN_LEFT));
            tInfo.addCell(celdaSinBorde("Cuit: 30-71745352-9", F_DETALLE, Element.ALIGN_LEFT));
            doc.add(tInfo);

            // 5. Pie institucional y logos INAES + Provincia de Santa Fe
            PdfPTable tPie = new PdfPTable(1);
            tPie.setWidthPercentage(88);
            tPie.setHorizontalAlignment(Element.ALIGN_CENTER);
            tPie.setSpacingBefore(10);

            tPie.addCell(celdaSinBorde("GRACIAS POR SU CONFIANZA!", F_GRACIAS, Element.ALIGN_CENTER));
            tPie.addCell(celdaSinBorde("Cooperativa de Trabajo Grupo Azul Limitada.", F_GRACIAS, Element.ALIGN_CENTER));
            tPie.addCell(celdaSinBorde("Amenabar 3427. Santa Fé, Santa Fé, CP 3000,", F_PIE_COOP, Element.ALIGN_CENTER));
            tPie.addCell(celdaSinBorde("Teléfono 3425237007", F_PIE_COOP, Element.ALIGN_CENTER));
            tPie.addCell(celdaSinBorde("grupoazul.ucd@gmail.com", F_PIE_COOP, Element.ALIGN_CENTER));

            Image logoInaes = cargarImagen(RUTA_LOGO_INAES);
            if (logoInaes != null) {
                logoInaes.scaleToFit(120, 42);
                logoInaes.setAlignment(Element.ALIGN_CENTER);
                PdfPCell cInaes = new PdfPCell();
                cInaes.setBorder(Rectangle.NO_BORDER);
                cInaes.setPaddingTop(6f);
                cInaes.setHorizontalAlignment(Element.ALIGN_CENTER);
                cInaes.addElement(logoInaes);
                tPie.addCell(cInaes);
            }

            Image logoSf = cargarImagen(RUTA_LOGO_SANTAFE);
            if (logoSf != null) {
                logoSf.scaleToFit(230, 36);
                logoSf.setAlignment(Element.ALIGN_CENTER);
                PdfPCell cSf = new PdfPCell();
                cSf.setBorder(Rectangle.NO_BORDER);
                cSf.setPaddingTop(4f);
                cSf.setHorizontalAlignment(Element.ALIGN_CENTER);
                cSf.addElement(logoSf);
                tPie.addCell(cSf);
            }
            doc.add(tPie);

            doc.close();
            return salida.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("No se pudo generar el PDF de Liquidación Familia", ex);
        }
    }

    private static void agregarFilaResumen(PdfPTable tabla, String etiqueta, String valor, Color fondo, boolean resaltarValor) {
        PdfPCell cLbl = new PdfPCell(new Phrase(etiqueta, F_RESUMEN_LBL));
        cLbl.setColspan(3);
        cLbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cLbl.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cLbl.setBackgroundColor(fondo);
        cLbl.setBorderColor(BORDE_SUAVE);
        cLbl.setBorderWidth(0.5f);
        cLbl.setPadding(4.2f);
        tabla.addCell(cLbl);

        PdfPCell cVal = new PdfPCell(new Phrase(valor, resaltarValor ? F_RESUMEN_TOTAL : F_RESUMEN_VAL));
        cVal.setHorizontalAlignment(Element.ALIGN_CENTER);
        cVal.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cVal.setBackgroundColor(fondo);
        cVal.setBorderColor(BORDE_SUAVE);
        cVal.setBorderWidth(0.5f);
        cVal.setPadding(4.2f);
        tabla.addCell(cVal);
    }

    private static PdfPCell celdaTabla(String texto, Font font, int align, Color fondo, float padding) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "", font));
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(fondo);
        c.setBorderColor(BORDE_SUAVE);
        c.setBorderWidth(0.5f);
        c.setPadding(padding);
        return c;
    }

    private static PdfPCell celdaSinBorde(String texto, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "", font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setPadding(1.6f);
        return c;
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
        cell.setCellValue(valor != null ? valor : "");
    }

    private static void escribirNumero(Sheet sheet, int rowIdx, int colIdx, BigDecimal valor) {
        Cell cell = obtenerOCrearCelda(sheet, rowIdx, colIdx);
        double num = valor != null ? valor.setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0d;
        cell.setCellValue(num);
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

    private static Image cargarImagen(String ruta) {
        try (InputStream is = new ClassPathResource(ruta).getInputStream()) {
            return Image.getInstance(is.readAllBytes());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String fmtMoneda(BigDecimal valor) {
        BigDecimal v = valor != null ? valor.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.forLanguageTag("es-AR"));
        sym.setGroupingSeparator('.');
        sym.setDecimalSeparator(',');
        return "$ " + new DecimalFormat("#,##0.00", sym).format(v);
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

    private static String fmtPorcentaje(BigDecimal valor) {
        if (valor == null) {
            return "0";
        }
        BigDecimal limpio = valor.stripTrailingZeros();
        if (limpio.scale() <= 0) {
            return limpio.toPlainString();
        }
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.forLanguageTag("es-AR"));
        sym.setDecimalSeparator(',');
        return new DecimalFormat("0.##", sym).format(valor);
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

