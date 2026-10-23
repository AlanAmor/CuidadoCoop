# Reglas de Trabajo del Proyecto (CuidadoApp)

## 1. Alcance Estricto y Preservación de la Lógica de Negocio
- **Modificar únicamente lo solicitado:** La lógica de negocio de la aplicación ya está implementada, verificada y testeada por el usuario.
- **Prohibido hacer cambios no solicitados:** No alteres, refactorices ni reescribas reglas de negocio, validaciones, servicios o vistas fuera del alcance exacto del pedido del usuario.

## 2. Optimización de Tokens y Tiempos de Demora (Agente Único vs. Equipo)
- **Evaluación previa de complejidad:** Antes de delegar o lanzar subagentes, evaluá si la tarea la puede resolver un solo agente de manera directa.
- **Agente único por defecto:** Resolvé las tareas directamente con un solo agente para minimizar el consumo de tokens y el tiempo de espera.
- **Uso excepcional de múltiples agentes:** Solo recurrí a un equipo de subagentes cuando la tarea sea de gran escala (múltiples módulos independientes en paralelo) y el beneficio supere claramente el costo en tiempo y tokens.

