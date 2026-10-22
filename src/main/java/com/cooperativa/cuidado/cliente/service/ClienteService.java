package com.cooperativa.cuidado.cliente.service;

import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.legajo.TipoLegajo;
import com.cooperativa.cuidado.legajo.service.LegajoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final LegajoService legajoService;

    public ClienteService(
            ClienteRepository clienteRepository,
            AsistenciaRepository asistenciaRepository,
            LegajoService legajoService) {
        this.clienteRepository = clienteRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.legajoService = legajoService;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findAll(Sort.by("nombrePaciente"));
    }

    @Transactional(readOnly = true)
    public Cliente obtener(Long id) {
        return buscar(id);
    }

    @Transactional
    public Cliente registrar(@Valid Cliente cliente) {
        cliente.setId(null);
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente actualizar(Long id, @Valid Cliente datos) {
        Cliente cliente = buscar(id);
        cliente.setNombreFamiliar(datos.getNombreFamiliar());
        cliente.setTelefono(datos.getTelefono());
        cliente.setCorreo(datos.getCorreo());
        cliente.setNombrePaciente(datos.getNombrePaciente());
        cliente.setDomicilio(datos.getDomicilio());
        cliente.setObservacionMedica(datos.getObservacionMedica());
        return clienteRepository.save(cliente);
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscar(id);
        asistenciaRepository.deleteAllByClienteId(id);
        asistenciaRepository.flush();
        legajoService.eliminarLegajo(TipoLegajo.CLIENTE, id);
        clienteRepository.delete(cliente);
    }

    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente con ID " + id));
    }
}
