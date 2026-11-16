# Práctica Calificada: Persistencia y Comunicación Multi-componente (S11 | AP8)

## 📌 Información General
- **Curso:** Orquestadores y Automatización de Software
- **Sesión:** S11 | AP8
- **Namespace:** `practica-s11`
- **Plataforma:** Kubernetes (Minikube / EKS / Cloud)
- **Arquitectura:** 2 Capas reactivas desacopladas (Spring WebFlux + MongoDB persistente)

---

## 🏛️ Arquitectura General del Sistema

El proyecto implementa una arquitectura distribuida y desacoplada compuesta por tres capas integradas dentro del espacio de nombres `practica-s11`:

```mermaid
graph TD
    subgraph Exterior["Acceso Externo (Clientes / Navegador / Postman)"]
        User["Cliente HTTP / Pruebas REST"]
    end

    subgraph K8sCluster["Clúster Kubernetes (Namespace: practica-s11)"]
        subgraph ConfigLayer["Capa de Configuración y Seguridad"]
            CM["ConfigMap: app-configmap\n• DB_HOST: mongo-service\n• DB_PORT: 27017\n• DB_NAME: proddb\n• APP_PORT: 8080"]
            SEC["Secret: db-secret (Opaque)\n• MONGO_INITDB_ROOT_USERNAME (Base64)\n• MONGO_INITDB_ROOT_PASSWORD (Base64)"]
        end

        subgraph BackendLayer["Capa Reactiva (Spring WebFlux Backend)"]
            NodePortSvc["Service: backend-service\nTipo: NodePort (Puerto: 8080 | NodePort: 30081)"]
            BackPod["Pod: backend-deployment\n(Spring WebFlux + Actuator)\nInyección simultánea: envFrom"]
            Probes["Sondas de Salud\n• Liveness: /actuator/health\n• Readiness: /actuator/health"]
        end

        subgraph StorageLayer["Capa de Persistencia NoSQL (MongoDB)"]
            MongoSvc["Service: mongo-service\nTipo: ClusterIP (Puerto: 27017)"]
            MongoPod["Pod: mongo-deployment\n(MongoDB 6.0)"]
            PVC["PersistentVolumeClaim: mongo-pvc\nCapacidad: 1Gi | Acceso: ReadWriteOnce\nMontaje: /data/db"]
        end
    end

    User -->|HTTP GET/POST :30081| NodePortSvc
    NodePortSvc --> BackPod
    BackPod -.-> Probes
    CM -.->|envFrom| BackPod
    SEC -.->|envFrom| BackPod
    CM -.->|configMapKeyRef| MongoPod
    SEC -.->|secretKeyRef| MongoPod
    BackPod -->|mongodb://admin:...@mongo-service:27017/proddb| MongoSvc
    MongoSvc --> MongoPod
    MongoPod -->|Montaje persistente en /data/db| PVC
```

---

## 📁 Estructura del Repositorio

```text
/
├── k8s/
│   ├── 00-namespace.yaml          # Aislamiento del entorno en namespace practica-s11
│   ├── 01-mongo-pvc.yaml           # Reclamo de almacenamiento persistente (1Gi, RWO)
│   ├── 02-app-configmap.yaml       # Variables públicas (DB_HOST, DB_PORT, DB_NAME, APP_PORT)
│   ├── 03-db-secret.yaml           # Credenciales confidenciales codificadas en Base64
│   ├── 04-mongo-stack.yaml         # Deployment de MongoDB con PVC montado + Service ClusterIP
│   └── 05-backend-stack.yaml       # Deployment Spring WebFlux (envFrom + Probes) + Service NodePort
├── src/                            # Código fuente reactivo en Spring Boot WebFlux
│   ├── pom.xml                     # Descriptor de dependencias Maven
│   ├── Dockerfile                  # Multi-stage build (Temurin JRE 17)
│   └── src/main/
│       ├── java/com/practica/webflux/
│       │   ├── Application.java
│       │   ├── model/Product.java
│       │   ├── repository/ProductRepository.java
│       │   └── controller/ProductController.java
│       └── resources/
│           └── application.properties
└── README.md                       # Documentación técnica completa
```

---

## ⚙️ Guía de Despliegue Paso a Paso

### Paso 1: Iniciar el clúster (si usas Minikube)
```bash
minikube start
```

### Paso 2: Compilar y construir la imagen del Backend
Dentro del demonio de Docker del clúster:
```bash
# Opción directa con Minikube:
minikube image build -t backend-webflux:1.0 ./src

# O si utilizas Docker local conectado a Minikube:
eval $(minikube docker-env)
docker build -t backend-webflux:1.0 ./src
```

### Paso 3: Aplicar los manifiestos en orden secuencial
```bash
# 1. Crear el Namespace aislado
kubectl apply -f k8s/00-namespace.yaml

# 2. Crear el almacenamiento persistente
kubectl apply -f k8s/01-mongo-pvc.yaml

# 3. Desplegar la configuración pública y privada
kubectl apply -f k8s/02-app-configmap.yaml
kubectl apply -f k8s/03-db-secret.yaml

# 4. Desplegar el stack de base de datos (MongoDB + PVC + ClusterIP)
kubectl apply -f k8s/04-mongo-stack.yaml

# 5. Desplegar el stack del backend reactivo (Deployment + Probes + NodePort)
kubectl apply -f k8s/05-backend-stack.yaml
```

---

## 📷 Evidencias Requeridas para el Informe Técnico

El informe debe incluir capturas claras y sustentadas de los siguientes 4 puntos:

### 1. Salud General del Clúster
Comando a ejecutar y capturar:
```bash
kubectl get all,pvc,configmaps,secrets -n practica-s11
```
* **Criterio a demostrar:** Todos los Pods (`backend` y `mongo`) deben figurar en estado `Running`, el PVC en `Bound`, los dos Services (`ClusterIP` y `NodePort`) activos, y los objetos `ConfigMap` y `Secret` listados.

### 2. Validación de Persistencia y Volúmenes
Comando a ejecutar y capturar:
```bash
kubectl describe pvc mongo-pvc -n practica-s11
```
* **Criterio a demostrar:** Demuestra que el PVC se encuentra en estado `Bound`, con capacidad solicitada de 1Gi y `AccessModes: ReadWriteOnce`, asociado correctamente al Pod de MongoDB.

### 3. Verificación de Inyección de Seguridad (`envFrom`)
Comando a ejecutar y capturar:
```bash
# Obtener el nombre del pod del backend
POD_BACKEND=$(kubectl get pods -n practica-s11 -l app=backend-webflux -o jsonpath='{.items[0].metadata.name}')

# Inspeccionar variables de entorno activas dentro del proceso
kubectl exec -it $POD_BACKEND -n practica-s11 -- env
```
* **Criterio a demostrar:** Verificar que las variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `APP_PORT` (del ConfigMap) y `MONGO_INITDB_ROOT_USERNAME`, `MONGO_INITDB_ROOT_PASSWORD` (del Secret) están inyectadas dinámicamente en el entorno de ejecución.

### 4. Prueba de Conectividad Multi-componente (REST API)
Obtén la URL accesible del servicio:
```bash
# En Minikube:
minikube service backend-service -n practica-s11 --url
# O directamente a la IP del nodo y puerto 30081:
curl http://$(minikube ip):30081/
```

Ejecuta pruebas funcionales reactivas hacia MongoDB:
```bash
# A) Verificar salud con Actuator
curl -i http://$(minikube ip):30081/actuator/health

# B) Insertar un producto reactivamente en MongoDB
curl -X POST http://$(minikube ip):30081/api/products \
     -H "Content-Type: application/json" \
     -d '{"name": "Cluster EKS Produccion", "category": "Cloud", "price": 120.50, "stock": 10}'

# C) Consultar los productos almacenados en MongoDB (comprobando persistencia)
curl -i http://$(minikube ip):30081/api/products
```
* **Criterio a demostrar:** Captura del navegador, Postman o terminal con respuesta `HTTP 200/201`, mostrando la lectura/escritura reactiva de datos desde Spring WebFlux hacia MongoDB.

---

## 📹 Estructura del Video Demostrativo (3 a 5 minutos)
1. **0:00 - 0:45:** Presentación del equipo y explicación de la arquitectura (Spring WebFlux + MongoDB + PVC).
2. **0:45 - 1:45:** Recorrido rápido por los manifiestos YAML (`envFrom`, montaje `/data/db`, probes `/actuator/health`).
3. **1:45 - 3:00:** Demostración en vivo de los comandos: `kubectl get all,pvc`, `kubectl describe pvc`, y verificación de `env`.
4. **3:00 - 4:30:** Prueba interactiva de inserción y consulta de datos en MongoDB vía `NodePort` y verificación de supervivencia de datos.
5. **4:30 - 5:00:** Conclusiones y cierre.

