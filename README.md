# Gestión de logs

Aplicación monolítica desarrollada con **Spring Boot** (generada inicialmente con **JHipster**) para demostrar la gestión centralizada de logs. Expone una API REST con un CRUD de productos y genera eventos estructurados en JSON mediante **Logback**, que luego pueden ser recolectados y analizados con **Filebeat, Logstash, Elasticsearch y Kibana**.

## Arquitectura

La aplicación sigue una organización **por capas**:

- **Presentación/API REST:** `web/rest/ProductoResource.java`, con los endpoints de productos.
- **Persistencia:** `repository/ProductoRepository.java`, que accede a la base de datos mediante Spring Data JPA.
- **Dominio:** `domain/Producto.java`, que define la entidad `Producto` (`id`, `nombre`, `precio`).
- **Servicios de apoyo:** `service/LogGenerator.java`, que genera eventos de ejemplo para el laboratorio.

Para desarrollo se utiliza **H2 en memoria**, **Liquibase** para las migraciones y **JWT** para proteger la API. El logging está configurado en `src/main/resources/logback-spring.xml` y escribe eventos JSON, uno por línea, en `logs/jhipster.json`.

La infraestructura de observabilidad se encuentra en [`observability/`](observability/README.md) y funciona de forma independiente: **JHipster → archivo JSON → Filebeat → Logstash → Elasticsearch → Kibana**.

## Build y ejecución

Desde la raíz del proyecto, iniciar la aplicación con el perfil de desarrollo:

```bash
./mvnw
```

El _Maven Wrapper_ (`mvnw`) permite compilar y ejecutar el proyecto con la versión de Maven prevista, sin instalar Maven manualmente. Se necesita un JDK compatible con el proyecto (configurado para **Java 21**).

La API se publica en `http://localhost:8080`. Para comprobar que la aplicación está activa:

```bash
curl -i http://localhost:8080/management/health
```

Para compilar y ejecutar los tests:

```bash
./mvnw verify
```

Para generar un JAR de producción:

```bash
./mvnw -Pprod clean verify
```

> Para la demostración del TP se utiliza el perfil de desarrollo. La base H2 es **en memoria**: los cambios pueden perderse al reiniciar la aplicación. No es necesario construir un JAR de producción para ejecutar las pruebas de observabilidad.

## Ejemplos con `curl`

Los endpoints `/api/productos` requieren un **JWT**. Primero obtenerlo con una cuenta de prueba válida; no colocar contraseñas ni tokens reales en el README ni en Git.

```bash
read -r -p 'Usuario: ' USUARIO
read -r -s -p 'Contraseña: ' CLAVE; echo
TOKEN=$(curl -fsS http://localhost:8080/api/authenticate \
  -H 'Content-Type: application/json' \
  -d "$(jq -n --arg u "$USUARIO" --arg p "$CLAVE" '{username:$u,password:$p,rememberMe:false}')" \
  | jq -r '.id_token')
unset CLAVE
```

Estos ejemplos requieren `jq` para construir el JSON de autenticación y leer la respuesta. Ejecutar los siguientes comandos **en la misma terminal**, para reutilizar `$TOKEN`.

### Listar productos

```bash
curl -i http://localhost:8080/api/productos \
  -H "Authorization: Bearer $TOKEN"
```

### Crear un producto

```bash
curl -i -X POST http://localhost:8080/api/productos \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Cuaderno","precio":2500.00}'
```

La respuesta incluye el `id` generado. Sustituir `1001` en los ejemplos siguientes por el ID obtenido, o definirlo de esta manera:

```bash
ID=1001  # cambiar por el ID real
```

### Consultar un producto

```bash
curl -i "http://localhost:8080/api/productos/$ID" \
  -H "Authorization: Bearer $TOKEN"
```

### Actualizar un producto

```bash
curl -i -X PUT "http://localhost:8080/api/productos/$ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"id\":$ID,\"nombre\":\"Cuaderno grande\",\"precio\":3200.00}"
```

### Buscar un producto inexistente (genera un `WARN`)

```bash
curl -i http://localhost:8080/api/productos/99999999 \
  -H "Authorization: Bearer $TOKEN"
```

Esta búsqueda devuelve `404` y genera un mensaje similar a `Producto inexistente: id=99999999`.

### Eliminar un producto

```bash
curl -i -X DELETE "http://localhost:8080/api/productos/$ID" \
  -H "Authorization: Bearer $TOKEN"
```

Las operaciones del CRUD generan eventos `DEBUG`, `INFO` y `WARN`. Los eventos `ERROR` de prueba permiten comprobar la alerta configurada en Kibana. Para consultar los registros localmente:

```bash
tail -n 10 logs/jhipster.json
```

> Los logs no deben incluir contraseñas, tokens JWT ni datos personales innecesarios. El nivel `DEBUG` se utiliza solamente en desarrollo.
