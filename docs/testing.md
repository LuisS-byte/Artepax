# Pruebas

El proyecto incluye JUnit Jupiter, Mockito y Testcontainers. Las fuentes se encuentran en [src/test/java](../src/test/java).

| Tipo | Ejemplos | Dependencias |
| --- | --- | --- |
| Unitarias y pruebas de componentes con mocks | `AuthControllerTest`, `LocalAuthServiceTest`, `EventoServiceImplTest`, `PagoServiceImplTest` | JVM y dependencias Maven |
| Carga de contexto | `ArtepaxApplicationTests` | Configuración de aplicación y una base aislada inicializada con el SQL de la demo |
| Integración | `EventoStockConcurrencyIT` | Docker disponible y acceso a la imagen PostgreSQL 16 Alpine |

`EventoStockConcurrencyIT` activa el perfil `test`, suministra la conexión con Testcontainers y usa el esquema generado para esa prueba. La anotación `disabledWithoutDocker = true` hace que se omita si Docker no está disponible. Puedes revisar la configuración en [application-test.properties](../src/test/resources/application-test.properties).

| Objetivo | Linux o macOS | Windows PowerShell |
| --- | --- | --- |
| Suite con descubrimiento predeterminado | `bash ./mvnw test` | `.\mvnw.cmd test` |
| Ejecutar una clase específica | `bash ./mvnw -Dtest=LocalAuthServiceTest test` | `.\mvnw.cmd "-Dtest=LocalAuthServiceTest" test` |
| Seleccionar clases de integración | `bash ./mvnw -Dtest="*IT" test` | `.\mvnw.cmd "-Dtest=*IT" test` |

El [POM](../pom.xml) carece de configuración explícita de Failsafe. La suite predeterminada de Surefire omite el sufijo `IT`, por lo que se selecciona expresamente. La suite predeterminada sí incluye la prueba de carga de contexto. Usa una base de desarrollo y revisa [las precondiciones](setup.md) antes de ejecutarla.

## CI y smoke de Compose

El workflow restaura PostgreSQL 18 y comprueba los objetos y catálogos. Luego ejecuta Maven con Java 21 y selecciona por separado las clases `*IT`. Otro job construye el stack de Compose, verifica Swagger, crea una cuenta local con credenciales efímeras y recorre catálogos, cliente, evento, reserva y pago. Los informes de OpenSpec corresponden a verificaciones históricas. Consulta el estado de cada ejecución en la pestaña Actions.
