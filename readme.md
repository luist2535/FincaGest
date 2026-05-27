# FincaGest — Sistema de gestión de fincas e inmuebles

Aplicación de escritorio en Java (Swing) para gestionar fincas, inmuebles, inquilinos, contratos de alquiler, recibos y movimientos bancarios. Usa MySQL como base de datos y FlatLaf para una interfaz moderna.

## Contenido del repositorio
- Código fuente: `src/main/java/com/fincas`
- Esquema de base de datos: [database/schema.sql](database/schema.sql)
- Archivo de construcción: `pom.xml` (Maven, Java 17)

## Requisitos
- Java 17 (JDK 17)
- Maven 3.x
- MySQL (por ejemplo XAMPP con MySQL activo en `localhost:3306`)
- Dependencias: `mysql-connector-java`, `flatlaf` (definidas en `pom.xml`)

## Preparar la base de datos
1. Inicia MySQL (por ejemplo con XAMPP: abre el Panel de Control y arranca MySQL).
2. Desde phpMyAdmin o una consola SQL crea la base de datos `fincas_db`.
3. Importa el script `database/schema.sql` para crear las tablas y datos de ejemplo.
4. Por defecto, la conexión a la base de datos está en `src/main/java/com/fincas/db/DatabaseConnection.java` con:

    - URL: `jdbc:mysql://localhost:3306/fincas_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`
    - Usuario: `root`
    - Contraseña: `""` (vacía, configuración por defecto de XAMPP)

    Si tu instalación usa otra contraseña o usuario, actualiza `DatabaseConnection.java` o adapta la configuración para leer variables de entorno.

## Construir y ejecutar
Desde la raíz del proyecto:

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.fincas.MainApp"
```

O empaqueta y ejecuta con `mvn package` y tu comando Java preferido (ten en cuenta que las dependencias no están empaquetadas por defecto en un único "uber-jar").

## Abrir en un IDE
- Importa el proyecto como Maven (IntelliJ, Eclipse, NetBeans, VS Code con soporte Java).
- Configura Java 17 como SDK del proyecto.
- Ejecuta la clase principal `com.fincas.MainApp`.

## Visión general de la aplicación
- Interfaz con menú lateral (sidebar) y paneles por sección: Dashboard, Inmuebles, Inquilinos, Alquileres, Recibos, Contabilidad e Informes.
- Cada panel provee listados y acciones CRUD para su entidad.
- La aplicación valida la conexión a la base de datos al inicio y muestra un diálogo con instrucciones si falla la conexión.

## Estructura y componentes importantes

- `com.fincas.MainApp` — Entrada de la aplicación; aplica el tema FlatLaf y verifica la conexión antes de mostrar `MainFrame`.
- `com.fincas.db.DatabaseConnection` — Provee conexiones JDBC a MySQL. Editar aquí las credenciales si es necesario.

- `com.fincas.model` — Modelos (POJOs): `Inmueble`, `Inquilino`, `Recibo`, `Banco`, `MovimientoBancario`, `Alquiler`.
   - `Inmueble` incluye campos: `id`, `tipo`, `direccion`, `numero`, `codigoPostal`, `planta`, `letra`, `parentEdificioId`, `codigoRecibo`.

- `com.fincas.dao` — DAOs que realizan operaciones SQL:
   - `InmuebleDAO`: `getAll()`, `getById(int)`, `getPisosYLocalesDeEdificio(int)`, `getDisponiblesParaAlquiler()`, `insert()`, `update()`, `delete()`.
   - Otros DAOs siguen patrones similares (`AlquilerDAO`, `InquilinoDAO`, `ReciboDAO`, `BancoDAO`, `MovimientoBancarioDAO`).

- `com.fincas.gui` — Interfaz gráfica:
   - `MainFrame` — Ventana principal con `CardLayout` para cambiar entre paneles y método `toggleTheme()` para modo claro/oscuro.
   - Paneles: `DashboardPanel`, `InmueblesPanel`, `InquilinosPanel`, `AlquileresPanel`, `RecibosPanel`, `ContabilidadPanel`, `InformesPanel`. Cada panel dispone de un método `refreshData()` llamado por `MainFrame` al cambiar de sección.

## Cómo agregar un Inmueble (ejemplo)
Desde código usando el DAO:

```java
Inmueble i = new Inmueble();
i.setTipo("PISO");
i.setDireccion("Calle Ejemplo");
i.setNumero("5");
InmuebleDAO dao = new InmuebleDAO();
dao.insert(i);
```

En la UI: abre la sección `Inmuebles` y usa el formulario correspondiente para crear/editar/eliminar registros.

## Solución de problemas rápida
- Si aparece el diálogo de error al arrancar, revisa que MySQL esté en `localhost:3306`, que la base `fincas_db` exista y que las credenciales en `DatabaseConnection` sean correctas.
- Ejecuta `mvn clean install` si las dependencias no se descargaron correctamente.

## Descripción por pestaña (qué hace cada sección)

Las siguientes descripciones explican la funcionalidad principal de cada panel accesible desde la barra lateral (sidebar) y cómo añadir/editar registros desde la UI o por código.

- **Dashboard**
   - Qué hace: Muestra un resumen general del sistema: totales de inmuebles, contratos activos, saldo bancario y atajos a acciones frecuentes.
   - Cómo usar: Consulta los indicadores principales. Usa los botones/atajos del panel para navegar a las secciones detalladas.

- **Inmuebles**
   - Qué hace: Lista todos los inmuebles registrados (edificios, pisos, locales) y muestra información básica (dirección, tipo, planta, letra, código de recibo).
   - Acciones comunes: buscar, filtrar, ordenar; añadir nuevo inmueble; editar o eliminar inmuebles existentes.
   - Cómo añadir: en la UI, pulsa el botón `Añadir` (o `Nuevo`) dentro del panel `Inmuebles`, completa el formulario y guarda. Los cambios se persistirán en la base de datos mediante `InmuebleDAO.insert()`.
   - Cómo agregar por código: usa `Inmueble` + `InmuebleDAO.insert(...)` (ver ejemplo en la sección "Cómo agregar un Inmueble").

- **Inquilinos**
   - Qué hace: Muestra la lista de inquilinos con sus datos de contacto y contratos asociados.
   - Acciones comunes: crear/editar/eliminar inquilinos, asignar un contrato de alquiler a un inmueble.
   - Cómo añadir: usa el botón `Nuevo` dentro del panel `Inquilinos` y completa el formulario.

- **Alquileres**
   - Qué hace: Gestiona los contratos de alquiler: contratos activos, fechas inicio/fin, importe, inquilino e inmueble relacionados.
   - Acciones comunes: crear nuevo contrato, renovar, finalizar contrato, marcar como activo/inactivo.
   - Cómo añadir: desde el panel `Alquileres` pulsa `Nuevo contrato`, selecciona inquilino e inmueble (se cargan desde `InquilinoDAO` e `InmuebleDAO`) y guarda.

- **Recibos**
   - Qué hace: Genera y lista recibos asociados a los alquileres y movimientos de pago. Permite visualizar detalles y marcar recibos como pagados.
   - Acciones comunes: emitir recibo, marcar pago, imprimir/exportar recibo.

- **Contabilidad**
   - Qué hace: Muestra movimientos bancarios y resumen contable relacionado con cobros y pagos.
   - Acciones comunes: registrar movimientos bancarios, conciliar pagos con recibos, ver balances por período.

- **Informes**
   - Qué hace: Genera listados e informes (por ejemplo, inmuebles disponibles, contratos próximos a vencer, ingresos por período).
   - Acciones comunes: seleccionar filtros (fecha, inmueble, inquilino) y exportar listados.

Nota: la interfaz concreta (etiquetas de botones, nombres) puede variar según la versión; los paneles usan métodos `refreshData()` que recargan la información desde la base de datos al navegar entre secciones.

## Siguientes pasos que puedo hacer por ti
- Actualizar `DatabaseConnection` para leer credenciales desde variables de entorno.
- Añadir `LICENSE` y `CONTRIBUTING.md` si quieres publicar el proyecto.

¿Quieres que adapte el README con capturas de pantalla de cada panel o que modifique `DatabaseConnection` para usar variables de entorno ahora?
