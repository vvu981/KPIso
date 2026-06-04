# 🏠 KPIso — Gestión Gamificada de Tareas, Gastos Proporcionales y Lista de la Compra Inteligente

¡Bienvenido a **KPIso**! Una plataforma full-stack moderna y premium diseñada para revolucionar la convivencia, la cohabitación y el orden en viviendas compartidas. KPIso transforma las tareas del hogar mediante un sistema de **puntos KPI**, automatiza las finanzas compartidas mediante un innovador motor de **reparto proporcional exacto**, ofrece una **lista de la compra inteligente** con checkout integrado, y garantiza la armonía del hogar a través de transparencia total con registros de auditoría visuales.

---

## 📋 Alcance y Funcionalidades del Proyecto

### 1. 📋 Gestión Gamificada de Deberes (Tareas) y Sistema de Puntos KPI
KPIso introduce una mecánica gamificada para incentivar el cumplimiento a tiempo de las responsabilidades del hogar. El sistema evalúa el desempeño de los integrantes de forma mensual en función del estado de las tareas y sus asignaciones:

* **Fórmula de Asignación Temporal:** Las tareas pueden asignarse bajo tres modalidades:
  * **Rotación Semanal:** Distribución rotativa periódica automática entre los miembros de la casa seleccionados.
  * **Tareas Fijas:** Asignación dedicada fija a un único conviviente.
  * **Días Específicos:** Planificación fina en días específicos de la semana con proyección visual en un calendario mensual interactivo.
* **Cálculo Mensual de Puntos KPI:**
  Se evalúan únicamente las tareas cuya fecha de evaluación (`completedAt` si está completada, o `dueDate` si está pendiente) pertenece al mes y año actuales. La puntuación individual de cada miembro se calcula según las siguientes reglas de negocio:
  * **Caso A: Rescate con Retraso (Late Rescue):** Si una tarea es completada por un usuario (rescatador) que **no es el usuario asignado originalmente**, y la fecha de finalización (`completedAt`) es **posterior** a la fecha de vencimiento (`dueDate`):
    * El usuario que completó la tarea (rescatador) recibe la totalidad de los puntos:
      $$\text{Puntos}_{\text{rescatador}} \leftarrow \text{Puntos}_{\text{rescatador}} + \text{Puntos de la tarea}$$
    * El usuario asignado originalmente (que no cumplió a tiempo) es penalizado restándole los puntos correspondientes:
      $$\text{Puntos}_{\text{asignado}} \leftarrow \text{Puntos}_{\text{asignado}} - \text{Puntos de la tarea}$$
  * **Caso B: Cumplimiento Normal:** Si la tarea es completada a tiempo, o es completada por el usuario que la tenía asignada, o se completó sin asignación previa:
    * El usuario ejecutor se lleva la recompensa:
      $$\text{Puntos}_{\text{ejecutor}} \leftarrow \text{Puntos}_{\text{ejecutor}} + \text{Puntos de la tarea}$$
  * **Caso C: Tarea Pendiente Expirada:** Si una tarea sigue en estado `PENDING`, su fecha límite (`dueDate`) ya ha pasado (es menor que la hora actual) y tiene un conviviente asignado:
    * El conviviente asignado es penalizado restándole los puntos de la tarea por negligencia:
      $$\text{Puntos}_{\text{asignado}} \leftarrow \text{Puntos}_{\text{asignado}} - \text{Puntos de la tarea}$$

---

### 2. 💶 Gastos Compartidos, Balances y Motor de Liquidación Cruzada
El sistema financiero de KPIso rastrea cada aportación individual para calcular de forma transparente el saldo neto global de cada miembro del hogar, facilitando acuerdos justos sin fricciones.

#### A. Reparto de Gastos (`exactSplits`)
Cuando un usuario registra un gasto, este puede dividirse de dos maneras:
1. **Reparto Equitativo por Defecto (Dividido al céntimo):** Si no se especifican coeficientes manuales, la aplicación divide el importe total equitativamente entre los participantes. Para evitar descuadres decimales originados por divisiones inexactas, el sistema realiza la distribución a nivel de céntimos:
   * Se convierte el importe total a céntimos: $\text{TotalCéntimos} = \text{round}(\text{Importe} \times 100)$.
   * Se obtiene la cuota base en céntimos: $\text{CuotaBase} = \lfloor \text{TotalCéntimos} / N \rfloor$ (donde $N$ es el número de participantes).
   * El sobrante restante $\text{Remanente} = \text{TotalCéntimos} \pmod N$ se distribuye de a $1\text{ céntimo}$ de forma secuencial a los primeros participantes de la lista.
2. **Reparto Proporcional Personalizado:** Los usuarios pueden definir un mapa de repartos exactos (`exactSplits`) asignando cantidades monetarias explícitas a cada integrante del gasto según su consumo real.

#### B. Cálculo del Balance Neto Individual
El balance neto global de cada conviviente ($U$) se determina analizando los gastos no liquidados (`settled = false`):
$$\text{Balance}_U = \sum_{g \in \text{Gastos como Pagador}} \text{Importe}_g - \sum_{g \in \text{Gastos como Participante}} \text{Cuota de } U \text{ en } g$$
* *Redondeo de Seguridad:* Si el valor absoluto del balance resultante es menor o igual a $0.05\text{ €}$, se ajusta automáticamente a $0.00\text{ €}$ para ignorar desajustes decimales insignificantes de redondeo.

#### C. Motor de Liquidación Recomendada (Algoritmo de Matching)
Para simplificar los cobros y evitar transferencias redundantes, KPIso implementa un algoritmo ávido (Greedy) que optimiza las transacciones de pago cruzado:
1. Se extrae el balance neto de todos los miembros. Aquellos cuyos balances sean despreciables (valor absoluto $\le 0.01\text{ €}$) se excluyen de la liquidación.
2. Se dividen en dos grupos:
   * **Acreedores:** Integrantes con balance neto positivo ($\text{Balance} > 0$).
   * **Deudores:** Integrantes con balance neto negativo ($\text{Balance} < 0$), representados por su deuda absoluta.
3. Se ordenan ambos grupos y se van emparejando usando un proceso iterativo con punteros:
   * En cada paso, el deudor actual transfiere al acreedor actual la cantidad mínima entre la deuda disponible y el crédito disponible:
     $$\text{Importe de Transferencia} = \min(\text{Deuda Restante}, \text{Crédito Restante})$$
   * Se descuenta dicha cantidad de ambos balances.
   * Si la deuda del deudor se salda completamente, se avanza al siguiente deudor.
   * Si el crédito del acreedor se cubre por completo, se avanza al siguiente acreedor.
4. El motor genera una lista simplificada de transferencias recomendadas (ej. *"Usuario A debe transferir X € a Usuario B"*). Las deudas pueden saldarse registrando transferencias individuales o archivando en bloque todos los gastos corrientes como liquidados (`settled = true`).

---

### 3. 🛒 Lista de la Compra Inteligente y Checkout Proporcional
La lista de la compra de KPIso agiliza el abastecimiento del hogar mediante la automatización de la estimación de costes y la integración con el sistema contable general.

* **Buscador Asistido (Open Food Facts):** Al escribir el nombre de un artículo, el sistema realiza sugerencias en tiempo real consultando la base de datos externa de alimentos. Importa de forma automatizada las etiquetas de categoría del producto, su imagen promocional y estima un coste aproximado en base a su clasificación de catálogo (con soporte de sobrescritura de precio manual).
* **Asignación de Consumidores:** Cada producto de la lista incluye un selector de usuarios asignados. Esto delimita quién va a consumir o beneficiarse de dicho producto. Si se deja vacío, se asume por defecto que el producto es consumido por todos los miembros del hogar.
* **Algoritmo de Checkout y Prorrateo del Ticket:**
  Cuando se realiza la compra física y se pulsa "Pagar compra", el usuario ingresa el **importe real pagado en el supermercado** ($\text{ImporteReal}$) y el pagador del ticket. El sistema realiza los siguientes pasos matemáticos para distribuir los costes justamente:
  1. Se calcula el presupuesto estimado acumulado de todos los artículos pendientes en la lista:
     $$\text{PresupuestoEstimado} = \sum_{i \in \text{Items Pendientes}} \text{PrecioEstimado}_i$$
     *(Si el presupuesto estimado es 0, se toma como 1 para evitar divisiones por cero).*
  2. Para cada producto individual ($i$) en la lista, se determina su proporción respecto al valor total de la cesta:
     $$\text{Proporción}_i = \frac{\text{PrecioEstimado}_i}{\text{PresupuestoEstimado}}$$
  3. Se calcula el coste real imputado a ese producto específico escalando el precio real total del ticket según su proporción:
     $$\text{PrecioReal}_i = \text{ImporteReal} \times \text{Proporción}_i$$
  4. El coste del producto se divide en partes iguales únicamente entre sus consumidores asignados ($C_i$):
     $$\text{Cuota por Consumidor del Item}_i = \frac{\text{PrecioReal}_i}{|C_i|}$$
  5. Se consolidan las porciones de todos los productos y se acumulan para cada usuario, generando un mapa de repartos exactos (`exactSplits`).
  6. Los artículos de la lista se marcan como comprados (`status = BOUGHT`) y se les asigna un identificador único de transacción (`checkoutId`) que agrupa la compra cronológicamente.
  7. Se registra automáticamente un nuevo Gasto compartido en el hogar con el título `"Compra DD/MM/YYYY"`, asignándole el importe real y el desglose de `exactSplits` calculado en el paso 5.

---

### 4. 🛡️ Auditoría y Transparencia Total
* **Registro de Actividades (Audit Trail):** Todas las acciones críticas (creación, edición o eliminación de tareas y gastos, registros de checkouts de compras y liquidaciones) generan un registro inalterable que detalla la acción, el usuario responsable, la casa y la marca de tiempo exacta. Esto previene conflictos y asegura claridad absoluta en las cuentas e historial del hogar.

---

### 5. 🎨 UX/UI Glassmorphism y Personalización Premium
* **Estilo Visual Moderno:** Interfaz estilizada con efectos de desenfoque de fondo (`backdrop-filter`), sombras suaves y bordes redondeados.
* **Modo Oscuro / Modo Claro Dinámico:** Toggle interactivo que adapta instantáneamente la combinación de colores y contrastes.
* **Color Representativo:** Cada miembro del hogar puede elegir su color en formato hexadecimal para personalizar su presencia visual, teñir sus tareas asignadas y representarse en gráficos.
* **Gestión de Perfil:** Panel para actualizar nombre de usuario, contraseña, dirección de correo, imagen de avatar mediante URL o desactivar la cuenta.
* **Código de Invitación Único:** Sistema para invitar a nuevos convivientes al hogar de forma ágil y segura.

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
