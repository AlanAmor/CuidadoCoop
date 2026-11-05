# Reglas de Trabajo del Proyecto (CuidadoApp)

## 1. Alcance Estricto y Preservación de la Lógica de Negocio
- **Modificar únicamente lo solicitado:** La lógica de negocio de la aplicación ya está implementada, verificada y testeada por el usuario.
- **Prohibido tocar la lógica de negocio:** No alteres, refactorices ni reescribas reglas de negocio, validaciones, servicios o vistas fuera del alcance exacto del pedido, salvo que sea estrictamente necesario para resolver lo pedido o el usuario lo solicite explícitamente.

## 2. Optimización de Tokens y Modo de Trabajo (Agente Único vs. Multiagente)
- **Libertad de elección con foco en ahorro de tokens:** Tenés libertad para decidir si resolver la tarea por tu cuenta o usando múltiples agentes, priorizando siempre gastar la menor cantidad de tokens posible.
- **Agente único por defecto:** Resolvé las tareas directamente como agente único para minimizar el consumo de tokens y el tiempo de espera.
- **Uso excepcional de multiagente:** Solo recurrí a subagentes cuando la tarea sea de gran escala y el ahorro de contexto/tokens justifique claramente su uso.
