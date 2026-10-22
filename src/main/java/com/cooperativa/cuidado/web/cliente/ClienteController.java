package com.cooperativa.cuidado.web.cliente;

import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @InitBinder
    void configurarBinder(WebDataBinder binder) {
        binder.setDisallowedFields("id");
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        return "clientes";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return formulario(model, new Cliente(), "Nuevo cliente", "/clientes");
    }

    @PostMapping
    public String crear(
            @Valid @ModelAttribute("cliente") Cliente cliente,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return formulario(model, null, "Nuevo cliente", "/clientes");
        }
        Cliente guardado = clienteService.registrar(cliente);
        redirect.addFlashAttribute("mensajeExito", "Se registró al cliente " + guardado.getNombrePaciente()
                + ". Ahora podés crear su servicio domiciliario.");
        return "redirect:/clientes";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        return formulario(model, clienteService.obtener(id), "Editar cliente", "/clientes/" + id, id);
    }

    @PostMapping("/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("cliente") Cliente cliente,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return formulario(model, null, "Editar cliente", "/clientes/" + id, id);
        }
        clienteService.actualizar(id, cliente);
        redirect.addFlashAttribute("mensajeExito", "Los datos del cliente se actualizaron correctamente.");
        return "redirect:/clientes";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        clienteService.eliminar(id);
        redirect.addFlashAttribute("mensajeExito",
                "El cliente, sus servicios y los turnos asociados se eliminaron.");
        return "redirect:/clientes";
    }

    private String formulario(Model model, Cliente cliente, String titulo, String accion) {
        return formulario(model, cliente, titulo, accion, null);
    }

    private String formulario(Model model, Cliente cliente, String titulo, String accion, Long legajoId) {
        if (cliente != null) {
            model.addAttribute("cliente", cliente);
        }
        model.addAttribute("legajoId", legajoId);
        model.addAttribute("legajoTipo", "clientes");
        model.addAttribute("titulo", titulo);
        model.addAttribute("accion", accion);
        return "cliente-form";
    }
}
