package com.cooperativa.cuidado.obrasocial;

import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoDuplicadoException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ObraSocialService {

    private final ObraSocialRepository obraSocialRepository;

    public ObraSocialService(ObraSocialRepository obraSocialRepository) {
        this.obraSocialRepository = obraSocialRepository;
    }

    @Transactional(readOnly = true)
    public List<ObraSocial> listar() {
        return obraSocialRepository.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public ObraSocial obtener(Long id) {
        return obraSocialRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la obra social con ID " + id));
    }

    @Transactional
    public ObraSocial guardar(ObraSocial os) {
        if (os.getNombre() == null || os.getNombre().isBlank()) {
            throw new DatosInvalidosException("El nombre de la Obra Social es obligatorio.");
        }
        if (os.getCuit() == null || os.getCuit().isBlank()) {
            throw new DatosInvalidosException("El CUIT de la Obra Social es obligatorio.");
        }
        String cuitLimpio = os.getCuit().trim();
        os.setCuit(cuitLimpio);

        if (os.getId() == null) {
            if (obraSocialRepository.existsByCuit(cuitLimpio)) {
                throw new RecursoDuplicadoException("Ya existe una Obra Social registrada con el CUIT " + cuitLimpio);
            }
        } else {
            obraSocialRepository.findByCuit(cuitLimpio).ifPresent(existente -> {
                if (!existente.getId().equals(os.getId())) {
                    throw new RecursoDuplicadoException("Ya existe otra Obra Social registrada con el CUIT " + cuitLimpio);
                }
            });
        }

        if (os.getRazonSocial() == null || os.getRazonSocial().isBlank()) {
            os.setRazonSocial(os.getNombre());
        }

        return obraSocialRepository.save(os);
    }

    @Transactional
    public void eliminar(Long id) {
        ObraSocial os = obtener(id);
        obraSocialRepository.delete(os);
    }
}
