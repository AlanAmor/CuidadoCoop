USE cooperativa_db;

START TRANSACTION;

DELETE asistencia
FROM asistencias asistencia
JOIN socios socio ON socio.id = asistencia.socio_id
JOIN servicios_domicilio servicio ON servicio.id = asistencia.servicio_domicilio_id
JOIN clientes cliente ON cliente.id = servicio.cliente_id
WHERE socio.dni BETWEEN '20900001' AND '20900010'
  AND cliente.nombre_paciente LIKE 'Demo Paciente %'
  AND asistencia.hora_inicio = '08:00:00'
  AND asistencia.hora_fin = '12:00:00';

DELETE servicio
FROM servicios_domicilio servicio
JOIN clientes cliente ON cliente.id = servicio.cliente_id
WHERE cliente.nombre_paciente LIKE 'Demo Paciente %';

DELETE FROM clientes
WHERE nombre_familiar LIKE 'Familia Demo %'
  AND nombre_paciente LIKE 'Demo Paciente %';

DELETE FROM socio_dias_disponibles
WHERE socio_id IN (SELECT id FROM socios WHERE dni BETWEEN '20900001' AND '20900010');

DELETE FROM socios
WHERE dni BETWEEN '20900001' AND '20900010';

COMMIT;
