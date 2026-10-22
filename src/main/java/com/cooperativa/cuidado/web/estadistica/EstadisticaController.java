package com.cooperativa.cuidado.web.estadistica;

import com.cooperativa.cuidado.estadistica.service.EstadisticaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Year;

@Controller
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    public EstadisticaController(EstadisticaService estadisticaService) {
        this.estadisticaService = estadisticaService;
    }

    @GetMapping("/estadisticas")
    public String mostrar(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer semana,
            Model model) {
        int anioSeleccionado = anio == null ? Year.now().getValue() : anio;
        model.addAttribute("panel", estadisticaService.generarPanel(anioSeleccionado, mes, semana));
        return "estadisticas";
    }
}
