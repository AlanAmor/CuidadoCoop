USE cooperativa_db;

START TRANSACTION;

INSERT INTO socios (
    nombre, apellido, dni, direccion, telefono, correo, cbu, estado, especialidad
) VALUES
    ('Ana', 'Gómez', '20900001', 'Av. San Martín 101', '1140000001', 'ana.gomez.demo@example.com', '2850590900000000000001', 'ACTIVO_EN_SERVICIO', 'CUIDADOR'),
    ('Carlos', 'Pérez', '20900002', 'Belgrano 202', '1140000002', 'carlos.perez.demo@example.com', '2850590900000000000002', 'ACTIVO_EN_SERVICIO', 'CUIDADOR'),
    ('Marta', 'López', '20900003', 'Sarmiento 303', '1140000003', 'marta.lopez.demo@example.com', '2850590900000000000003', 'ACTIVO_EN_SERVICIO', 'ENFERMERO'),
    ('Diego', 'Fernández', '20900004', 'Rivadavia 404', '1140000004', 'diego.fernandez.demo@example.com', '2850590900000000000004', 'ACTIVO_EN_SERVICIO', 'CUIDADOR'),
    ('Laura', 'Rodríguez', '20900005', 'Moreno 505', '1140000005', 'laura.rodriguez.demo@example.com', '2850590900000000000005', 'ACTIVO_EN_SERVICIO', 'CUIDADOR'),
    ('Jorge', 'Sosa', '20900006', 'Mitre 606', '1140000006', 'jorge.sosa.demo@example.com', '2850590900000000000006', 'DISPONIBLE_REEMPLAZO', 'ENFERMERO'),
    ('Silvia', 'Ramírez', '20900007', 'Belgrano 707', '1140000007', 'silvia.ramirez.demo@example.com', '2850590900000000000007', 'DISPONIBLE_REEMPLAZO', 'CUIDADOR'),
    ('Pablo', 'Torres', '20900008', 'San Juan 808', '1140000008', 'pablo.torres.demo@example.com', '2850590900000000000008', 'DISPONIBLE_REEMPLAZO', 'CUIDADOR'),
    ('Elena', 'Acosta', '20900009', 'Italia 909', '1140000009', 'elena.acosta.demo@example.com', '2850590900000000000009', 'DISPONIBLE_REEMPLAZO', 'ENFERMERO'),
    ('Ricardo', 'Díaz', '20900010', 'España 1010', '1140000010', 'ricardo.diaz.demo@example.com', '2850590900000000000010', 'DISPONIBLE_REEMPLAZO', 'CUIDADOR')
AS nuevo_socio
ON DUPLICATE KEY UPDATE
    nombre = nuevo_socio.nombre,
    apellido = nuevo_socio.apellido,
    direccion = nuevo_socio.direccion,
    telefono = nuevo_socio.telefono,
    correo = nuevo_socio.correo,
    estado = nuevo_socio.estado,
    especialidad = nuevo_socio.especialidad;

INSERT INTO clientes (
    nombre_familiar, telefono, correo, nombre_paciente, domicilio, observacion_medica
)
SELECT demo.nombre_familiar, demo.telefono, demo.correo, demo.nombre_paciente,
       demo.domicilio, demo.observacion_medica
FROM (
    SELECT 'Familia Demo 01' nombre_familiar, '1141000001' telefono,
           'familia01.demo@example.com' correo, 'Demo Paciente 01' nombre_paciente,
           'Domicilio Demo 01, Buenos Aires' domicilio, 'Dato ficticio para pruebas.' observacion_medica
    UNION ALL SELECT 'Familia Demo 02', '1141000002', 'familia02.demo@example.com', 'Demo Paciente 02',
           'Domicilio Demo 02, Buenos Aires', 'Dato ficticio para pruebas.'
    UNION ALL SELECT 'Familia Demo 03', '1141000003', 'familia03.demo@example.com', 'Demo Paciente 03',
           'Domicilio Demo 03, Buenos Aires', 'Dato ficticio para pruebas.'
    UNION ALL SELECT 'Familia Demo 04', '1141000004', 'familia04.demo@example.com', 'Demo Paciente 04',
           'Domicilio Demo 04, Buenos Aires', 'Dato ficticio para pruebas.'
    UNION ALL SELECT 'Familia Demo 05', '1141000005', 'familia05.demo@example.com', 'Demo Paciente 05',
           'Domicilio Demo 05, Buenos Aires', 'Dato ficticio para pruebas.'
    UNION ALL SELECT 'Familia Demo 06', '1141000006', 'familia06.demo@example.com', 'Demo Paciente 06',
           'Domicilio Demo 06, Buenos Aires', 'Dato ficticio para pruebas.'
    UNION ALL SELECT 'Familia Demo 07', '1141000007', 'familia07.demo@example.com', 'Demo Paciente 07',
           'Domicilio Demo 07, Buenos Aires', 'Dato ficticio para pruebas.'
    UNION ALL SELECT 'Familia Demo 08', '1141000008', 'familia08.demo@example.com', 'Demo Paciente 08',
           'Domicilio Demo 08, Buenos Aires', 'Dato ficticio para pruebas.'
    UNION ALL SELECT 'Familia Demo 09', '1141000009', 'familia09.demo@example.com', 'Demo Paciente 09',
           'Domicilio Demo 09, Buenos Aires', 'Dato ficticio para pruebas.'
    UNION ALL SELECT 'Familia Demo 10', '1141000010', 'familia10.demo@example.com', 'Demo Paciente 10',
           'Domicilio Demo 10, Buenos Aires', 'Dato ficticio para pruebas.'
) demo
LEFT JOIN clientes existente
    ON existente.nombre_familiar = demo.nombre_familiar
   AND existente.nombre_paciente = demo.nombre_paciente
WHERE existente.id IS NULL;

INSERT IGNORE INTO tipos_servicio (nombre) VALUES
    ('Por horas'), ('Servicio 24/7'), ('Enfermería express');

INSERT INTO servicios_domicilio (valor_hora, valor_hora_socio, tipo_servicio_id)
SELECT demo.valor_hora, demo.valor_hora_socio, tipo.id
FROM (
    SELECT 3000.00 valor_hora, 2100.00 valor_hora_socio, 'Por horas' tipo
    UNION ALL SELECT 2600.00, 1800.00, 'Servicio 24/7'
    UNION ALL SELECT 4500.00, 3200.00, 'Enfermería express'
) demo
JOIN tipos_servicio tipo ON tipo.nombre = demo.tipo
WHERE NOT EXISTS (
    SELECT 1 FROM servicios_domicilio sd WHERE sd.tipo_servicio_id = tipo.id
);

INSERT INTO asistencias (
    socio_id, cliente_id, servicio_domicilio_id, fecha, hora_inicio, hora_fin, horas_totales,
    valor_hora_aplicado, valor_hora_socio_aplicado
)
SELECT socio.id,
       cliente.id,
       servicio.id,
       DATE_ADD(CURRENT_DATE, INTERVAL demo.dia DAY),
       '08:00:00',
       '12:00:00',
       4.00,
       servicio.valor_hora,
       servicio.valor_hora_socio
FROM (
    SELECT 0 dia UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) demo
JOIN socios socio
    ON socio.dni = CONCAT('209000', LPAD(MOD(demo.dia, 5) + 1, 2, '0'))
JOIN clientes cliente
    ON cliente.nombre_paciente = CONCAT('Demo Paciente ', LPAD(demo.dia + 1, 2, '0'))
JOIN servicios_domicilio servicio
    ON servicio.id = (SELECT MIN(s.id) FROM servicios_domicilio s
                      JOIN tipos_servicio t ON t.id = s.tipo_servicio_id WHERE t.nombre = 'Por horas')
WHERE NOT EXISTS (
    SELECT 1
    FROM asistencias asistencia
    WHERE asistencia.socio_id = socio.id
      AND asistencia.cliente_id = cliente.id
      AND asistencia.fecha = DATE_ADD(CURRENT_DATE, INTERVAL demo.dia DAY)
      AND asistencia.hora_inicio = '08:00:00'
      AND asistencia.hora_fin = '12:00:00'
);
COMMIT;

SELECT
    (SELECT COUNT(*) FROM socios WHERE dni BETWEEN '20900001' AND '20900010') AS socios_demo,
    (SELECT COUNT(*) FROM clientes WHERE nombre_paciente LIKE 'Demo Paciente %') AS clientes_demo,
    (SELECT COUNT(*) FROM servicios_domicilio) AS servicios,
    (SELECT COUNT(*) FROM asistencias a
        JOIN socios s ON s.id = a.socio_id
        JOIN clientes c ON c.id = a.cliente_id
        WHERE s.dni BETWEEN '20900001' AND '20900010'
          AND c.nombre_paciente LIKE 'Demo Paciente %') AS asistencias_demo;