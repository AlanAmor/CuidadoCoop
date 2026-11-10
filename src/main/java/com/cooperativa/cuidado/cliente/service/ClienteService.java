package com.cooperativa.cuidado.cliente.service;

import com.cooperativa.cuidado.asistencia.AsistenciaRepository;
import com.cooperativa.cuidado.cliente.Cliente;
import com.cooperativa.cuidado.cliente.ClienteRepository;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.legajo.TipoLegajo;
import com.cooperativa.cuidado.legajo.service.LegajoService;
import com.cooperativa.cuidado.liquidacionfamilia.LiquidacionFamiliaRepository;
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
    private final LiquidacionFamiliaRepository liquidacionFamiliaRepository;

    public ClienteService(
            ClienteRepository clienteRepository,
            AsistenciaRepository asistenciaRepository,
            LegajoService legajoService,
            LiquidacionFamiliaRepository liquidacionFamiliaRepository) {
        this.clienteRepository = clienteRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.legajoService = legajoService;
        this.liquidacionFamiliaRepository = liquidacionFamiliaRepository;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findAll(Sort.by("nombrePaciente"));
    }

    @Transactional(readOnly = true)
    public List<Cliente> buscar(String termino, String inicial) {
        String q = termino == null ? null : termino.trim();
        String ini = inicial == null ? null : inicial.trim();
        boolean sinTermino = q == null || q.isEmpty();
        boolean sinInicial = ini == null || ini.isEmpty();
        if (sinTermino && sinInicial) {
            return listar();
        }
        return clienteRepository.buscarIndexado(sinTermino ? null : q, sinInicial ? null : ini);
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
        cliente.setDniPaciente(datos.getDniPaciente());
        cliente.setDomicilio(datos.getDomicilio());
        cliente.setCiudad(datos.getCiudad());
        cliente.setLocalidad(datos.getLocalidad());
        cliente.setCodigoPostal(datos.getCodigoPostal());
        cliente.setObservacionMedica(datos.getObservacionMedica());
        return clienteRepository.save(cliente);
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscar(id);
        liquidacionFamiliaRepository.deleteAllByClienteId(id);
        liquidacionFamiliaRepository.flush();
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
