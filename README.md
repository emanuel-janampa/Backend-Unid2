# EcoRecicla API — backend

Backend REST para el caso EcoRecicla. Este ZIP contiene **solo el servicio backend**; no incluye la aplicación Android.

## Tecnologías

- Kotlin 2.4.21 y Spring Boot 4.1.2
- Gradle Kotlin DSL y Gradle Wrapper 9.7.0 (versiones compatibles según la matriz de Kotlin)
- Spring Web MVC, Spring Data JPA, Spring Security y Bean Validation
- PostgreSQL 16
- JWT firmado mediante HMAC (vigencia predeterminada: 60 minutos)
- Docker Compose

## Requisitos

- JDK 21
- Docker Desktop con Docker Compose
- Conexión a Internet la primera vez que Gradle Wrapper descarga Gradle y dependencias Maven

## Opción A: iniciar backend y PostgreSQL con Docker

Desde la carpeta donde está este README:

```powershell
docker compose up --build
```

Docker inicia PostgreSQL, espera a que esté listo, compila la API y arranca el servicio. La API queda en `http://localhost:8080`.

Para detener los servicios:

```powershell
docker compose down
```

Para detenerlos y borrar también los datos de desarrollo (incluye tablas y usuarios creados):

```powershell
docker compose down -v
```

**Advertencia:** `down -v` borra permanentemente el volumen de PostgreSQL de este entorno local.

## Opción B: ejecutar desde VS Code o IntelliJ

Primero inicia solamente PostgreSQL:

```powershell
docker compose up -d postgres
```

Después, en otra terminal ubicada en la raíz del proyecto:

```powershell
.\gradlew.bat clean test
.\gradlew.bat bootRun
```

Linux/macOS:

```bash
docker compose up -d postgres
./gradlew clean test
./gradlew bootRun
```

El primer arranque crea el esquema JPA y carga los datos de demostración si la base de datos está vacía. Por defecto, Hibernate usa `ddl-auto=update` para facilitar desarrollo. Para producción, configura migraciones de esquema y `DDL_AUTO=validate`.

## Comprobación rápida

El endpoint de comprobación también está protegido por token, porque en el examen la única ruta pública debe ser la de inicio de sesión. Inicia sesión siguiendo el ejemplo de autenticación y usa `GET /api/health` con `Authorization: Bearer <token>`; la respuesta será `200 OK` con `{"status":"ok","service":"EcoAPI"}`. Sin token debe responder `401`.

## Usuarios de demostración

| Usuario | Contraseña | Rol |
|---|---|---|
| `operario1` | `Eco2026*` | OPERARIO |
| `supervisor1` | `Eco2026*` | SUPERVISOR |
| `admin` | `Eco2026*` | ADMINISTRADOR |

Las contraseñas se almacenan cifradas con BCrypt. Son credenciales educativas para local; cámbialas antes de cualquier despliegue real.

## Pruebas con Postman

Importa `EcoRecicla-Postman-Collection.json` en Postman. Ejecuta primero **01 - Login operario**; el script guarda el token automáticamente en la variable de colección. Para comprobar las acciones de supervisor o administrador, cambia las credenciales del request de login y ejecútalo otra vez. Algunas operaciones de prueba pueden responder `403` o `409` intencionalmente, de acuerdo con los privilegios y las reglas de negocio.

## Autenticación

`POST /api/v1/auth/login` recibe `usuario` y `contrasena`:

```json
{
  "usuario": "operario1",
  "contrasena": "Eco2026*"
}
```

La respuesta incluye `token`, `tipo: Bearer`, usuario, nombre, rol y privilegios. El token se firma y vence en una hora. En cada endpoint protegido se envía:

```http
Authorization: Bearer <token>
```

`GET /api/v1/auth/me` devuelve la identidad y los privilegios derivados del token verificado.

## Endpoints principales

Todas las rutas de negocio usan el prefijo `/api/v1`. Los listados utilizan el contrato común de paginación: `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas`, `ultima`.

| Método | Ruta | Privilegio | Comportamiento |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Público | Validar credenciales y emitir JWT |
| `GET` | `/api/v1/auth/me` | Autenticado | Identidad y privilegios actuales |
| `GET` | `/api/v1/rutas/dia?fecha=YYYY-MM-DD` | `PUNTO_LEER` | Ruta del día con puntos ordenados |
| `GET` | `/api/v1/rutas/{rutaId}` | `PUNTO_LEER` | Detalle de una ruta autorizada |
| `POST` | `/api/v1/rutas` | `PUNTO_GESTIONAR` | Crear y asignar una ruta a un operario |
| `POST` | `/api/v1/rutas/{rutaId}/cerrar` | `RUTA_CERRAR` | Cerrar si todos los puntos fueron atendidos u omitidos con motivo |
| `PUT` | `/api/v1/rutas/{rutaId}/puntos/{puntoId}/omision` | `RUTA_CERRAR` | Registrar motivo de omisión de un punto |
| `GET` | `/api/v1/puntos?page=0&size=20&zona=Norte` | `PUNTO_LEER` | Listado paginado con filtro opcional por zona |
| `POST` | `/api/v1/puntos` | `PUNTO_GESTIONAR` | Crear un punto de acopio |
| `GET` | `/api/v1/puntos/{puntoId}/pesajes?page=0&size=20` | `PESAJE_LEER` | Pesajes paginados del más reciente al más antiguo |
| `POST` | `/api/v1/puntos/{puntoId}/pesajes` | `PESAJE_CREAR` | Registrar un peso bruto y una tara; el servidor calcula el neto |
| `PUT` | `/api/v1/pesajes/{pesajeId}` | `PESAJE_EDITAR` | Corregir un pesaje sujeto a reglas de negocio |
| `PATCH` | `/api/v1/pesajes/{pesajeId}/anular` | `PESAJE_ANULAR` | Anular sin borrar físicamente el registro |

### Ejemplo: registrar un pesaje

```http
POST /api/v1/puntos/1/pesajes
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "material": "Papel",
  "pesoBruto": 25.500,
  "tara": 2.500,
  "observacion": "Contenedores recogidos en el turno de mañana"
}
```

No se envía `pesoNeto`: el servidor calcula `pesoBruto - tara` y rechaza la operación si el resultado no es mayor que cero. Una creación correcta devuelve `201 Created` con el identificador definitivo y el peso neto calculado.

### Ejemplo: anular un pesaje

```http
PATCH /api/v1/pesajes/1/anular
Authorization: Bearer <token de supervisor>
Content-Type: application/json
```

```json
{"motivo":"Registro duplicado"}
```

La anulación marca el registro como anulado; no elimina la fila de la base de datos.

## Reglas de negocio aplicadas en el servicio

- **RN-01:** no se puede registrar ni corregir un pesaje en una ruta cerrada; responde `409 Conflict`.
- **RN-02:** el operario solo puede corregir sus propios pesajes y únicamente durante el mismo día; en caso contrario responde `403 Forbidden`. El supervisor puede corregir conforme a su privilegio.
- **RN-03:** el peso neto es calculado por el backend y debe ser mayor que cero; si no, responde `400 Bad Request` con detalle de validación.
- **RN-04:** no se cierra una ruta si hay puntos sin pesaje válido ni motivo de omisión; responde `409 Conflict` indicando cuántos puntos faltan.

La autorización se implementa por privilegios, no por comparaciones del nombre del rol dentro de los controladores. Las operaciones no autorizadas devuelven `403`; un token ausente, inválido o vencido devuelve `401`.

## Datos semilla

En una base vacía se crean:

- 3 usuarios de prueba, uno por rol.
- 7 privilegios y la matriz de privilegios del caso.
- 12 puntos de acopio distribuidos en las zonas Norte, Centro y Sur.
- 2 rutas para `operario1`: la ruta abierta de hoy (6 puntos, varios todavía sin atender) y la ruta cerrada de ayer.
- 15 pesajes de demostración, incluyendo un pesaje anulado y uno registrado por otro usuario para probar RN-02.

La carga se ejecuta solo cuando no existen usuarios. Para recrear los datos en Docker después de pruebas, detén la aplicación y usa `docker compose down -v`; esto elimina todos los datos del volumen.

## Contrato común de error

Las respuestas de error siguen este formato:

```json
{
  "timestamp": "2026-10-28T15:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Datos inválidos",
  "path": "/api/v1/puntos",
  "validationErrors": {
    "zona": "La zona es obligatoria"
  }
}
```

## Configuración útil

Las propiedades pueden ajustarse con variables de entorno:

| Variable | Valor predeterminado |
|---|---|
| `SERVER_PORT` | `8080` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/ecorecicla` |
| `DB_USER` | `ecorecicla` |
| `DB_PASSWORD` | `ecorecicla_dev` |
| `APP_JWT_SECRET` | Secreto local de demostración; reemplazar en despliegues |
| `APP_JWT_EXPIRATION_MINUTES` | `60` |
| `DDL_AUTO` | `update` |
| `SEED_ENABLED` | `true` |

En el emulador Android, la dirección local del host normalmente es `http://10.0.2.2:8080`. En un dispositivo físico se usa la IP LAN del equipo donde corre el backend y ambos deben estar en la misma red.

## Pruebas

```powershell
.\gradlew.bat test
```

Las pruebas utilizan una base de datos H2 en memoria; no necesitan un PostgreSQL externo. Incluyen comprobación de salud, rechazo de endpoint protegido sin token y login de demostración seguido de una consulta autenticada.

## Límites de este ZIP

Este artefacto implementa el backend EcoAPI. La base de datos local SQLDelight, la sincronización offline-first y la interfaz son responsabilidades del repositorio Android y no forman parte de este ZIP.
