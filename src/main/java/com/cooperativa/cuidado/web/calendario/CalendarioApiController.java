package com.cooperativa.cuidado.web.calendario;

import com.cooperativa.cuidado.cuenta.UsuarioCuenta;
import com.cooperativa.cuidado.web.seguridad.SesionHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class CalendarioApiController {

    private final CalendarioService calendarioService;
    private final SesionHelper sesionHelper;

    public CalendarioApiController(CalendarioService calendarioService, SesionHelper sesionHelper) {
        this.calendarioService = calendarioService;
        this.sesionHelper = sesionHelper;
    }

    @GetMapping("/api/calendario")
    public CalendarioRespuesta consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long socioId,
            HttpServletRequest request) {
        String dniFiltro = sesionHelper.obtenerUsuarioActual(request)
                .filter(UsuarioCuenta::esSoloTurnosPropios)
                .map(UsuarioCuenta::getDni)
                .orElse(null);
        Long socioIdEfectivo = dniFiltro != null ? null : socioId;
        return calendarioService.consultar(desde, hasta, dniFiltro, socioIdEfectivo);
    }
}
