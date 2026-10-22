package com.cooperativa.cuidado.web.documento;

import com.cooperativa.cuidado.cliente.service.ClienteService;
import com.cooperativa.cuidado.documento.AlicuotaIva;
import com.cooperativa.cuidado.documento.CalculoPresupuesto;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.servicio.ServicioDomicilio;
import com.cooperativa.cuidado.servicio.ServicioDomicilioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class PresupuestoController {

    private final ServicioDomicilioRepository servicioRepository;
    private final ClienteService clienteService;

    public PresupuestoController(ServicioDomicilioRepository servicioRepository, ClienteService clienteService) {
        this.servicioRepository = servicioRepository;
        this.clienteService = clienteService;
    }

    @GetMapping("/presupuestos")
    public String mostrarFormulario(Model model) {
        prepararOpciones(model, null, null, AlicuotaIva.SIN_IVA, false);
        return "presupuestos";
    }

    @GetMapping("/presupuestos/calculo")
    public String calcular(
            @RequestParam(required = false) Long servicioId,
            @RequestParam(required = false) BigDecimal horas,
            @RequestParam(defaultValue = "SIN_IVA") AlicuotaIva alicuotaIva,
            @RequestParam(defaultValue = "false") boolean pagoEfectivo,
            Model model) {
        prepararOpciones(model, servicioId, horas, alicuotaIva, pagoEfectivo);
        if (servicioId == null || horas == null) {
            model.addAttribute("errorOperacion",
                    "Elegí un servicio e indicá las horas estimadas para ver el cálculo.");
            return "presupuestos";
        }
        try {
            ServicioDomicilio servicio = servicioRepository.findById(servicioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "El servicio seleccionado ya no existe. Actualizá la página y elegí otro."));
            model.addAttribute("calculo",
                    CalculoPresupuesto.calcular(horas, servicio.getValorHora(), alicuotaIva, pagoEfectivo));
        } catch (IllegalArgumentException | RecursoNoEncontradoException ex) {
            model.addAttribute("errorOperacion", ex.getMessage());
        }
        return "presupuestos";
    }

    private void prepararOpciones(
            Model model, Long servicioId, BigDecimal horas, AlicuotaIva alicuota, boolean pagoEfectivo) {
        model.addAttribute("servicios", servicioRepository.findAllByOrderByIdAsc());
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("alicuotas", AlicuotaIva.values());
        model.addAttribute("servicioId", servicioId);
        model.addAttribute("horas", horas);
        model.addAttribute("alicuotaIva", alicuota);
        model.addAttribute("pagoEfectivo", pagoEfectivo);
    }
}
