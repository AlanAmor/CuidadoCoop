package com.cooperativa.cuidado.facturacion.service;

import com.cooperativa.cuidado.facturacion.Factura;
import com.cooperativa.cuidado.facturacion.LineaFactura;
import com.cooperativa.cuidado.facturacion.TipoReceptor;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class FacturacionExportService {

    private final ArcaFacturacionService arcaService;

    private static final Color AZUL_ARCA = new Color(0, 87, 108);
    private static final Color GRIS_FONDO = new Color(245, 247, 250);
    private static final Color BORDE_SUAVE = new Color(200, 210, 220);
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public FacturacionExportService(ArcaFacturacionService arcaService) {
        this.arcaService = arcaService;
    }

    public byte[] generarPdf(Factura factura) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 30, 30, 25, 25);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Tipografías
            Font fTitDoc = new Font(Font.HELVETICA, 16, Font.BOLD, AZUL_ARCA);
            Font fLetra = new Font(Font.HELVETICA, 24, Font.BOLD, Color.BLACK);
            Font fCodLetra = new Font(Font.HELVETICA, 8, Font.BOLD, Color.DARK_GRAY);
            Font fNegrita = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
            Font fNormal = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, Color.BLACK);
            Font fMuted = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
            Font fHeaderTabla = new Font(Font.HELVETICA, 8.5f, Font.BOLD, Color.WHITE);
            Font fCae = new Font(Font.HELVETICA, 10, Font.BOLD, AZUL_ARCA);
            Font fCodigoBarras = new Font(Font.COURIER, 9, Font.BOLD, Color.BLACK);

            // Banner superior: ORIGINAL
            Paragraph pOriginal = new Paragraph("ORIGINAL", new Font(Font.HELVETICA, 9, Font.BOLD, Color.DARK_GRAY));
            pOriginal.setAlignment(Element.ALIGN_CENTER);
            pOriginal.setSpacingAfter(4);
            document.add(pOriginal);

            // Cabecera fiscal (3 columnas: Emisor (45%) | Recuadro Tipo (10%) | Comprobante (45%))
            PdfPTable headerTable = new PdfPTable(new float[]{4.5f, 1.2f, 4.3f});
            headerTable.setWidthPercentage(100);

            // Columna 1: Emisor
            PdfPCell cellEmisor = new PdfPCell();
            cellEmisor.setBorder(Rectangle.BOX);
            cellEmisor.setBorderColor(BORDE_SUAVE);
            cellEmisor.setPadding(8);
            cellEmisor.addElement(new Paragraph(arcaService.getRazonSocialEmisor(), new Font(Font.HELVETICA, 11, Font.BOLD, AZUL_ARCA)));
            cellEmisor.addElement(new Paragraph("CuidadoCoop · Cuidados Integrales", fNormal));
            cellEmisor.addElement(new Paragraph("Domicilio Comercial: " + arcaService.getDomicilioEmisor(), fNormal));
            cellEmisor.addElement(new Paragraph("Condición frente al IVA: " + arcaService.getCondicionIvaEmisor(), fNormal));
            headerTable.addCell(cellEmisor);

            // Columna 2: Recuadro con la Letra del comprobante
            PdfPCell cellLetra = new PdfPCell();
            cellLetra.setBorder(Rectangle.BOX);
            cellLetra.setBorderColor(AZUL_ARCA);
            cellLetra.setBackgroundColor(GRIS_FONDO);
            cellLetra.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellLetra.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellLetra.setPadding(4);

            Paragraph pLetra = new Paragraph(factura.getTipoComprobante().getLetra(), fLetra);
            pLetra.setAlignment(Element.ALIGN_CENTER);
            Paragraph pCod = new Paragraph("COD. " + factura.getTipoComprobante().getCodigoAfip(), fCodLetra);
            pCod.setAlignment(Element.ALIGN_CENTER);
            cellLetra.addElement(pLetra);
            cellLetra.addElement(pCod);
            headerTable.addCell(cellLetra);

            // Columna 3: Datos Comprobante
            PdfPCell cellComp = new PdfPCell();
            cellComp.setBorder(Rectangle.BOX);
            cellComp.setBorderColor(BORDE_SUAVE);
            cellComp.setPadding(8);
            cellComp.addElement(new Paragraph(factura.getTipoComprobante().getDescripcion().toUpperCase(), fTitDoc));
            cellComp.addElement(new Paragraph("Punto de Venta: " + factura.getPuntoVenta() + "   Comp. Nro: " + factura.getNumeroComprobante(), fNegrita));
            cellComp.addElement(new Paragraph("Fecha de Emisión: " + factura.getFechaEmision().format(FMT_FECHA), fNormal));
            cellComp.addElement(new Paragraph("CUIT: " + formatearCuit(arcaService.getCuitEmisor()), fNormal));
            cellComp.addElement(new Paragraph("Ingresos Brutos: " + arcaService.getIngresosBrutosEmisor(), fNormal));
            cellComp.addElement(new Paragraph("Fecha Inicio de Actividades: " + arcaService.getInicioActividadesEmisor(), fNormal));
            headerTable.addCell(cellComp);

            document.add(headerTable);

            // Período de servicio
            PdfPTable tPeriodo = new PdfPTable(3);
            tPeriodo.setWidthPercentage(100);
            tPeriodo.setSpacingBefore(4);

            PdfPCell cPer1 = celdaGris("Período Facturado Desde: " + (factura.getPeriodoDesde() != null ? factura.getPeriodoDesde().format(FMT_FECHA) : "-"), fNormal);
            PdfPCell cPer2 = celdaGris("Hasta: " + (factura.getPeriodoHasta() != null ? factura.getPeriodoHasta().format(FMT_FECHA) : "-"), fNormal);
            PdfPCell cPer3 = celdaGris("Vto. para el Pago: " + (factura.getFechaVencimientoPago() != null ? factura.getFechaVencimientoPago().format(FMT_FECHA) : "-"), fNormal);
            tPeriodo.addCell(cPer1);
            tPeriodo.addCell(cPer2);
            tPeriodo.addCell(cPer3);
            document.add(tPeriodo);

            // Datos del Receptor
            PdfPTable tReceptor = new PdfPTable(2);
            tReceptor.setWidthPercentage(100);
            tReceptor.setSpacingBefore(6);

            PdfPCell cRecLeft = new PdfPCell();
            cRecLeft.setBorder(Rectangle.BOX);
            cRecLeft.setBorderColor(BORDE_SUAVE);
            cRecLeft.setPadding(8);

            String etiquetaDoc = factura.getTipoDocumentoReceptor() != null ? factura.getTipoDocumentoReceptor().getDescripcion() : "Documento";
            cRecLeft.addElement(new Paragraph(etiquetaDoc + ": " + factura.getDocumentoReceptor(), fNormal));

            String etiquetaNombre = factura.getTipoReceptor() == TipoReceptor.OBRA_SOCIAL
                    ? "Razón Social (Obra Social): " : "Nombre y Apellido: ";
            cRecLeft.addElement(new Paragraph(etiquetaNombre + factura.getDenominacionReceptor(), fNegrita));
            cRecLeft.addElement(new Paragraph("Condición frente al IVA: " + factura.getCondicionIvaReceptor().getDescripcion(), fNormal));

            tReceptor.addCell(cRecLeft);

            PdfPCell cRecRight = new PdfPCell();
            cRecRight.setBorder(Rectangle.BOX);
            cRecRight.setBorderColor(BORDE_SUAVE);
            cRecRight.setPadding(8);
            cRecRight.addElement(new Paragraph("Domicilio Comercial / Particular: " + factura.getDomicilioReceptor(), fNormal));
            cRecRight.addElement(new Paragraph("Condición de Venta: Contado / Transferencia", fNormal));

            if (factura.getTipoReceptor() == TipoReceptor.OBRA_SOCIAL) {
                cRecRight.addElement(new Paragraph("Beneficiario / Paciente: " + (factura.getNombrePacienteAsociado() != null && !factura.getNombrePacienteAsociado().isBlank() ? factura.getNombrePacienteAsociado() : "-"), fNegrita));
                cRecRight.addElement(new Paragraph("Nro. de Afiliado: " + (factura.getNumeroAfiliado() != null && !factura.getNumeroAfiliado().isBlank() ? factura.getNumeroAfiliado() : "Sin especificar"), fNormal));
            } else if (factura.getEmailReceptor() != null && !factura.getEmailReceptor().isBlank()) {
                cRecRight.addElement(new Paragraph("Correo Electrónico: " + factura.getEmailReceptor(), fNormal));
            }
            tReceptor.addCell(cRecRight);

            document.add(tReceptor);

            // Detalle de ítems
            PdfPTable tItems = new PdfPTable(new float[]{1.0f, 5.0f, 1.2f, 1.2f, 1.8f, 1.8f});
            tItems.setWidthPercentage(100);
            tItems.setSpacingBefore(10);

            // Headers
            agregarHeaderTabla(tItems, "Item", fHeaderTabla);
            agregarHeaderTabla(tItems, "Descripción / Servicio", fHeaderTabla);
            agregarHeaderTabla(tItems, "Cantidad", fHeaderTabla);
            agregarHeaderTabla(tItems, "U. Medida", fHeaderTabla);
            agregarHeaderTabla(tItems, "Precio Unit.", fHeaderTabla);
            agregarHeaderTabla(tItems, "Subtotal", fHeaderTabla);

            int itemIndex = 1;
            for (LineaFactura linea : factura.getLineas()) {
                tItems.addCell(celdaItem(String.valueOf(itemIndex++), fNormal, Element.ALIGN_CENTER));
                tItems.addCell(celdaItem(linea.getDescripcion(), fNormal, Element.ALIGN_LEFT));
                tItems.addCell(celdaItem(formatearNumero(linea.getCantidad()), fNormal, Element.ALIGN_RIGHT));
                tItems.addCell(celdaItem(linea.getUnidadMedida(), fNormal, Element.ALIGN_CENTER));
                tItems.addCell(celdaItem(formatearDinero(linea.getPrecioUnitario()), fNormal, Element.ALIGN_RIGHT));
                tItems.addCell(celdaItem(formatearDinero(linea.getSubtotal()), fNormal, Element.ALIGN_RIGHT));
            }

            document.add(tItems);

            // Totales
            PdfPTable tTotales = new PdfPTable(new float[]{6.4f, 3.6f});
            tTotales.setWidthPercentage(100);
            tTotales.setSpacingBefore(8);

            PdfPCell cObs = new PdfPCell();
            cObs.setBorder(Rectangle.BOX);
            cObs.setBorderColor(BORDE_SUAVE);
            cObs.setPadding(6);
            cObs.addElement(new Paragraph("Observaciones:", fNegrita));
            cObs.addElement(new Paragraph(factura.getObservaciones() != null && !factura.getObservaciones().isBlank()
                    ? factura.getObservaciones()
                    : "Servicios de atención y acompañamiento de cuidados cooperativos.", fMuted));
            tTotales.addCell(cObs);

            PdfPCell cTotalesValores = new PdfPCell();
            cTotalesValores.setBorder(Rectangle.BOX);
            cTotalesValores.setBorderColor(BORDE_SUAVE);
            cTotalesValores.setPadding(6);

            PdfPTable tSubTot = new PdfPTable(new float[]{1.5f, 1.5f});
            tSubTot.setWidthPercentage(100);

            agregarFilaTotal(tSubTot, "Subtotal:", formatearDinero(factura.getSubtotal()), fNormal, fNormal);
            if (factura.getPorcentajeIva().compareTo(BigDecimal.ZERO) > 0) {
                agregarFilaTotal(tSubTot, "IVA (" + formatearNumero(factura.getPorcentajeIva()) + "%):", formatearDinero(factura.getImporteIva()), fNormal, fNormal);
            }
            if (factura.getImporteOtrosTributos().compareTo(BigDecimal.ZERO) > 0) {
                agregarFilaTotal(tSubTot, "Otros Tributos:", formatearDinero(factura.getImporteOtrosTributos()), fNormal, fNormal);
            }
            agregarFilaTotal(tSubTot, "Importe Total:", formatearDinero(factura.getTotal()), new Font(Font.HELVETICA, 11, Font.BOLD, AZUL_ARCA), new Font(Font.HELVETICA, 11, Font.BOLD, AZUL_ARCA));

            cTotalesValores.addElement(tSubTot);
            tTotales.addCell(cTotalesValores);

            document.add(tTotales);

            // Pie ARCA: CAE, Vto CAE y Código de Barras
            PdfPTable tArca = new PdfPTable(new float[]{5.0f, 5.0f});
            tArca.setWidthPercentage(100);
            tArca.setSpacingBefore(12);

            PdfPCell cCaeLeft = new PdfPCell();
            cCaeLeft.setBorder(Rectangle.NO_BORDER);
            cCaeLeft.setPadding(4);
            cCaeLeft.addElement(new Paragraph("Comprobante Autorizado por ARCA", new Font(Font.HELVETICA, 9, Font.BOLD, AZUL_ARCA)));
            cCaeLeft.addElement(new Paragraph("Agencia de Recaudación y Control Aduanero (ex AFIP)", fMuted));
            cCaeLeft.addElement(new Paragraph("Código de Barras Fiscal:", fNormal));
            cCaeLeft.addElement(new Paragraph(factura.getCodigoBarras(), fCodigoBarras));
            tArca.addCell(cCaeLeft);

            PdfPCell cCaeRight = new PdfPCell();
            cCaeRight.setBorder(Rectangle.BOX);
            cCaeRight.setBorderColor(AZUL_ARCA);
            cCaeRight.setBackgroundColor(GRIS_FONDO);
            cCaeRight.setPadding(8);
            cCaeRight.addElement(new Paragraph("CAE Nº: " + factura.getCae(), fCae));
            cCaeRight.addElement(new Paragraph("Fecha de Vto. de CAE: " + factura.getFechaVencimientoCae().format(FMT_FECHA), fNegrita));
            cCaeRight.addElement(new Paragraph("Consulta pública oficial QR: afip.gob.ar/fe/qr", fMuted));
            tArca.addCell(cCaeRight);

            document.add(tArca);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar el comprobante PDF de ARCA", e);
        }

        return baos.toByteArray();
    }

    private static PdfPCell celdaGris(String texto, Font fuente) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente));
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(BORDE_SUAVE);
        c.setBackgroundColor(GRIS_FONDO);
        c.setPadding(5);
        return c;
    }

    private static void agregarHeaderTabla(PdfPTable table, String texto, Font fuente) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente));
        c.setBackgroundColor(AZUL_ARCA);
        c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(AZUL_ARCA);
        table.addCell(c);
    }

    private static PdfPCell celdaItem(String texto, Font fuente, int alineacion) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente));
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(BORDE_SUAVE);
        c.setPadding(5);
        c.setHorizontalAlignment(alineacion);
        return c;
    }

    private static void agregarFilaTotal(PdfPTable table, String label, String valor, Font fLabel, Font fValor) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, fLabel));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(2);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(valor, fValor));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(2);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c2);
    }

    private static String formatearDinero(BigDecimal valor) {
        if (valor == null) {
            return "$ 0,00";
        }
        DecimalFormat df = new DecimalFormat("$ #,##0.00", DecimalFormatSymbols.getInstance(new Locale("es", "AR")));
        return df.format(valor);
    }

    private static String formatearNumero(BigDecimal valor) {
        if (valor == null) {
            return "0,00";
        }
        DecimalFormat df = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(new Locale("es", "AR")));
        return df.format(valor);
    }

    private static String formatearCuit(String cuit) {
        if (cuit == null || cuit.length() != 11) {
            return cuit != null ? cuit : "";
        }
        return cuit.substring(0, 2) + "-" + cuit.substring(2, 10) + "-" + cuit.substring(10);
    }
}
