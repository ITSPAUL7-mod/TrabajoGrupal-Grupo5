# Sistema de Reserva de Vehículos — Quarkus

Proyecto backend en Java 25 + Quarkus (RESTEasy Reactive + Jackson, Hibernate ORM con Panache, JDBC PostgreSQL) para un sistema de reserva de vehículos con 6 entidades.

## 1. Requisitos previos

- JDK 25
- Maven 3.9+
- PostgreSQL corriendo en `localhost:5432`

Crea la base de datos:

```sql
CREATE DATABASE reservas_vehiculos;
```

## 2. Configuración (`src/main/resources/application.properties`)

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/reservas_vehiculos

quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.log.sql=true

quarkus.http.port=8080
quarkus.http.cors=true
```

Ajusta `username`, `password` y `jdbc.url` a tu entorno local.

## 3. Ejecutar el proyecto

```bash
mvn quarkus:dev
```

La API queda disponible en `http://localhost:8080`.

## 4. Estructura de paquetes

```
uce.edu.ec
 ├── domain.model              -> Entidades JPA (extienden PanacheEntityBase, id de tipo Integer con SequenceGenerator)
 ├── infraestructure.repository -> XxxxRepositoryImpl implementando PanacheRepositoryBase<Entidad, Integer>
 ├── application.service.interceptor  -> Interceptores CDI y anotaciones de auditoría
 ├── application.service        -> XxxxService con la lógica de negocio y validaciones
 └── web.resource                -> XxxxResources con los endpoints JAX-RS
```

Los endpoints reciben y devuelven directamente las entidades. Para las entidades con relaciones (`Vehiculo`, `ReservaVehiculo`, `Vendedores`) el JSON de entrada envía el objeto relacionado anidado, indicando solo el dato necesario para ubicarlo; el servicio se encarga de buscar la entidad real en la base de datos antes de guardar.

## 5. Endpoints disponibles

Cada entidad expone las mismas 5 rutas, siguiendo el mismo patrón:

| Método | Ruta                              | Acción                          |
|--------|------------------------------------|----------------------------------|
| GET    | `/{entidad}/porId/{id}`            | Buscar por id                    |
| GET    | `/{entidad}/todos`                 | Listar todos                     |
| POST   | `/{entidad}/guardar`               | Crear                            |
| PUT    | `/{entidad}/actualizar/{id}`       | Actualizar                       |
| DELETE | `/{entidad}/eliminar/{id}`         | Eliminar                         |

Recursos:

- `/usuarios`
- `/vendedores`
- `/sucursales`
- `/vehiculos` 
- `/reservas`
- `/auditoria`
- `/estado-disponibilidad`

## 6. Orden de creación en Postman

Crea los recursos en este orden exacto para no violar restricciones de clave foránea.

### 6.1 POST `http://localhost:8080/usuarios/guardar`

### 6.2 POST `http://localhost:8080/vendedores/guardar`

### 6.3 POST `http://localhost:8080/sucursales/guardar`

### 6.4 POST `http://localhost:8080/vehiculos/guardar`

### 6.5 POST `http://localhost:8080/reservas/guardar`

