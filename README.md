# Cuidado App

Sistema web para la gestión de una cooperativa de cuidado, construido con Java 17, Spring Boot, Spring Data JPA, Bean Validation, Thymeleaf y MySQL.

## Requisitos

- JDK 17 o superior
- Maven 3.9 o superior
- MySQL 8

## Base de datos

Crear la base antes de iniciar la aplicación ejecutando [create_database.sql](./database/create_database.sql), o con este SQL:

```sql
CREATE DATABASE cooperativa_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

La conexión se configura con `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. Para evitar guardar secretos en el proyecto, configurá las credenciales mediante variables de entorno antes de ejecutar la aplicación.

`spring.jpa.hibernate.ddl-auto=update` está habilitado para facilitar el desarrollo inicial. Para producción se debe reemplazar por migraciones versionadas (por ejemplo, Flyway) y no depender de actualizaciones automáticas del esquema.

## Fase 2: persistencia y lógica de negocio

- `SocioService` registra, modifica y cambia el estado de los socios; valida la unicidad de DNI/CBU y consulta horas trabajadas.
- `AsistenciaService` registra y actualiza turnos, obtiene las referencias persistidas, conserva el valor hora vigente como dato histórico y rechaza solapamientos del mismo socio. Los extremos de dos turnos pueden coincidir sin considerarse solapamiento.
- `LiquidacionService` calcula horas y retornos mensuales por socio, con un desglose por servicio/domicilio. El importe usa el valor hora guardado en cada asistencia, no el valor actual del servicio.
- Las operaciones de escritura usan transacciones. Las excepciones de dominio están en `exception`.
- Los repositorios permanecen junto a sus entidades de dominio (`socio`, `asistencia`, `cliente` y `servicio`) para conservar la organización modular existente.

Las pruebas incluyen una base H2 aislada para verificar el mapeo JPA y las consultas de repositorio; no requiere una instancia MySQL para ejecutarse.

## Fase 3: interfaz operativa

- El dashboard está disponible en `/` y muestra socios en servicio, disponibles y horas cargadas del mes actual.
- `/asignaciones/nueva` permite registrar un turno para un socio disponible y un servicio existente. La operación crea la asistencia y cambia el estado del socio dentro de una única transacción.
- `/liquidaciones` permite consultar el retorno mensual por socio y ver el detalle por servicio/domicilio.
- Las vistas usan Thymeleaf y CSS propio adaptable a móviles, sin dependencias de JavaScript ni de CDN.

## Fase 4: documentos y estadísticas

- Los retornos se pueden descargar en PDF desde el resultado mensual; el documento muestra horas, importes por domicilio, DNI/CBU del socio y total liquidado.
- El PDF de retorno es un modelo orientativo. No se presenta como plantilla oficial ni homologada por INAES; la cooperativa debe validarlo con su asesoría antes de utilizarlo como documento formal.
- `/presupuestos` permite generar un PDF con cabecera institucional genérica, desglose de horas/valor hora, validez de 15 días y una sección de troquel para el cliente. El importe se calcula con el valor hora actual y el documento indica que no es comprobante fiscal.
- `/asistencias/historico` exporta el histórico de un rango de fechas en PDF o Excel (XLSX), con valor hora histórico, horas y datos de auditoría. El archivo contiene datos personales y debe compartirse únicamente con personas autorizadas.
- `/estadisticas` es un panel completo con filtros de año, mes y semana (lunes a domingo): horas de año/mes/semana con variación frente al período anterior, gráficos de horas por mes (año vs. año anterior) y por semana, top 10 de socios del mes y de la semana, tabla comparativa de socios con mapa de calor mensual, servicios más demandados, clientes con más horas, horas por día de la semana y por franja horaria, resultado económico mensual (facturado, retornos a socios y excedente, sin IVA) y socios sin turnos en el mes. Los gráficos son CSS puro, sin scripts.

## Calendario y datos de demostración

- La pantalla principal muestra un calendario navegable con vistas de día, semana y mes, selector de fecha y grilla horaria de 24 horas. Los eventos incluyen socio asignado, paciente, domicilio y rango horario.
- El panel de disponibilidad muestra socios actualmente marcados como disponibles para reemplazos y servicios que no tienen asistencias registradas en el período consultado. “Sin turno” describe la falta de una asistencia registrada en ese rango; no significa que el contrato o el servicio esté cancelado.
- La API interna del calendario (`/api/calendario`) consulta turnos en un rango acotado; cada turno que cruza medianoche se muestra en su fecha de inicio y mantiene su hora final real.
- Para cargar diez socios, diez clientes, diez servicios y diez asistencias ficticias en la base activa, ejecutá [seed_demo_data.sql](./database/seed_demo_data.sql) con MySQL Shell. La carga es repetible y no elimina datos.
- Para quitar únicamente ese conjunto de demostración, ejecutá [remove_demo_data.sql](./database/remove_demo_data.sql). Revisá los identificadores de demostración en el script antes de utilizarlo; no es un mecanismo de borrado general de datos.

## Errores y ayuda al usuario

- Los errores de validación, datos duplicados, referencias inexistentes, reglas de negocio, parámetros incorrectos, rutas inexistentes y problemas de persistencia se traducen a una página de error en español con una explicación y un siguiente paso.
- Los errores técnicos inesperados no muestran trazas, consultas SQL ni detalles internos al usuario. Se registran en el log de la aplicación y la pantalla recomienda revisar el resultado antes de repetir una operación.
- La página de error ofrece volver a la pantalla anterior o al inicio. Para situaciones en las que el navegador no puede mostrar la vista, se conservan los códigos HTTP adecuados (por ejemplo 400, 404, 409, 503 y 500).

## Administración, precios e IVA

- `/socios`, `/clientes` y `/servicios`: alta, edición y baja. Eliminar pide confirmación y es en cascada (socio → sus turnos; cliente → sus turnos; servicio → sus turnos).
- `/servicios` es un catálogo: solo tipo de servicio, cobro al cliente por hora y pago al socio por hora, sin datos de cliente. El cliente se elige al asignar o editar un turno y al generar un presupuesto (que requiere cliente y servicio).
- `/tipos-servicio` permite crear, renombrar y eliminar los tipos de servicio (se crean "Por horas", "Servicio 24/7" y "Enfermería express" en una base nueva). Un tipo en uso no se puede eliminar. Si tenés una base creada con la versión anterior (tipos fijos), ejecutá una vez [migrate_tipos_servicio.sql](./database/migrate_tipos_servicio.sql) con la aplicación detenida.
- El precio se define por servicio en dos valores independientes: lo que se **cobra al cliente** por hora y lo que se **paga al socio** por hora. Los presupuestos usan el primero y las liquidaciones/retornos usan el segundo. Cambiarlos no altera los turnos ya cargados, que conservan los valores con los que se registraron.
- Presupuestos: se puede elegir IVA (sin IVA, 10,5 %, 21 %, 27 %) y pago en efectivo (10 % de descuento). El descuento se aplica antes del IVA. "Ver cálculo" muestra el desglose y el PDF lo incluye.
- Calendario: las horas quedan fijas al hacer scroll y cada turno ocupa tantas filas como horas dure. Al hacer clic en un turno se ve el detalle y se puede eliminar.

## Asignación por horario y edición de turnos

- **Asignar turno** (`/asignaciones/nueva`) tiene dos pasos: primero se indica día y horario; luego se elige entre los socios disponibles.
- Un socio está disponible si no es `INACTIVO` y no tiene otro turno que se superponga con ese día y horario.
- En el calendario, al abrir un turno, **Editar turno** permite cambiar fecha, horario, socio y cliente. Se guarda en la base de datos desde el mismo panel y se rechazan los solapamientos.
- **Cuidados fijos (larga duración)**: en el paso 1, marcá "Repetir este horario", elegí duración (días, semanas o meses; máximo un año) y los días de la semana. Se crea un turno por cada fecha, todo o nada: si el socio tiene un turno en alguna fecha, no figura como disponible y, si hubiera un choque al confirmar, no se crea ningún turno. Cada turno se puede editar o eliminar luego desde el calendario.
- **Históricos descargables**: en Socios y Clientes, los botones "Histórico PDF" y "Excel" bajan el historial completo de asistencias de un socio o de servicios de un cliente (también `/socios/{id}/historico/{pdf|excel}` y `/clientes/{id}/historico/{pdf|excel}`, con `desde` y `hasta` opcionales).

## Legajo de socios y clientes

- En **Editar socio** y **Editar cliente** hay un panel "Legajo": se arrastran (o se eligen) facturas, presupuestos u otros documentos y se guardan al instante. Se admiten PDF, JPG y PNG de hasta 10 MB; el tipo se verifica por el contenido real del archivo.
- En los listados, el botón **Legajo** (junto a Editar y Eliminar) abre una ventana flotante y movible para leer los archivos; también permite abrirlos en otra pestaña o descargarlos.
- Cada socio y cliente tiene su carpeta en disco: `legajos/socios/<id>/` y `legajos/clientes/<id>/` (puede tener varios archivos). La base solo guarda los metadatos en `archivos_legajo`. La carpeta se borra junto con la persona.
- La ubicación se cambia con la variable de entorno `LEGAJOS_DIR`. Conviene respaldar esa carpeta junto con la base de datos.

## Ejecutar y probar

```powershell
$env:DB_USERNAME = "usuario"
$env:DB_PASSWORD = "contraseña"
mvn spring-boot:run
```

Al iniciar, Hibernate crea o actualiza las tablas de la base indicada. La aplicación queda disponible en `http://localhost:8080`.

Para ejecutar las pruebas unitarias:

```powershell
mvn test
```
