package com.cooperativa.cuidado.datos;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.facturacion.ConceptoFactura;
import com.cooperativa.cuidado.facturacion.CondicionIva;
import com.cooperativa.cuidado.facturacion.FacturaRepository;
import com.cooperativa.cuidado.facturacion.TipoComprobante;
import com.cooperativa.cuidado.facturacion.TipoDocumentoReceptor;
import com.cooperativa.cuidado.facturacion.TipoReceptor;
import com.cooperativa.cuidado.facturacion.dto.EmisionFacturaForm;
import com.cooperativa.cuidado.facturacion.dto.ItemLineaFacturaForm;
import com.cooperativa.cuidado.facturacion.service.ArcaFacturacionService;
import com.cooperativa.cuidado.liquidacionfamilia.dto.EmisionLiquidacionFamiliaForm;
import com.cooperativa.cuidado.liquidacionfamilia.service.LiquidacionFamiliaService;
import com.cooperativa.cuidado.obrasocial.ObraSocial;
import com.cooperativa.cuidado.obrasocial.ObraSocialRepository;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import com.cooperativa.cuidado.servicio.TipoServicio;
import com.cooperativa.cuidado.servicio.TipoServicioRepository;
import com.cooperativa.cuidado.socio.Especialidad;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Carga inicial de datos demo realistas (socios, clientes, servicios, turnos y facturas)
 * para evaluar y testear todas las funcionalidades del sistema CuidadoCoop en localhost.
 */
@Component
@Order(10)
public class DatosDemoInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemoInitializer.class);

    private final SocioRepository socioRepository;
    private final ClienteRepository clienteRepository;
    private final TipoServicioRepository tipoServicioRepository;
    private final ServicioDomicilioRepository servicioDomicilioRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final FacturaRepository facturaRepository;
    private final LiquidacionFamiliaService liquidacionFamiliaService;
    private final ArcaFacturacionService arcaFacturacionService;
    private final ObraSocialRepository obraSocialRepository;

    @Value("${app.datos.demo:true}")
    private boolean habilitado;

    public DatosDemoInitializer(
            SocioRepository socioRepository,
            ClienteRepository clienteRepository,
            TipoServicioRepository tipoServicioRepository,
            ServicioDomicilioRepository servicioDomicilioRepository,
            AsistenciaRepository asistenciaRepository,
            FacturaRepository facturaRepository,
            LiquidacionFamiliaService liquidacionFamiliaService,
            ArcaFacturacionService arcaFacturacionService,
            ObraSocialRepository obraSocialRepository) {
        this.socioRepository = socioRepository;
        this.clienteRepository = clienteRepository;
        this.tipoServicioRepository = tipoServicioRepository;
        this.servicioDomicilioRepository = servicioDomicilioRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.facturaRepository = facturaRepository;
        this.liquidacionFamiliaService = liquidacionFamiliaService;
        this.arcaFacturacionService = arcaFacturacionService;
        this.obraSocialRepository = obraSocialRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!habilitado) {
            return;
        }

        log.info("Inicializando datos demo realistas para CuidadoCoop...");

        // 1. Asegurar tipos de servicio y servicios de domicilio
        TipoServicio tipoHoras = asegurarTipoServicio("Por horas");
        TipoServicio tipo247 = asegurarTipoServicio("Servicio 24/7");
        TipoServicio tipoEnfermeria = asegurarTipoServicio("Enfermería express");

        ServicioDomicilio servCuidadorDiurno = asegurarServicioDomicilio(
                tipoHoras,
                new BigDecimal("4200.00"),
                new BigDecimal("4000.00"),
                new BigDecimal("4900.00"),
                new BigDecimal("4700.00"),
                new BigDecimal("5800.00"),
                new BigDecimal("5500.00"),
                new BigDecimal("3300.00"),
                new BigDecimal("3150.00"),
                new BigDecimal("3850.00"),
                new BigDecimal("3700.00"),
                new BigDecimal("4550.00"),
                new BigDecimal("4300.00"));

        ServicioDomicilio servNocturno = asegurarServicioDomicilio(
                tipo247,
                new BigDecimal("4800.00"),
                new BigDecimal("4500.00"),
                new BigDecimal("5600.00"),
                new BigDecimal("5300.00"),
                new BigDecimal("6700.00"),
                new BigDecimal("6300.00"),
                new BigDecimal("3750.00"),
                new BigDecimal("3500.00"),
                new BigDecimal("4400.00"),
                new BigDecimal("4150.00"),
                new BigDecimal("5250.00"),
                new BigDecimal("4950.00"));

        ServicioDomicilio servEnfermeria = asegurarServicioDomicilio(
                tipoEnfermeria,
                new BigDecimal("6200.00"),
                new BigDecimal("5900.00"),
                new BigDecimal("7200.00"),
                new BigDecimal("6900.00"),
                new BigDecimal("8600.00"),
                new BigDecimal("8200.00"),
                new BigDecimal("4900.00"),
                new BigDecimal("4650.00"),
                new BigDecimal("5700.00"),
                new BigDecimal("5450.00"),
                new BigDecimal("6800.00"),
                new BigDecimal("6500.00"));

        // 2. Socios realistas (cuidadores y enfermeros con domicilios en Santa Fe / Rosario)
        List<Socio> socios = new ArrayList<>();
        socios.add(crearOSocio(
                "Ana", "Gómez", "20900001", "27-20900001-4", "San Martín 2450", "Santa Fe", "3000",
                "342-4521890", "ana.gomez@cuidadocoop.org", "2850590900000000000001",
                EstadoSocio.ACTIVO_EN_SERVICIO, Especialidad.CUIDADOR,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)));

        socios.add(crearOSocio(
                "Carlos", "Pérez", "28345678", "20-28345678-3", "Bv. Pellegrini 3120", "Santa Fe", "3000",
                "342-5123456", "carlos.perez@cuidadocoop.org", "2850590900000000000002",
                EstadoSocio.ACTIVO_EN_SERVICIO, Especialidad.ENFERMERO,
                EnumSet.allOf(DayOfWeek.class)));

        socios.add(crearOSocio(
                "Laura", "Rodríguez", "31456789", "27-31456789-5", "Urquiza 1845", "Santa Fe", "3000",
                "342-6234567", "laura.rodriguez@cuidadocoop.org", "2850590900000000000003",
                EstadoSocio.ACTIVO_EN_SERVICIO, Especialidad.CUIDADOR,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)));

        socios.add(crearOSocio(
                "Martín", "Benítez", "33567890", "20-33567890-8", "Av. Freyre 2250", "Santa Fe", "3000",
                "342-4890123", "martin.benitez@cuidadocoop.org", "2850590900000000000004",
                EstadoSocio.ACTIVO_EN_SERVICIO, Especialidad.ENFERMERO,
                EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)));

        socios.add(crearOSocio(
                "Valeria", "Rossi", "35678901", "27-35678901-2", "General Paz 5420", "Santa Fe", "3000",
                "342-5901234", "valeria.rossi@cuidadocoop.org", "2850590900000000000005",
                EstadoSocio.ACTIVO_EN_SERVICIO, Especialidad.CUIDADOR,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)));

        socios.add(crearOSocio(
                "Esteban", "Morales", "29789012", "20-29789012-6", "Cordoba 1420", "Rosario", "2000",
                "341-4321987", "esteban.morales@cuidadocoop.org", "2850590900000000000006",
                EstadoSocio.DISPONIBLE_REEMPLAZO, Especialidad.CUIDADOR,
                EnumSet.allOf(DayOfWeek.class)));

        socios.add(crearOSocio(
                "Sofía", "Herrera", "36890123", "27-36890123-9", "Oroño 850", "Rosario", "2000",
                "341-5432109", "sofia.herrera@cuidadocoop.org", "2850590900000000000007",
                EstadoSocio.ACTIVO_EN_SERVICIO, Especialidad.ENFERMERO,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)));

        // 3. Clientes y pacientes con diagnósticos y familiares realistas
        List<Cliente> clientes = new ArrayList<>();
        clientes.add(crearOCliente(
                "Mariana Rostagno", "342-4112233", "mrostagno@gmail.com",
                "Elena Rostagno", "4125896", "4 de Enero 3420 4to B", "Santa Fe", "Centro", "3000",
                "Paciente de 84 años con demencia senil moderada y movilidad reducida. Requiere asistencia para higienización, marcha con andador y administración de medicación diaria. Dieta hiposódica."));

        clientes.add(crearOCliente(
                "Gonzalo Martínez", "342-5223344", "gonzalo.mtz@hotmail.com",
                "Héctor Martínez", "6321458", "Av. 7 de Marzo 1850", "Santo Tomé", "Centro", "3016",
                "Paciente de 79 años en postoperatorio de fractura de cadera. Requiere cuidados de enfermería, cambio de vendajes y control de signos vitales mañana y tarde."));

        clientes.add(crearOCliente(
                "Carolina Benavídez", "342-6334455", "caro.benavidez@yahoo.com.ar",
                "Rosa Benavídez", "5894123", "Salvador del Carril 2100", "Santa Fe", "Guadalupe", "3000",
                "Paciente de 88 años con secuelas de ACV isquémico leve y Parkinson inicial. Guardia nocturna activa de 12 horas para prevención de caídas y asistencia en traslados."));

        clientes.add(crearOCliente(
                "Federico Giménez", "341-4778899", "fgimenez@empresa.com.ar",
                "Juan Ignacio Giménez", "8521478", "Bv. Oroño 1230 Piso 2", "Rosario", "Centro", "2000",
                "Paciente de 76 años hipertenso y diabético tipo 2. Control glucémico capilar matutino, administración de insulina subcutánea y acompañamiento general."));

        clientes.add(crearOCliente(
                "Patricia Albornoz", "342-4889900", "patricia_albornoz@gmail.com",
                "Teresa Fernández", "7412589", "Castelli 3920", "Santa Fe", "Fomento 9 de Julio", "3000",
                "Paciente de 82 años con hipertensión arterial crónica y dificultades motrices. Cuidados integrales de lunes a viernes en turno diurno."));

        // 4. Asistencias (turnos) con distribución realista: pasados (completados con check-in/out), hoy, y futuros
        if (asistenciaRepository.count() == 0) {
            LocalDate hoy = LocalDate.now();
            Socio ana = socios.get(0);
            Socio carlos = socios.get(1);
            Socio laura = socios.get(2);
            Socio martin = socios.get(3);
            Socio valeria = socios.get(4);

            Cliente elena = clientes.get(0);
            Cliente hector = clientes.get(1);
            Cliente rosa = clientes.get(2);
            Cliente teresa = clientes.get(4);

            // Turnos pasados (semanas recientes del mes actual para que haya datos en liquidación)
            for (int i = 14; i >= 1; i--) {
                LocalDate fecha = hoy.minusDays(i);
                DayOfWeek dow = fecha.getDayOfWeek();

                // Elena: turnos diurnos de lunes a viernes (Ana o Valeria)
                if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                    Socio soc = (i % 2 == 0) ? ana : valeria;
                    crearTurno(soc, elena, servCuidadorDiurno, fecha,
                            LocalTime.of(8, 0), LocalTime.of(16, 0),
                            fecha.atTime(7, 58), fecha.atTime(16, 5),
                            "Turno realizado con normalidad. Se administró medicación indicada.",
                            BigDecimal.valueOf(1500), BigDecimal.ZERO);
                }

                // Rosa: guardias nocturnas 20:00 a 08:00 (Laura)
                if (i % 2 == 0) {
                    crearTurno(laura, rosa, servNocturno, fecha,
                            LocalTime.of(20, 0), LocalTime.of(8, 0),
                            fecha.atTime(19, 55), fecha.plusDays(1).atTime(8, 10),
                            "Guardia nocturna sin novedades. Descanso adecuado de la paciente.",
                            BigDecimal.valueOf(2000), BigDecimal.ZERO);
                }

                // Héctor: enfermería express interdiaria (Carlos)
                if (i % 3 == 0) {
                    crearTurno(carlos, hector, servEnfermeria, fecha,
                            LocalTime.of(10, 0), LocalTime.of(12, 0),
                            fecha.atTime(9, 57), fecha.atTime(12, 3),
                            "Curación de herida quirúrgica y registro de tensión arterial (125/80).",
                            BigDecimal.ZERO, BigDecimal.valueOf(1200));
                }
            }

            // Turnos de HOY
            // 1. En curso (entrada marcada, salida pendiente)
            crearTurno(ana, elena, servCuidadorDiurno, hoy,
                    LocalTime.of(8, 0), LocalTime.of(16, 0),
                    hoy.atTime(7, 59), null,
                    "En curso. Asistencia para desayuno y ejercicios de movilidad matutina.",
                    BigDecimal.valueOf(1500), BigDecimal.ZERO);

            // 2. Por iniciar hoy en la tarde
            crearTurno(martin, hector, servEnfermeria, hoy,
                    LocalTime.of(17, 0), LocalTime.of(19, 0),
                    null, null,
                    "Control vespertino de signos vitales y medicación reglada.",
                    BigDecimal.ZERO, BigDecimal.valueOf(1200));

            // 3. Guardia nocturna de hoy
            crearTurno(laura, rosa, servNocturno, hoy,
                    LocalTime.of(20, 0), LocalTime.of(8, 0),
                    null, null,
                    "Guardia nocturna programada 12 hs.",
                    BigDecimal.valueOf(2000), BigDecimal.ZERO);

            // Turnos FUTUROS (próximos 7 a 10 días para ver en calendario y lista de turnos)
            for (int i = 1; i <= 8; i++) {
                LocalDate fecha = hoy.plusDays(i);
                DayOfWeek dow = fecha.getDayOfWeek();

                if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                    // Turno con Teresa
                    crearTurno(valeria, teresa, servCuidadorDiurno, fecha,
                            LocalTime.of(9, 0), LocalTime.of(15, 0),
                            null, null,
                            "Cuidado integral diurno programado.",
                            BigDecimal.valueOf(1000), BigDecimal.ZERO);

                    // Turno con Elena
                    crearTurno(ana, elena, servCuidadorDiurno, fecha,
                            LocalTime.of(8, 0), LocalTime.of(16, 0),
                            null, null,
                            "Acompañamiento diurno y preparación de almuerzo.",
                            BigDecimal.valueOf(1500), BigDecimal.ZERO);
                }

                if (i % 2 != 0) {
                    crearTurno(laura, rosa, servNocturno, fecha,
                            LocalTime.of(20, 0), LocalTime.of(8, 0),
                            null, null,
                            "Guardia nocturna programada.",
                            BigDecimal.valueOf(2000), BigDecimal.ZERO);
                }

                if (i % 3 == 0) {
                    crearTurno(carlos, hector, servEnfermeria, fecha,
                            LocalTime.of(10, 0), LocalTime.of(12, 0),
                            null, null,
                            "Curación y control de herida postquirúrgica.",
                            BigDecimal.ZERO, BigDecimal.valueOf(1200));
                }
            }

            log.info("Turnos realistas generados exitosamente.");
        }

        // 5. Pre-generar Liquidación Familia y Facturas ARCA realistas para testeo inmediato
        generarComprobantesDemoSiNoExisten(clientes);

        // 6. Padrón inicial de Obras Sociales y Prepagas
        asegurarPadronObrasSociales();

        log.info("Datos demo de CuidadoCoop inicializados correctamente.");
    }

    private void asegurarPadronObrasSociales() {
        if (obraSocialRepository.count() > 0) {
            return;
        }

        obraSocialRepository.saveAll(List.of(
                new ObraSocial("PAMI - INSSJP", "30-52625211-9", "INSTITUTO NACIONAL DE SERVICIOS SOCIALES PARA JUBILADOS Y PENSIONADOS", CondicionIva.IVA_EXENTO, "Av. Corrientes 655, CABA"),
                new ObraSocial("I.A.P.O.S.", "30-65481234-8", "INSTITUTO AUTARQUICO PROVINCIAL DE OBRA SOCIAL", CondicionIva.IVA_EXENTO, "San Martín 3145, Santa Fe"),
                new ObraSocial("OSDE Binario", "30-54674125-3", "ORGANIZACION DE SERVICIOS DIRECTOS EMPRESARIOS", CondicionIva.IVA_RESPONSABLE_INSCRIPTO, "Av. Leandro N. Alem 1067, CABA"),
                new ObraSocial("OSECAC", "30-54877408-6", "OBRA SOCIAL DE LOS EMPLEADOS DE COMERCIO Y ACTIVIDADES CIVILES", CondicionIva.IVA_EXENTO, "Moreno 648, CABA"),
                new ObraSocial("Swiss Medical", "30-67123456-1", "SWISS MEDICAL S.A.", CondicionIva.IVA_RESPONSABLE_INSCRIPTO, "Av. Pueyrredón 1441, CABA"),
                new ObraSocial("Medicus", "30-56214789-4", "MEDICUS S.A. DE ASISTENCIA MEDICA Y CIENTIFICA", CondicionIva.IVA_RESPONSABLE_INSCRIPTO, "Azcuénaga 1252, CABA"),
                new ObraSocial("Galeno Argentina", "30-68954123-7", "GALENO ARGENTINA S.A.", CondicionIva.IVA_RESPONSABLE_INSCRIPTO, "Av. Córdoba 1712, CABA"),
                new ObraSocial("UPCN / Unión Personal", "30-64125896-3", "OBRA SOCIAL DE LA UNION DEL PERSONAL CIVIL DE LA NACION", CondicionIva.IVA_EXENTO, "Tucumán 949, CABA")
        ));
        log.info("Padrón inicial de obras sociales cargado exitosamente.");
    }

    private void generarComprobantesDemoSiNoExisten(List<Cliente> clientes) {
        if (facturaRepository.count() > 0) {
            return;
        }

        try {
            Cliente elena = clientes.get(0);
            Cliente rosa = clientes.get(2);
            LocalDate hoy = LocalDate.now();
            YearMonth ym = YearMonth.from(hoy);

            // Emitir una liquidación familiar para Elena
            EmisionLiquidacionFamiliaForm formLiq = new EmisionLiquidacionFamiliaForm();
            formLiq.setClienteId(elena.getId());
            formLiq.setMes(ym.getMonthValue());
            formLiq.setAnio(ym.getYear());
            formLiq.setPeriodo(ym.toString());
            formLiq.setPorcentajeIva(new BigDecimal("0.00"));
            var liqEmitida = liquidacionFamiliaService.emitir(formLiq);

            // Factura 1: Factura B a Consumidor Final (Elena Rostagno) prellenada desde la liquidación
            EmisionFacturaForm formFact1 = arcaFacturacionService.prepararFormularioDesdeLiquidacionFamilia(liqEmitida.getId());
            formFact1.setPuntoVenta("0001");
            formFact1.setCondicionIvaReceptor(CondicionIva.CONSUMIDOR_FINAL);
            arcaFacturacionService.emitirFactura(formFact1);

            // Factura 2: Factura B a Obra Social (PAMI / INSSJP) para el paciente Rosa Benavídez
            EmisionFacturaForm formFact2 = new EmisionFacturaForm();
            formFact2.setTipoComprobante(TipoComprobante.FACTURA_B);
            formFact2.setPuntoVenta("0001");
            formFact2.setFechaEmision(hoy);
            formFact2.setTipoReceptor(TipoReceptor.OBRA_SOCIAL);
            formFact2.setTipoDocumentoReceptor(TipoDocumentoReceptor.CUIT);
            formFact2.setDocumentoReceptor("30522763922");
            formFact2.setDenominacionReceptor("INSTITUTO NACIONAL DE SERVICIOS SOCIALES PARA JUBILADOS Y PENSIONADOS");
            formFact2.setObraSocialNombre("PAMI - INSSJP");
            formFact2.setObraSocialCuit("30522763922");
            formFact2.setCondicionIvaReceptor(CondicionIva.IVA_EXENTO);
            formFact2.setDomicilioReceptor("Av. Corrientes 655, CABA");
            formFact2.setNombrePacienteAsociado("Rosa Benavídez");
            formFact2.setNumeroAfiliado("150389281200");
            formFact2.setClienteId(rosa.getId());
            formFact2.setConcepto(ConceptoFactura.SERVICIOS);
            formFact2.setObservaciones("Módulo de Cuidados Domiciliarios Integrales - Autorización N° PAMI-78412/2026.");

            ItemLineaFacturaForm lineaOs = new ItemLineaFacturaForm(
                    "Servicio de Acompañamiento y Cuidado Domiciliario Nocturno 12 hs",
                    new BigDecimal("15.00"),
                    "turnos",
                    new BigDecimal("5600.00"),
                    BigDecimal.ZERO,
                    new BigDecimal("84000.00"));
            formFact2.setLineas(List.of(lineaOs));

            arcaFacturacionService.emitirFactura(formFact2);

            log.info("Liquidación de muestra y Facturas ARCA (Consumidor Final y Obra Social) generadas con éxito.");
        } catch (Exception e) {
            log.warn("No se pudieron pre-generar los comprobantes ARCA de prueba: {}", e.getMessage());
        }
    }

    private TipoServicio asegurarTipoServicio(String nombre) {
        return tipoServicioRepository.findAll().stream()
                .filter(t -> t.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .orElseGet(() -> tipoServicioRepository.save(new TipoServicio(nombre)));
    }

    private ServicioDomicilio asegurarServicioDomicilio(
            TipoServicio tipo,
            BigDecimal valSemanaMenos12,
            BigDecimal valSemanaMas12,
            BigDecimal valFindeMenos12,
            BigDecimal valFindeMas12,
            BigDecimal valFeriadoMenos12,
            BigDecimal valFeriadoMas12,
            BigDecimal valSocioSemanaMenos12,
            BigDecimal valSocioSemanaMas12,
            BigDecimal valSocioFindeMenos12,
            BigDecimal valSocioFindeMas12,
            BigDecimal valSocioFeriadoMenos12,
            BigDecimal valSocioFeriadoMas12) {

        List<ServicioDomicilio> existentes = servicioDomicilioRepository.findAll();
        for (ServicioDomicilio s : existentes) {
            if (s.getTipoServicio() != null && s.getTipoServicio().getId().equals(tipo.getId())) {
                return s;
            }
        }

        ServicioDomicilio serv = new ServicioDomicilio();
        serv.setTipoServicio(tipo);
        serv.setValorHora(valSemanaMenos12);
        serv.setValorHoraClienteSemanaMas12(valSemanaMas12);
        serv.setValorHoraClienteFindeMenos12(valFindeMenos12);
        serv.setValorHoraClienteFindeMas12(valFindeMas12);
        serv.setValorHoraClienteFeriadoMenos12(valFeriadoMenos12);
        serv.setValorHoraClienteFeriadoMas12(valFeriadoMas12);

        serv.setValorHoraSocio(valSocioSemanaMenos12);
        serv.setValorHoraSocioSemanaMas12(valSocioSemanaMas12);
        serv.setValorHoraSocioFindeMenos12(valSocioFindeMenos12);
        serv.setValorHoraSocioFindeMas12(valSocioFindeMas12);
        serv.setValorHoraSocioFeriadoMenos12(valSocioFeriadoMenos12);
        serv.setValorHoraSocioFeriadoMas12(valSocioFeriadoMas12);

        return servicioDomicilioRepository.save(serv);
    }

    private Socio crearOSocio(
            String nombre, String apellido, String dni, String cuil, String direccion,
            String localidad, String cp, String telefono, String correo, String cbu,
            EstadoSocio estado, Especialidad especialidad, Set<DayOfWeek> dias) {

        return socioRepository.findByDni(dni).orElseGet(() -> {
            Socio s = new Socio();
            s.setNombre(nombre);
            s.setApellido(apellido);
            s.setDni(dni);
            s.setCuil(cuil);
            s.setDireccion(direccion);
            s.setLocalidad(localidad);
            s.setCodigoPostal(cp);
            s.setTelefono(telefono);
            s.setCorreo(correo);
            s.setCbu(cbu);
            s.setEstado(estado);
            s.setEspecialidad(especialidad);
            s.setDiasDisponibles(dias);
            return socioRepository.save(s);
        });
    }

    private Cliente crearOCliente(
            String familiar, String telefono, String correo,
            String paciente, String dniPaciente, String domicilio,
            String ciudad, String localidad, String cp, String observaciones) {

        return clienteRepository.findAll().stream()
                .filter(c -> c.getNombrePaciente().equalsIgnoreCase(paciente))
                .findFirst()
                .orElseGet(() -> {
                    Cliente c = new Cliente();
                    c.setNombreFamiliar(familiar);
                    c.setTelefono(telefono);
                    c.setCorreo(correo);
                    c.setNombrePaciente(paciente);
                    c.setDniPaciente(dniPaciente);
                    c.setDomicilio(domicilio);
                    c.setCiudad(ciudad);
                    c.setLocalidad(localidad);
                    c.setCodigoPostal(cp);
                    c.setObservacionMedica(observaciones);
                    return clienteRepository.save(c);
                });
    }

    private void crearTurno(
            Socio socio, Cliente cliente, ServicioDomicilio servicio, LocalDate fecha,
            LocalTime inicio, LocalTime fin, LocalDateTime marcacionEntrada,
            LocalDateTime marcacionSalida, String aclaracion, BigDecimal viatico, BigDecimal sanatorio) {

        Asistencia a = new Asistencia();
        a.setSocio(socio);
        a.setCliente(cliente);
        a.setServicioDomicilio(servicio);
        a.setFecha(fecha);
        a.setHoraInicio(inicio);
        a.setHoraFin(fin);
        a.setMarcacionEntrada(marcacionEntrada);
        a.setMarcacionSalida(marcacionSalida);
        a.setAclaracionTurno(aclaracion);
        a.setImporteViatico(viatico);
        a.setImporteSanatorio(sanatorio);
        a.calcularHorasTotales();
        a.capturarValorHora();
        asistenciaRepository.save(a);
    }
}
