package com.cooperativa.cuidado.web.documento;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class HistoricoController {

    @GetMapping("/asistencias/historico")
    public String mostrarFormulario(Model model) {
        LocalDate hoy = LocalDate.now();
        model.addAttribute("desde", hoy.withDayOfMonth(1));
        model.addAttribute("hasta", hoy);
        return "historico";
    }
}
