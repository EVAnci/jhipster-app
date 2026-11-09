# Observabilidad con Elastic Stack

Infraestructura de observabilidad para centralizar los logs JSON de la aplicación **Gestión de logs** (Spring Boot / JHipster). Se utilizan **Filebeat, Logstash, Elasticsearch y Kibana**, ejecutados con Docker Compose; la aplicación Java se ejecuta directamente en el equipo.

## Arquitectura

```text
Spring Boot / Logback ──> ../logs/jhipster.json
                                │ lee el archivo (volumen de solo lectura)
                                ▼
                             Filebeat
                                │ Beats / puerto 5044
                                ▼
                             Logstash
                                │ procesa JSON
                                ▼
                          Elasticsearch
                                │ índices jhipster-logs-*
                                ▼
                              Kibana
                   (Discover, dashboard, alertas)
```

- **Logback:** produce eventos JSON estructurados de nivel `DEBUG`, `INFO`, `WARN` y `ERROR`.
- **Filebeat:** lee el archivo de logs y envía los eventos a Logstash.
- **Logstash:** interpreta el JSON, normaliza los campos y añade `application: gestionlogs`.
- **Elasticsearch:** guarda los documentos en índices `jhipster-logs-*`.
- **Kibana:** permite buscar, visualizar y detectar picos de errores.

La aplicación **no envía logs directamente a Logstash**.

## Requisitos

- Docker Engine y Docker Compose (`docker compose`).
- Aplicación Spring Boot en ejecución con logs en `../logs/jhipster.json`.
- Memoria suficiente para los cuatro servicios; el proyecto utiliza límites de memoria por contenedor.
- En Linux, comprobar `sysctl vm.max_map_count`; Elasticsearch puede requerir `1048576`.
- Archivo `.env` local con la clave de cifrado de Kibana, si la configuración de Compose actual la utiliza. **No versionar `.env`.**

Los siguientes comandos se ejecutan desde `jhipster-app/observability/`:

```bash
cd observability  # desde la raíz de jhipster-app
# Validar la configuración sin imprimir secretos expandidos
docker compose config --quiet
```

## Despliegue

Iniciar los servicios en este orden para facilitar la verificación:

```bash
docker compose up -d elasticsearch
docker compose up -d logstash
docker compose up -d filebeat
docker compose up -d kibana
docker compose ps
```

En otra terminal, desde la **raíz del proyecto**, iniciar la aplicación:

```bash
./mvnw
```

Verificar que Elasticsearch y Kibana respondan:

```bash
curl -s http://localhost:9200
curl -I http://localhost:5601
```

Comprobar que los logs llegaron a Elasticsearch:

```bash
curl -s 'http://localhost:9200/_cat/indices/jhipster-logs-*?v'
curl -s 'http://localhost:9200/jhipster-logs-*/_count?pretty'
```

Para inspeccionar eventos recientes:

```bash
curl -s 'http://localhost:9200/jhipster-logs-*/_search' \
  -H 'Content-Type: application/json' \
  -d '{"size":5,"sort":[{"@timestamp":"desc"}],"_source":["@timestamp","level","message","application"],"query":{"match_all":{}}}' \
  | jq '.hits.hits[]._source'
```

El último comando requiere `jq`.

## Kibana: consultas, dashboard y alertas

Abrir **http://localhost:5601** y seleccionar el Data View correspondiente al patrón **`jhipster-logs-*`**, con **`@timestamp`** como campo temporal. En Discover, ampliar el rango de tiempo (por ejemplo, *Last 7 days*) y utilizar consultas KQL como:

```text
level: "ERROR"
level: "WARN"
application: "gestionlogs"
level: "WARN" AND message: "Producto inexistente"
```

### Dashboard

Se creó un dashboard con tres visualizaciones:

1. Cantidad de eventos agrupados por `level`.
2. Evolución temporal de los eventos `ERROR`.
3. Listado de errores recientes.

El dashboard fue exportado a formato `.ndjson` para poder importarlo en otro entorno. Guardar el archivo exportado junto a esta configuración y utilizar **Stack Management → Saved Objects → Import** si es necesario restaurarlo.

### Regla de alerta

Se creó y probó la regla **`Pico de errores - JHipster`** de tipo **Elasticsearch query**, con estas condiciones:

| Configuración | Valor |
| --- | --- |
| Índices | `jhipster-logs-*` |
| Consulta KQL | `level: "ERROR"` |
| Agregación | `count()` sobre todos los documentos |
| Umbral | Mayor que 5 (al menos 6 eventos) |
| Ventana | Últimos 5 minutos |
| Frecuencia de ejecución | Cada 1 minuto |
| Acciones externas | Ninguna; consulta de alertas desde Kibana |

**Prueba realizada:** se generaron seis eventos `ERROR` de prueba desde `LogGenerator.java`. Kibana registró una alerta **Active**, con última ejecución **Succeeded**, el **9 de noviembre de 2026**. El bloque de simulación puede retirarse después de guardar la evidencia o mantenerse desactivado para futuras demostraciones.

Para volver a probarla, generar seis eventos `ERROR` dentro de cinco minutos, confirmar su ingreso en Discover y abrir **Stack Management → Rules → Pico de errores - JHipster** para revisar *Alerts* y *History*.

Para exportar la regla, utilizar **Stack Management → Saved Objects**, seleccionar el objeto de tipo regla si aparece y exportarlo como `.ndjson`. Si la exportación no aparece disponible en esa vista, conservar capturas y los parámetros anteriores para recrearla. Las reglas importadas pueden requerir habilitación y la configuración local de cifrado y permisos.

> El archivo `.env` y las claves de cifrado no se incluyen en las exportaciones destinadas al repositorio. Si se restaura el proyecto, configurar nuevamente la clave en el entorno y comprobar que Kibana la reciba.

## Operación y seguridad

```bash
docker compose ps
docker stats --no-stream
free -h
```

Para detener los contenedores sin eliminar datos:

```bash
docker compose down
```

Los volúmenes de Docker conservan los índices de Elasticsearch y el estado de lectura de Filebeat y Kibana. **No ejecutar `docker compose down -v`** salvo que se desee borrar esos datos.

**Alcance de seguridad:** entorno de laboratorio local. En la configuración de referencia, Elasticsearch no requiere autenticación y las interfaces web están ligadas a `127.0.0.1`; **no utilizar esta configuración en producción**. Evitar registrar contraseñas, JWT, cuerpos completos de peticiones o datos personales. El nivel `DEBUG` está reservado para desarrollo.

**Decisión de diseño:** una sola aplicación y un único repositorio, con la observabilidad desacoplada en esta carpeta; sin microservicios, sin Dockerfiles propios y sin herramientas adicionales fuera del alcance del TP.
