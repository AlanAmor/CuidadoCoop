USE cooperativa_db;

-- Días de la semana disponibles por socio. Los socios existentes quedan disponibles todos los días.
CREATE TABLE IF NOT EXISTS socio_dias_disponibles (
    socio_id BIGINT NOT NULL,
    dia VARCHAR(12) NOT NULL,
    PRIMARY KEY (socio_id, dia),
    CONSTRAINT fk_socio_dias_socio FOREIGN KEY (socio_id) REFERENCES socios (id)
);

INSERT IGNORE INTO socio_dias_disponibles (socio_id, dia)
SELECT socio.id, dias.dia
FROM socios socio
CROSS JOIN (
    SELECT 'MONDAY' dia UNION ALL SELECT 'TUESDAY' UNION ALL SELECT 'WEDNESDAY'
    UNION ALL SELECT 'THURSDAY' UNION ALL SELECT 'FRIDAY' UNION ALL SELECT 'SATURDAY'
    UNION ALL SELECT 'SUNDAY'
) dias
WHERE NOT EXISTS (SELECT 1 FROM socio_dias_disponibles existente WHERE existente.socio_id = socio.id);
