package com.cooperativa.cuidado.legajo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArchivoLegajoRepository extends JpaRepository<ArchivoLegajo, Long> {

    @Query("""
            select new com.cooperativa.cuidado.legajo.ArchivoLegajoResumen(
                a.id, a.nombre, a.tipoContenido, a.tamano, a.fechaSubida)
            from ArchivoLegajo a
            where a.tipo = :tipo and a.propietarioId = :propietarioId
            order by a.fechaSubida desc, a.id desc
            """)
    List<ArchivoLegajoResumen> listarResumenes(
            @Param("tipo") TipoLegajo tipo, @Param("propietarioId") Long propietarioId);

    List<ArchivoLegajo> findAllByTipoAndPropietarioId(TipoLegajo tipo, Long propietarioId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ArchivoLegajo a where a.tipo = :tipo and a.propietarioId = :propietarioId")
    void eliminarDelPropietario(@Param("tipo") TipoLegajo tipo, @Param("propietarioId") Long propietarioId);
}
