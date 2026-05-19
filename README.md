# 🏠 KPIso — Gestión Gamificada de Tareas, Gastos Proporcionales y Lista de la Compra Inteligente

¡Bienvenido a **KPIso**! Una plataforma full-stack moderna y premium diseñada para revolucionar la convivencia, la cohabitación y el orden en viviendas compartidas. KPIso transforma las tareas del hogar mediante un sistema de **puntos KPI**, automatiza las finanzas compartidas mediante un innovador motor de **reparto proporcional exacto**, ofrece una **lista de la compra inteligente** con checkout integrado, y garantiza la armonía del hogar a través de transparencia total con registros de auditoría visuales.

---

## 📋 Alcance y Funcionalidades del Proyecto

### 1. 📋 Gestión Gamificada de Deberes (Tareas)
* **Sistema de Puntos KPI:** Cada tarea completada otorga puntos de recompensa (KPIs) a su ejecutor. Un marcador visible fomenta una sana competencia por mantener limpia la casa.
* **Configuración de Asignación Flexible:**
  * **Rotación Semanal:** Distribución rotativa periódica automática entre los miembros de la casa seleccionados.
  * **Tareas Fijas:** Asignación dedicada fija a un único conviviente.
  * **Días Específicos:** Planificación fina en días específicos de la semana con proyección visual de ocurrencias.
* **Calendario Interactivo:** Vista fluida en formato mensual y de listado que soporta **Drag-and-Drop** para reprogramar deberes de forma intuitiva, adaptando las fechas de vencimiento en caliente.
* **Actualización en un Clic:** Panel interactivo para marcar tareas como completadas o pendientes con respuesta visual inmediata.

### 2. 💶 Gastos Compartidos y Balances Proporcionales
* **Registro de Gastos:** Introduce facturas, compras de suministros o servicios compartidos, detallando quién realizó el pago.
* **Reparto Proporcional Exacto (`exactSplits`)**: Superando el modelo de división simple a partes iguales, KPIso permite definir de forma precisa la porción del importe total que le corresponde pagar a cada participante, adaptándose a consumos asimétricos.
* **Panel de Saldos y Liquidación Recomendada**:
  * Muestra el saldo neto global de cada conviviente (positivo o negativo) según sus aportaciones y consumos.
  * **Motor de Liquidación Cruzada Inteligente**: Calcula de forma automática la cantidad exacta que cada deudor debe transferir a cada acreedor respetando de forma proporcional las divisiones personalizadas.
* **Liquidación Instantánea**: Permite registrar transferencias individuales entre integrantes para saldar deudas pendientes con un solo clic.

### 3. 🛒 Lista de la Compra Inteligente e Integración de Checkout
* **Buscador Asistido por API (Open Food Facts)**: Sugiere productos en tiempo real a medida que escribes e importa imágenes y precios estimados de forma automatizada.
* **Selector de Asignación por Producto**: Mediante un menú desplegable interactivo, asocia en caliente qué integrantes consumirán cada producto de la lista.
* **Checkout Integrado ("Pagar compra")**:
  * Unifica la adquisición de todos los artículos pendientes en una sola acción.
  * Permite establecer manualmente el precio total del ticket real pagado en el supermercado y el pagador del mismo.
  * **Cálculo Proporcional de Compra**: Distribuye el importe real entre los convivientes basándose de forma proporcional en el peso estimado de cada artículo y sus consumidores asignados, registrando de forma automática un gasto con `exactSplits` en el sistema de cuentas de la casa.
  * **Historial Agrupado por Transacciones**: Los productos comprados se archivan agrupados de forma independiente por cada checkout físico realizado, mostrando la fecha y hora de la compra (`Compra del DD/MM/YYYY a las HH:mm`), en lugar de una lista plana.
  * **Sincronización en Tiempo Real**: Refresca automáticamente todo el panel lateral de balances y la lista de gastos general tras realizar el pago sin requerir recargar la página.

### 4. 🛡️ Auditoría y Transparencia Total
* **Registro de Actividades (Audit Trail)**: Panel inalterable que almacena y muestra de forma cronológica todas las acciones críticas (creación de tareas, cambios de asignaciones, eliminación de productos, registros de gastos y liquidaciones) detallando marcas de tiempo e identificadores de usuario para garantizar total claridad y confianza en la convivencia.

### 5. 🎨 UX/UI Glassmorphism y Personalización Premium
* **Estilo Visual Moderno**: Interfaz estilizada con efectos de desenfoque de fondo (`backdrop-filter`), sombras suaves y bordes redondeados.
* **Modo Oscuro / Modo Claro Dinámico**: Toggle interactivo que adapta instantáneamente la combinación de colores y contrastes.
* **Color Representativo**: Cada miembro del hogar puede elegir su color en formato hexadecimal para personalizar su presencia visual, teñir sus tareas asignadas y representarse en gráficos.
* **Gestión de Perfil**: Panel para actualizar nombre de usuario, contraseña, dirección de correo, imagen de avatar mediante URL o desactivar la cuenta.
* **Código de Invitación Único**: Sistema para invitar a nuevos convivientes al hogar de forma ágil y segura.

---

## 🛠️ Stack Tecnológico

### **Backend (KPIso-backend)**
* **Core:** Java 25, Spring Boot 3.4.2
* **Seguridad:** Spring Security, cifrado de contraseñas con **BCrypt**, tokens de acceso JWT compactos (`jjwt`).
* **Persistencia:** Spring Data JPA, Hibernate, Hibernate Envers (Control de auditoría y versiones de entidades).
* **Base de Datos:** PostgreSQL 15 (Alpine).
* **Gestión de Dependencias y Boilerplate:** Maven, Project Lombok 1.18.46 (compatible con JDK 25).
* **Validación:** JSR 380 (`spring-boot-starter-validation`).

### **Frontend (KPIso-frontend)**
* **Core:** React 19, Vite 8
* **Enrutamiento:** React Router 7 (`react-router-dom`)
* **Cliente HTTP:** Axios preconfigurado con interceptores.
* **Estilizado (CSS):** Vanilla CSS optimizado con variables de diseño personalizadas (`tokens.css`, `components.css`) y Tailwind CSS para utilidades ágiles de maquetación.
* **Iconografía:** Tabler Icons React.

### **Infraestructura y Contenedores**
* **Orquestación:** Docker Compose.
* **Servidor Web / Proxy Inverso:** Nginx configurado dentro del contenedor del frontend para redirigir las peticiones de `/api/v1` al backend de Spring Boot, evitando bloqueos de CORS y facilitando el despliegue monopuerto.

---

## 📂 Estructura del Monorrepositorio

```text
KPIso/
├── docker-compose.yml       # Configuración multiservicio de contenedores (db, backend, frontend)
├── .env                     # Variables de entorno globales del sistema
├── KPIso-backend/           # Código fuente y configuración del backend Spring Boot
│   ├── Dockerfile
│   ├── pom.xml              # Definición de dependencias Maven
│   └── src/
│       ├── main/java/com/kpiso/api/
│       │   ├── config/      # Configuración de Seguridad, JWT, CORS y RestTemplate
│       │   ├── core/        # Entidades base, clases comunes y log de auditoría
│       │   └── modules/     # Módulos funcionales (auth, user, house, task, expense, shoppinglist, activity)
│       └── test/            # Suite de pruebas unitarias y de integración (JUnit 5, Mockito)
└── KPIso-frontend/          # SPA en React + Vite
    ├── Dockerfile
    ├── package.json
    ├── nginx.conf           # Configuración de Nginx y proxy inverso de la API
    └── src/
        ├── api/             # Cliente de Axios configurado
        ├── components/      # Componentes comunes de Layout, Tareas, Gastos y Lista de la Compra
        ├── context/         # Contexto de Autenticación de React
        ├── pages/           # Vistas principales (Dashboard, Detalle del Hogar, Perfil, Login, Registro)
        └── styles/          # Hojas de estilo Vanilla CSS y variables globales
```

---

## ⚙️ Configuración y Despliegue

### Requisitos Previos
* **Docker** y **Docker Compose**
* **Node.js** (v18 o superior - para desarrollo local del frontend sin Docker)
* **Java 25 / Maven** (para desarrollo local del backend sin Docker)

---

### Despliegue Completo con Docker 🐳

1. **Configurar Variables de Entorno:**
   El proyecto incluye un archivo `.env` en la raíz del repositorio con variables listas para desarrollo local:
   ```env
   DB_USER=postgres
   DB_PASSWORD=postgres
   DB_NAME=kpiso
   DB_PORT=5432
   FRONTEND_URL=http://localhost:5173
   BACKEND_URL=http://localhost:8080
   VITE_API_URL=/api/v1
   ```

2. **Compilar y Levantar Contenedores:**
   Ejecuta el siguiente comando en la raíz del proyecto:
   ```bash
   docker compose up --build -d
   ```

3. **Acceso a los Servicios:**
   * **Aplicación Web (SPA):** [http://localhost](http://localhost) (Puerto 80, servido por Nginx).
   * **API Backend:** [http://localhost:8080/api/v1](http://localhost:8080/api/v1).
   * **Base de datos (PostgreSQL):** `localhost:5432` con usuario `postgres` y contraseña `postgres`.

---

### Desarrollo Local (Modo Híbrido / Sin Docker) 💻

Para agilizar el desarrollo con recarga rápida de código en caliente:

#### 1. Iniciar Base de Datos (Docker)
Levanta únicamente el contenedor de base de datos de PostgreSQL:
```bash
docker compose up -d db
```

#### 2. Ejecutar el Backend (Spring Boot)
Accede al directorio del backend y arranca la aplicación utilizando el wrapper de Maven:
```bash
cd KPIso-backend
./mvnw spring-boot:run
```
El servidor backend estará disponible en [http://localhost:8080](http://localhost:8080).

#### 3. Ejecutar el Frontend (Vite)
Accede al directorio del frontend, instala las dependencias e inicia el servidor de desarrollo Vite:
```bash
cd KPIso-frontend
npm install
npm run dev
```
La aplicación web estará disponible con recarga en caliente instantánea en [http://localhost:5173](http://localhost:5173).

---

## 🔒 Arquitectura de Software y Buenas Prácticas

* **Principios SOLID:** Código desacoplado con componentes de responsabilidad única en React y servicios enfocados en Spring Boot.
* **Seguridad Robusta:** Control de acceso en la API a nivel de método basado en roles, filtrado CORS preciso, encriptación segura de credenciales y sanitización de peticiones.
* **Modularidad Dinámica:** Diseño de módulos encapsulados e independientes en el backend para facilitar la mantenibilidad y la escalabilidad del sistema de cohabitación.

---

## 📄 Licencia
Este proyecto está bajo la Licencia MIT.

---
*Diseñado con ❤️ para hacer de la convivencia compartida una experiencia ordenada, divertida y justa.*
