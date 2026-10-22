-- Migración: el tipo de servicio pasa de una lista fija (columna tipo_servicio) a una tabla editable.
-- Ejecutar UNA vez, con la aplicación detenida, sobre una base creada con la versión anterior.
USE cooperativa_db;

CREATE TABLE IF NOT EXISTS tipos_servicio (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL,
    CONSTRAINT uk_tipo_servicio_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

INSERT IGNORE INTO tipos_servicio (nombre) VALUES
    ('Por horas'), ('Servicio 24/7'), ('Enfermería express');

ALTER TABLE servicios_domicilio ADD COLUMN tipo_servicio_id BIGINT NULL;

UPDATE servicios_domicilio s
JOIN tipos_servicio t ON t.nombre = CASE s.tipo_servicio
    WHEN 'HORAS' THEN 'Por horas'
    WHEN 'SERVICIO_24_7' THEN 'Servicio 24/7'
    WHEN 'ENFERMERIA_EXPRESS' THEN 'Enfermería express'
END
SET s.tipo_servicio_id = t.id;

ALTER TABLE servicios_domicilio MODIFY tipo_servicio_id BIGINT NOT NULL;
ALTER TABLE servicios_domicilio
    ADD CONSTRAINT fk_servicio_tipo FOREIGN KEY (tipo_servicio_id) REFERENCES tipos_servicio (id);
ALTER TABLE servicios_domicilio DROP COLUMN tipo_servicio;
