package com.cooperativa.cuidado.web.dashboard;

import com.cooperativa.cuidado.asistencia.Asistencia;
import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cuenta.UsuarioCuenta;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.SocioRepository;
import com.cooperativa.cuidado.web.seguridad.SesionHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class DashboardController {

    private final SocioRepository socioRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final SesionHelper sesionHelper;

    public DashboardController(
            SocioRepository socioRepository,
            AsistenciaRepository asistenciaRepository,
            SesionHelper sesionHelper) {
        this.socioRepository = socioRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.sesionHelper = sesionHelper;
    }

    @GetMapping("/")
    public String mostrarDashboard(Model model, HttpServletRequest request) {
        LocalDate hoy = LocalDate.now();
        LocalDate primerDiaMes = hoy.withDayOfMonth(1);
        LocalDate ultimoDiaMes = hoy.withDayOfMonth(hoy.lengthOfMonth());

        Optional<UsuarioCuenta> usuarioOpt = sesionHelper.obtenerUsuarioActual(request);
        boolean soloTurnosPropios = usuarioOpt.map(UsuarioCuenta::esSoloTurnosPropios).orElse(false);

        if (soloTurnosPropios) {
            UsuarioCuenta cuenta = usuarioOpt.get();
            model.addAttribute("vistaSocioPropia", true);
            socioRepository.findByDni(cuenta.getDni()).ifPresentOrElse(socio -> {
                BigDecimal horasSocio = socioRepository.sumarHorasTrabajadas(socio.getId(), primerDiaMes, ultimoDiaMes);
                boolean tieneTurnos = asistenciaRepository.existsBySocioId(socio.getId());
                List<Asistencia> turnosHoy = asistenciaRepository
                        .findAllBySocioIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(
                                socio.getId(), hoy.minusDays(1), hoy)
                        .stream()
                        .filter(a -> a.esDiaDelTurno(hoy))
                        .toList();
                model.addAttribute("socioVinculado", socio);
                model.addAttribute("horasMes", horasSocio == null ? BigDecimal.ZERO : horasSocio);
                model.addAttribute("sinTurnosAsignados", !tieneTurnos);
                model.addAttribute("turnosHoy", turnosHoy);
            }, () -> {
                model.addAttribute("horasMes", BigDecimal.ZERO);
                model.addAttribute("sinTurnosAsignados", true);
                model.addAttribute("turnosHoy", List.of());
            });
        } else {
            BigDecimal horasMes = asistenciaRepository.sumarHorasCargadas(primerDiaMes, ultimoDiaMes);
            model.addAttribute("vistaSocioPropia", false);
            model.addAttribute("sinTurnosAsignados", false);
            model.addAttribute("sociosEnServicio",
                    socioRepository.countByEstado(EstadoSocio.ACTIVO_EN_SERVICIO));
            model.addAttribute("sociosDisponibles",
                    socioRepository.countByEstado(EstadoSocio.DISPONIBLE_REEMPLAZO));
            model.addAttribute("horasMes", horasMes == null ? BigDecimal.ZERO : horasMes);
        }

        model.addAttribute("fechaActual", hoy);
        return "dashboard";
    }
}
