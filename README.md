# 🏠 KPIso — Gestión Gamificada de Tareas y Gastos Compartidos

¡Bienvenido a **KPIso**! Una plataforma full-stack moderna y premium diseñada para revolucionar la convivencia y cohabitación. KPIso gamifica las tareas del hogar mediante un sistema de **puntos KPI**, automatiza las finanzas compartidas y ofrece transparencia total entre los convivientes mediante un registro detallado de actividades.

---

## 🚀 Características Principales

### 📋 Gestión de Deberes (Tareas) Gamificada
* **Sistema de Puntos KPI:** Cada tarea completada otorga puntos de recompensa a su ejecutor.
* **Rotación Dinámica e Inteligente:** 
  * **Rotación Semanal:** Distribución rotativa periódica entre miembros seleccionados.
  * **Tareas Fijas:** Asignación dedicada a un único conviviente.
  * **Días Específicos:** Planificación fina en días concretos de la semana con proyección de ocurrencias.
* **Calendario Interactivo:** Vista fluida mensual y de lista que soporta **Drag-and-Drop** para reprogramar deberes de forma intuitiva.
* **Actualización en Tiempo Real:** Marcar tareas como completadas o pendientes con un solo clic.

### 💶 Gestión de Gastos y Balances Transparentes
* **Registro Detallado:** Introduce facturas, compras y gastos de la vivienda.
* **Cálculo de Deudas Automatizado:** KPIso calcula dinámicamente quién debe a quién y el balance general (positivo o negativo) de cada miembro.
* **Liquidaciones en Un Clic:** Registra pagos individuales instantáneamente para saldar deudas pendientes.

### 🛡️ Transparencia Total e Historial
* **Registro de Actividades (Audit Trail):** Log visible de todas las acciones (creación de tareas, eliminación, edición, liquidaciones) con marcas de tiempo y autorías de forma visual e inalterable.

### 🎨 Personalización Premium y UX/UI
* **Estilo Futurista y Glassmorphism:** Interfaz limpia, bordes suaves, desenfoques de fondo premium (`backdrop-filter`) y paleta de colores armónica.
* **Modo Oscuro / Modo Claro Dinámico:** Cambia instantáneamente la atmósfera visual mediante el toggle inteligente.
* **Preferencia de Colores:** Cada conviviente puede elegir su propio color representativo que tiñe visualmente sus aportes y tareas asignadas.
* **Gestión de Perfil Completa:** Cambia avatar por URL, edita nombre de usuario, correo electrónico, actualiza contraseña o elimina la cuenta si decides marcharte.
* **Código de Invitación Único:** Comparte el código único de tu vivienda para que nuevos convivientes puedan unirse fácilmente.

---

## 🛠️ Stack Tecnológico

### **Backend (KPIso-backend)**
* **Core:** Java 17, Spring Boot 3.4.2
* **Seguridad:** Spring Security con autenticación basada en tokens JWT de alta seguridad (`jjwt`).
* **Persistencia:** Spring Data JPA, Hibernate, Hibernate Envers (Auditoría).
* **Base de Datos:** PostgreSQL 15 (Alpine).
* **Otros:** Lombok para código limpio, validación de inputs con JSR 380 (`spring-boot-starter-validation`).

### **Frontend (KPIso-frontend)**
* **Core:** React 19, Vite 8
* **Navegación:** React Router 7 (`react-router-dom`)
* **Cliente HTTP:** Axios para comunicación asíncrona optimizada.
* **Estilizado (CSS):** Combinación premium de Vanilla CSS optimizado con variables de diseño personalizadas (`tokens.css`, `components.css`) y utilidades rápidas de Tailwind CSS.

### **Infraestructura y Contenedores**
* **Orquestación:** Docker Compose para despliegue automatizado.
* **Servidor Web:** Nginx (dentro del contenedor frontend) actuando como reverse proxy para enrutar las peticiones `/api/v1` al backend sin conflictos de CORS en entornos productivos.

---

## 📂 Estructura del Proyecto

El monorrepocitorio está organizado de la siguiente manera:

```text
KPIso/
├── docker-compose.yml       # Configuración multiservicio (db, backend, frontend)
├── .env                     # Variables de entorno globales del sistema
├── KPIso-backend/           # Código fuente y configuración del backend Spring Boot
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/kpiso/api/
│       ├── config/          # Seguridad, CORS y codificación
│       ├── core/            # Entidades transversales y clases comunes
│       └── modules/         # Módulos de negocio (auth, user, house, task, expense, activity)
└── KPIso-frontend/          # Código fuente de la SPA en React + Vite
    ├── Dockerfile
    ├── package.json
    ├── nginx.conf           # Configuración de Nginx y Proxy de API
    └── src/
        ├── api/             # Cliente base de Axios preconfigurado
        ├── components/      # Componentes comunes de Layout y UI
        ├── context/         # Contexto de autenticación global
        ├── pages/           # Vistas (Dashboard, HouseDetail, Login, Register, etc.)
        └── styles/          # Tokens de diseño y variables globales CSS
```

---

## ⚙️ Configuración y Despliegue Local

### Requisitos Previos
Asegúrate de tener instalados los siguientes componentes:
* **Docker** y **Docker Compose**
* **Node.js** (opcional, solo para desarrollo local del frontend sin Docker)
* **Java 17 / Maven** (opcional, solo para desarrollo local del backend sin Docker)

---

### Despliegue Completo con Docker 🐳

Esta es la forma recomendada y más rápida de iniciar el proyecto en segundos.

1. **Configura las Variables de Entorno:**
   El proyecto incluye un archivo `.env` en la raíz con valores predeterminados listos para desarrollo local:
   ```env
   DB_USER=postgres
   DB_PASSWORD=postgres
   DB_NAME=kpiso
   DB_PORT=5432
   FRONTEND_URL=http://localhost:5173
   BACKEND_URL=http://localhost:8080
   VITE_API_URL=/api/v1
   ```

2. **Compila y Arranca los Contenedores:**
   Ejecuta el siguiente comando en la raíz del repositorio:
   ```bash
   docker compose up --build -d
   ```

3. **¡Accede a la Aplicación!**
   * **Frontend (App Web):** [http://localhost](http://localhost) (Puerto por defecto `80` a través de Nginx).
   * **Backend API:** [http://localhost:8080/api/v1](http://localhost:8080/api/v1).
   * **Base de Datos (PostgreSQL):** `localhost:5432` (Acceso externo habilitado).

---

### Desarrollo Local (Fuera de Docker) 💻

Si prefieres correr los servicios de manera nativa para agilizar el tiempo de recarga en caliente durante el desarrollo:

#### 1. Iniciar la Base de Datos (Docker)
Puedes levantar únicamente la base de datos PostgreSQL utilizando:
```bash
docker compose up -d db
```

#### 2. Arrancar el Backend (Spring Boot)
Ve al directorio del backend e inicia la aplicación Spring Boot utilizando Maven:
```bash
cd KPIso-backend
mvn spring-boot:run
```
El servidor backend se ejecutará en [http://localhost:8080](http://localhost:8080).

#### 3. Arrancar el Frontend (Vite)
Ve al directorio del frontend, instala las dependencias e inicia el servidor de desarrollo Vite:
```bash
cd KPIso-frontend
npm install
npm run dev
```
La SPA estará disponible con recarga en caliente instantánea en [http://localhost:5173](http://localhost:5173).

---

## 🔒 Seguridad y Buenas Prácticas

El proyecto ha sido concebido bajo los más rigurosos estándares arquitectónicos y de código limpio:
* **Principios SOLID:** Cumplimiento estricto en el frontend y backend. Los componentes de UI tienen una responsabilidad única y están desacoplados mediante inyección de dependencias implícita.
* **Control de Acceso Basado en Roles:** Las operaciones críticas (como eliminar la vivienda, expulsar miembros o configurar los datos de la casa) están restringidas en el backend a usuarios con rol `ADMIN`.
* **Protección contra Inyección y Vulnerabilidades:** Uso de JPA parameterized queries para erradicar ataques SQL Injection, cifrado fuerte de contraseñas con **BCrypt**, y control exhaustivo de CORS.

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Siéntete libre de clonarlo, modificarlo y adaptarlo a tus necesidades. 

---

*Desarrollado con ❤️ para hacer de la convivencia una experiencia divertida y ordenada.*
