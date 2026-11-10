package com.cooperativa.cuidado.cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("""
            select c from Cliente c
            where (:termino is null or :termino = ''
                or lower(c.nombrePaciente) like lower(concat('%', :termino, '%'))
                or lower(coalesce(c.dniPaciente, '')) like lower(concat('%', :termino, '%'))
                or lower(c.nombreFamiliar) like lower(concat('%', :termino, '%'))
                or lower(c.telefono) like lower(concat('%', :termino, '%'))
                or lower(coalesce(c.correo, '')) like lower(concat('%', :termino, '%'))
                or lower(c.domicilio) like lower(concat('%', :termino, '%'))
                or lower(coalesce(c.ciudad, '')) like lower(concat('%', :termino, '%'))
                or lower(coalesce(c.localidad, '')) like lower(concat('%', :termino, '%')))
              and (:inicial is null or :inicial = '' or upper(c.nombrePaciente) like concat(upper(:inicial), '%'))
            order by c.nombrePaciente asc
            """)
    List<Cliente> buscarIndexado(
            @Param("termino") String termino,
            @Param("inicial") String inicial);
}
