package com.cooperativa.cuidado.cuenta;

public enum Permiso {
    VER_CALENDARIO_GLOBAL("Ver panel completo y calendario general de todos los socios"),
    ASIGNAR_TURNOS("Asignar nuevos turnos y coberturas"),
    MODIFICAR_TURNOS("Modificar turnos existentes en el calendario"),
    ELIMINAR_TURNOS("Eliminar turnos del calendario"),
    AGREGAR_SOCIOS("Ver listado, dar de alta y editar socios"),
    ELIMINAR_SOCIOS("Eliminar socios"),
    AGREGAR_CLIENTES("Ver listado, dar de alta y editar clientes"),
    ELIMINAR_CLIENTES("Eliminar clientes"),
    AGREGAR_LEGAJOS("Ver y agregar archivos al legajo de socios y clientes"),
    ELIMINAR_LEGAJOS("Eliminar archivos del legajo"),
    GESTIONAR_SERVICIOS("Gestionar catálogo de servicios y tipos de servicio"),
    REALIZAR_PRESUPUESTOS("Calcular y descargar presupuestos"),
    REALIZAR_LIQUIDACIONES("Consultar y descargar liquidaciones / retornos mensuales"),
    EXPORTAR_HISTORICOS("Exportar históricos de asistencias en PDF y Excel"),
    VER_ESTADISTICAS("Acceder al panel de estadísticas"),
    GESTIONAR_CUENTAS("Gestionar cuentas de usuarios y roles");

    private final String etiqueta;

    Permiso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}

