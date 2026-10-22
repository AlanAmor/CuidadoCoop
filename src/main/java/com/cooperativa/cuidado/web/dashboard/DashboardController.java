package com.cooperativa.cuidado.web.dashboard;

import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class DashboardController {

    private final SocioRepository socioRepository;
    private final AsistenciaRepository asistenciaRepository;

    public DashboardController(
            SocioRepository socioRepository,
            AsistenciaRepository asistenciaRepository) {
        this.socioRepository = socioRepository;
        this.asistenciaRepository = asistenciaRepository;
    }

    @GetMapping("/")
    public String mostrarDashboard(Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDate primerDiaMes = hoy.withDayOfMonth(1);
        BigDecimal horasMes = asistenciaRepository.sumarHorasCargadas(
                primerDiaMes, hoy.withDayOfMonth(hoy.lengthOfMonth()));

        model.addAttribute("sociosEnServicio",
                socioRepository.countByEstado(EstadoSocio.ACTIVO_EN_SERVICIO));
        model.addAttribute("sociosDisponibles",
                socioRepository.countByEstado(EstadoSocio.DISPONIBLE_REEMPLAZO));
        model.addAttribute("horasMes", horasMes == null ? BigDecimal.ZERO : horasMes);
        model.addAttribute("fechaActual", hoy);
        return "dashboard";
    }
}
