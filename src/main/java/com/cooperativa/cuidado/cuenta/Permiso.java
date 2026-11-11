package com.cooperativa.cuidado.cuenta;

public enum Permiso {
    VER_CALENDARIO_GLOBAL(
            "Calendario y turnos",
            "Ver panel completo y calendario general de todos los socios"),
    ASIGNAR_TURNOS(
            "Calendario y turnos",
            "Asignar nuevos turnos y coberturas"),
    MODIFICAR_TURNOS(
            "Calendario y turnos",
            "Modificar turnos existentes en el calendario (horarios, viáticos y sanatorio)"),
    ELIMINAR_TURNOS(
            "Calendario y turnos",
            "Eliminar turnos del calendario"),
    GESTIONAR_MARCACIONES_TURNOS(
            "Calendario y turnos",
            "Registrar y editar fichajes de entrada, salida y aclaraciones de turnos de cualquier socio"),
    AGREGAR_SOCIOS(
            "Socios",
            "Acceder al buscador indexado de socios, dar de alta y editar socios"),
    ELIMINAR_SOCIOS(
            "Socios",
            "Eliminar socios"),
    REALIZAR_LIQUIDACIONES(
            "Socios",
            "Emitir recibos de retribución y descargar liquidaciones mensuales de socios"),
    GESTIONAR_RETENCIONES(
            "Socios",
            "Registrar, consultar y eliminar retenciones mensuales a socios"),
    AGREGAR_CLIENTES(
            "Clientes",
            "Acceder al buscador indexado de clientes, dar de alta y editar clientes"),
    ELIMINAR_CLIENTES(
            "Clientes",
            "Eliminar clientes"),
    REALIZAR_LIQUIDACIONES_FAMILIA(
            "Clientes",
            "Generar y exportar liquidaciones mensuales para familias / clientes"),
    REALIZAR_PRESUPUESTOS(
            "Clientes",
            "Calcular y descargar presupuestos en PDF y Excel"),
    AGREGAR_LEGAJOS(
            "Legajos digitales",
            "Ver y adjuntar archivos al legajo digital de socios y clientes"),
    ELIMINAR_LEGAJOS(
            "Legajos digitales",
            "Eliminar archivos del legajo digital"),
    GESTIONAR_SERVICIOS(
            "Servicios y feriados",
            "Gestionar catálogo de servicios, tipos de servicio y cuadro tarifario diferenciado"),
    GESTIONAR_FERIADOS(
            "Servicios y feriados",
            "Gestionar calendario de feriados nacionales"),
    EXPORTAR_HISTORICOS(
            "Histórico, estadísticas y cuentas",
            "Consultar buscador de históricos (general, por socio, por cliente, cruces y período/total) y exportar en PDF y Excel"),
    VER_ESTADISTICAS(
            "Histórico, estadísticas y cuentas",
            "Acceder al panel de estadísticas operativas"),
    GESTIONAR_CUENTAS(
            "Histórico, estadísticas y cuentas",
            "Gestionar cuentas de usuarios y configurar roles y permisos");

    private final String categoria;
    private final String etiqueta;

    Permiso(String categoria, String etiqueta) {
        this.categoria = categoria;
        this.etiqueta = etiqueta;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}

