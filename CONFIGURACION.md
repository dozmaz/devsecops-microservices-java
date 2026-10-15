# Pipeline CI/CD — user-service (Maven + Docker Hub)

Ya se tiene Jenkins, SonarQube y Docker corriendo.

## 1. Configuracion en SonarQube

> URL: `http://localhost:9000`

### 1.1 Crear el proyecto

```
Projects → Create Project → Manually
→ Project key:   user-service          ← debe coincidir con SONAR_PROJECT_KEY en Jenkinsfile
→ Display name:  User Service
→ Set Up → Locally → Use existing token
```

Si no se tiene token, generar uno nuevo:

```
My Account → Security → Generate Tokens
→ Name: jenkins-token
→ Type: Global Analysis Token
→ Generate → copia el token
```

---

## 2. Configuracion en Jenkins

### 2.1 Plugin adicional necesario

Ve a **Manage Jenkins → Plugins → Available plugins** e instala:

| Plugin | Para que sirve |
|---|---|
| **Pipeline Maven Integration** | Soporte para builds Maven en Jenkins |

(Pipeline, JaCoCo, SonarQube Scanner, Workspace Cleanup ya estan instalados)

### 2.2 Registrar Maven en Global Tool Configuration

Ve a **Manage Jenkins → Tools → Maven**:

```
Maven → Add Maven
→ Name:   Maven-3             ← debe coincidir con maven 'Maven-3' en Jenkinsfile
→ Marca "Install automatically"
→ Version: 3.9.x
→ Save
```

### 2.3 Credenciales en Jenkins

Ve a **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**.

#### 2.3.1 Token SonarQube (si no se tiene aun)

```
Kind:        Secret text
Secret:      <token de SonarQube>
ID:          sonarqube-token
Description: SonarQube Analysis Token
```

#### 2.3.2 Credenciales Docker Hub (NUEVO — no existia en el ejemplo)

```
Kind:        Username with password
Username:    <tu usuario de Docker Hub>
Password:    <tu contrasena o Access Token de Docker Hub>
ID:          dockerhub-credentials
Description: Docker Hub Credentials
```

> Recomendado: usa un Access Token desde hub.docker.com → Account Settings → Security → Personal Access Token

### 2.4 Configurar SonarQube en Jenkins

Ve a **Manage Jenkins → Configure System → SonarQube servers**:

```
☑ Environment variables
Add SonarQube:
→ Name:               SonarQube
→ Server URL:         http://sonarqube:9000
→ Server auth token:  sonarqube-token
→ Save
```

### 2.5 Webhook en SonarQube (Quality Gate)

En SonarQube: **Administration → Configuration → Webhooks → Create**

```
Name: jenkins
URL:  http://jenkins:8080/sonarqube-webhook/
→ Create
```

---

## 3. Ajustar el Jenkinsfile con tu repositorio Git

En `Jenkinsfile` va la URL del git clone (stage Checkout):

```groovy
sh 'git -c http.sslVerify=false clone https://github.com/dozmaz/devsecops-microservices-java.git .'
```

---

## 4. Crear el pipeline en Jenkins

```
Jenkins Dashboard → New Item
→ Name: user-service-pipeline
→ Type: Pipeline
→ OK
```

**Opcion A — Desde SCM (recomendado)**

```
Definition: Pipeline script from SCM
SCM: Git
Repository URL: <URL de tu repositorio>
Branch: */main
Script Path: Jenkinsfile
→ Save
```

**Opcion B — Pegar directamente**

```
Definition: Pipeline script
→ Pega el contenido del Jenkinsfile
→ Save
```

---

## 5. Stages del pipeline

| Stage | Comando Maven | Salida |
|---|---|---|
| **Build** | `mvn -pl user-service -am clean package -DskipTests` | FAT JAR en `user-service/target/*.jar` |
| **Testing** | `mvn -pl user-service -am test` | Surefire XML + JaCoCo XML/HTML |
| **Sonar** | `mvn -pl user-service -am sonar:sonar` | Analisis en SonarQube + Quality Gate |
| **Deploy a Docker Hub** | `docker build + docker push` | Imagen `<usuario>/user-service:BUILD_NUMBER` y `:latest` |

### Rutas de reportes

```
user-service/target/surefire-reports/*.xml         ← JUnit (Jenkins)
user-service/target/site/jacoco/jacoco.xml         ← JaCoCo XML (SonarQube)
user-service/target/site/jacoco/index.html         ← JaCoCo HTML (Jenkins)
user-service/target/user-service-1.0-SNAPSHOT.jar  ← FAT JAR ejecutable
```

---

## 6. Verificar resultado en Docker Hub

Despues del pipeline la imagen estara en:

```
https://hub.docker.com/r/<TU_USUARIO>/user-service
Tags: latest, <BUILD_NUMBER>
```

---

## 7. Troubleshooting

| Problema | Solucion |
|---|---|
| `maven 'Maven-3' not found` | Verifica nombre en Jenkins → Tools → Maven |
| `docker: command not found` | `docker exec -u root jenkins bash -c "apt-get install -y docker.io"` |
| `denied: requested access to the resource is denied` | Verifica credencial `dockerhub-credentials` en Jenkins |
| Quality Gate timeout | Verifica webhook en SonarQube (paso 2.5) |
| `sonar:sonar failed - project not found` | Verifica que `sonar.projectKey=user-service` coincida con el proyecto en SonarQube |
| Tests fallan por DB/Eureka | Normal: los tests usan Mockito, NO necesitan MySQL ni Eureka |
| `cannot find symbol (setId, getName...)` | Ejecuta Maven con JDK-21 (no JDK-25) |

---

## 8. Comandos utiles

```bash
# Verificar conectividad Jenkins → SonarQube
docker exec jenkins curl -s http://sonarqube:9000/api/system/status

# Verificar que Jenkins tiene Docker
docker exec jenkins docker --version

# Ver logs Jenkins
docker logs -f jenkins
```

---

## URLs de referencia

| Servicio | URL |
|---|---|
| Jenkins | http://localhost:8080 |
| SonarQube | http://localhost:9000 |
| Docker Hub | https://hub.docker.com |
