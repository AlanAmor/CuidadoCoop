package com.cooperativa.cuidado.web.liquidacion;

import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.liquidacion.dto.LiquidacionMensualSocio;
import com.cooperativa.cuidado.liquidacion.service.LiquidacionService;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Controller
public class LiquidacionController {

    private final LiquidacionService liquidacionService;
    private final SocioRepository socioRepository;

    public LiquidacionController(LiquidacionService liquidacionService, SocioRepository socioRepository) {
        this.liquidacionService = liquidacionService;
        this.socioRepository = socioRepository;
    }

    @GetMapping("/liquidaciones")
    public String mostrarFormulario(Model model) {
        model.addAttribute("socios", socioRepository.findAll());
        model.addAttribute("periodoActual", YearMonth.now().toString());
        return "liquidaciones";
    }

    @GetMapping("/liquidaciones/resultado")
    public String consultar(
            @RequestParam Long socioId,
            @RequestParam String periodo,
            Model model) {
        model.addAttribute("socios", socioRepository.findAll());
        model.addAttribute("periodoActual", periodo);
        try {
            YearMonth yearMonth = YearMonth.parse(periodo);
            LiquidacionMensualSocio liquidacion =
                    liquidacionService.calcularRetornoMensual(socioId, yearMonth);
            model.addAttribute("liquidacion", liquidacion);
        } catch (DateTimeParseException ex) {
            model.addAttribute("errorConsulta", "El período debe tener el formato AAAA-MM.");
        } catch (RecursoNoEncontradoException ex) {
            model.addAttribute("errorConsulta", ex.getMessage());
        }
        return "liquidaciones";
    }
}
