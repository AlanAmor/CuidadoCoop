package com.cooperativa.cuidado.servicio.service;

import com.cooperativa.cuidado.servicio.TipoServicio;
import com.cooperativa.cuidado.servicio.TipoServicioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Crea los tipos habituales en una base nueva; después la cooperativa los administra libremente. */
@Component
public class TiposServicioIniciales implements ApplicationRunner {

    private final TipoServicioRepository tipoRepository;

    public TiposServicioIniciales(TipoServicioRepository tipoRepository) {
        this.tipoRepository = tipoRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (tipoRepository.count() == 0) {
            tipoRepository.saveAll(List.of(
                    new TipoServicio("Por horas"),
                    new TipoServicio("Servicio 24/7"),
                    new TipoServicio("Enfermería express")));
        }
    }
}
