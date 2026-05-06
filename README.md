# Objetivos de Ventas — App Android (Kotlin + Jetpack Compose)

Aplicación nativa Android para gestionar y dar seguimiento a objetivos comerciales con dos modos:

- **Modo Asesor (Vendedor)**: importa o carga manualmente su presupuesto mensual, registra resultados diarios y ve dashboard, proyección y semáforo de cumplimiento.
- **Modo Gerencia (Administrador)**: carga el presupuesto total de la sucursal, divide automáticamente entre asesores (1 a 50), exporta el presupuesto individual de cada asesor y lo comparte por WhatsApp / Email / archivo.

Compatible con **Android 7.0 (API 24) hasta Android 16 (API 35)**.

---

## 1. Stack técnico

| Capa | Tecnología |
|------|------------|
| Lenguaje | Kotlin 1.9.24 |
| UI | Jetpack Compose (Material 3) |
| Arquitectura | MVVM |
| Estado | `ViewModel` + `StateFlow` |
| Persistencia | Room 2.6.1 |
| Navegación | Navigation Compose 2.7.7 |
| Notificaciones | WorkManager 2.9.1 + NotificationCompat |
| Preferencias | DataStore Preferences 1.1.1 |
| Serialización | kotlinx.serialization (JSON) + CSV manual |
| Tests | JUnit, Espresso, Compose UI Test |

---

## 2. Requisitos para compilar

1. **Android Studio Hedgehog (2023.1.1)** o superior — recomendado **Android Studio Koala / Ladybug** para soporte Compose 2024.
2. **JDK 17** (Android Studio lo trae incluido).
3. **Android SDK Platform 35** (compileSdk = 35).
4. Un dispositivo Android 7.0+ o un emulador.

---

## 3. Instrucciones paso a paso para instalar la app en tu celular

### Opción A — Usar Android Studio (recomendado)

1. Abrí Android Studio y seleccioná **File → Open**.
2. Apuntá a la carpeta `SalesGoalsApp` (la raíz del proyecto, donde está `settings.gradle.kts`).
3. Esperá a que Gradle sincronice (la primera vez descarga dependencias y el wrapper de Gradle 8.9).
4. Conectá tu celular vía USB con **Depuración USB** activada (Ajustes → Opciones de desarrollador → Depuración USB).
5. Aceptá la huella en el celular cuando aparezca el diálogo "Permitir depuración USB".
6. En la barra superior de Android Studio, elegí el dispositivo y presioná **Run ▶** (Shift+F10).
7. La app se compila, instala y se abre.

### Opción B — Instalar APK por línea de comandos

Desde la raíz del proyecto:

```bash
# Linux / macOS
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Windows
gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Opción C — Generar APK firmado para release

```bash
./gradlew assembleRelease
# APK queda en: app/build/outputs/apk/release/app-release-unsigned.apk
```

Para distribuir por WhatsApp / email / drive: pasale el `.apk` al usuario y desde el celular debe permitir "instalar apps de fuentes desconocidas" para esa app de origen.

---

## 4. Estructura del proyecto

```
SalesGoalsApp/
├── settings.gradle.kts
├── build.gradle.kts                         # buildscript top-level
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts                     # módulo de la app
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/                             # recursos (strings, colores, themes, iconos)
        └── java/com/salesgoals/app/
            ├── MainActivity.kt              # actividad única (Compose)
            ├── SalesGoalsApplication.kt     # Application: inicializa Room + WorkManager
            ├── data/
            │   ├── database/                # Room: AppDatabase, DAOs
            │   ├── entities/                # Entidades Room
            │   ├── models/                  # Modelos serializables (DTOs/UI)
            │   └── repository/              # SalesRepository (única fuente de verdad)
            ├── notifications/
            │   ├── NotificationHelper.kt    # canal + mostrar notificaciones
            │   └── DailyReminderWorker.kt   # WorkManager periódico (recordatorio + alertas)
            ├── ui/
            │   ├── theme/                   # Colores, tipografía, MaterialTheme
            │   ├── components/              # ProgressCard, VariableInput, SectionHeader
            │   ├── navigation/AppNavGraph.kt# NavHost con rutas
            │   └── screens/
            │       ├── HomeScreen.kt        # Selección de modo
            │       ├── advisor/             # Pantallas + ViewModel del modo Asesor
            │       ├── manager/             # Pantallas + ViewModel del modo Gerencia
            │       └── common/DaysConfigScreen.kt
            └── utils/
                ├── Formatters.kt            # Formatos $ AR, fechas, parsing seguro
                ├── ExportImportHelper.kt    # Export/import JSON + CSV, share intents
                └── Prefs.kt                 # DataStore (modo, días, onboarded)
```

---

## 5. Explicación de cada módulo

### 5.1 `data/`

- **`models/Variables.kt`**: define `VariableSet` (volumen, crédito, garantía, crédito efectivo, celulares) con operadores `+` y `/` para que la división del presupuesto sea declarativa.
- **`models/DashboardData.kt`**: `VariableProgress` calcula en tiempo real `remaining`, `percent`, `dailyGoal`, `requiredPace`, `currentPace`, `projection` y el estado **semáforo** (En línea / Riesgo / Atrasado).
- **`models/ExportPayload.kt`**: `AdvisorBudgetPayload` es el JSON que la Gerencia exporta y el Asesor importa. Incluye versión para futura compatibilidad.
- **`entities/`**: `BudgetEntity` (presupuesto activo con id=1) y `DailyEntryEntity` (resultado por fecha, PK = `date` para upsert idempotente).
- **`database/`**: `AppDatabase` (Room v1), `BudgetDao` y `DailyEntryDao` con `Flow` para reactividad.
- **`repository/SalesRepository.kt`**: único punto de acceso a datos; mantiene a los ViewModels desacoplados de Room.

### 5.2 `ui/`

- **`theme/`**: paleta corporativa con verde/amarillo/rojo para el semáforo. Soporte automático claro/oscuro.
- **`components/ProgressCard.kt`**: card con barra de progreso, chip de estado, métricas (acumulado, restante, %, diario base, ritmo necesario, ritmo actual y proyección).
- **`components/VariableInput.kt`**: input numérico para las 5 variables, con teclado numérico y `supportingText`.
- **`screens/HomeScreen.kt`**: pantalla de bienvenida con dos cards grandes para elegir modo.
- **`screens/advisor/`**:
  - `AdvisorViewModel.kt` — combina `Budget` y `DailyEntry`s en `AdvisorUiState`, recalculando todo cuando cambian.
  - `AdvisorDashboardScreen.kt` — tarjetas de progreso por variable + accesos rápidos.
  - `DailyEntryScreen.kt` — carga / edición rápida (menos de 10 segundos por carga).
  - `HistoryScreen.kt` — listado del mes con editar/eliminar.
  - `ImportBudgetScreen.kt` — importa archivo JSON/CSV o carga manualmente.
- **`screens/manager/`**:
  - `ManagerViewModel.kt` — combina estado local editable + persistencia; calcula automáticamente la división por asesor y por día.
  - `BudgetSetupScreen.kt` — datos de la sucursal y presupuesto total.
  - `DistributionScreen.kt` — vista por asesor con botones **Compartir / WhatsApp / Email**.
  - `ManagerDashboardScreen.kt` — supervisión de la estructura.
- **`screens/common/DaysConfigScreen.kt`** — configuración de días laborales (1 a 31) con slider y campo numérico.

### 5.3 `notifications/`

- **`NotificationHelper.kt`** crea el canal y dispara notificaciones (Daily Reminder y Performance Alert).
- **`DailyReminderWorker.kt`** corre 1 vez al día (19:00 hs por default) usando `WorkManager`. Si no hay carga del día, recuerda. Si alguna variable está atrasada, muestra alerta.

### 5.4 `utils/`

- **`Formatters.kt`** — formato moneda en es-AR, fechas, parser seguro de `String → Double`.
- **`ExportImportHelper.kt`** — JSON con `kotlinx.serialization`, CSV manual; usa `FileProvider` para compartir.
- **`Prefs.kt`** — DataStore para preferencias de usuario (modo, días, onboarded).

---

## 6. Funcionalidades implementadas (mapeo con tu spec)

| Spec | Implementación |
|------|----------------|
| Cargar presupuesto mensual (Asesor) | `ImportBudgetScreen` → JSON/CSV o carga manual |
| Cargar presupuesto sucursal (Gerencia) | `BudgetSetupScreen` |
| Configuración de días 1-31 | `DaysConfigScreen` (slider + input) |
| División entre 1 a 50 asesores | `ManagerViewModel.updateAdvisorCount` + `DistributionScreen` |
| Objetivo individual y diario por asesor | calculado en `state.perAdvisor` y `state.perDayPerAdvisor` |
| Exportar JSON / CSV | `ExportImportHelper.exportAdvisorPayload(format)` |
| Compartir por WhatsApp / Email / Archivo | `whatsappIntent`, `emailIntent`, `shareIntent` |
| Importar archivo recibido | `ActivityResultContracts.OpenDocument` + `applyImportedPayload` |
| Carga diaria rápida | `DailyEntryScreen`, todos los campos en una pantalla |
| Edición de días anteriores | `HistoryScreen` → editar |
| Eliminación | `dailyEntryDao.deleteByDate` |
| Dashboard inteligente | `AdvisorDashboardScreen` + `ProgressCard` |
| Proyección a fin de mes | `VariableProgress.projection` |
| Semáforo (verde/amarillo/rojo) | `VariableProgress.status` |
| Cálculos automáticos en tiempo real | StateFlow + `combine` reaccionando a cambios |
| Historial vista lista | `HistoryScreen` |
| Notificación recordatorio diario | `DailyReminderWorker` |
| Alertas de bajo rendimiento | misma worker, evalúa estado BEHIND |
| Diseño minimalista, botones grandes | Material3, botones de 56dp |
| Colores semáforo verde/amarillo/rojo | `Color.kt` (`SuccessGreen`, `WarningYellow`, `DangerRed`) |

---

## 7. Ejemplo: cargar el presupuesto gerencial del enunciado

En **Gerencia → Presupuesto Sucursal** ingresá:

| Variable | Valor |
|----------|-------|
| Volumen | `287947479` |
| Crédito | `44112000` |
| Garantía | `14307000` |
| Crédito Efectivo | `12000000` |
| Celulares | `66` |

Definí **Días laborales = 22** y **Asesores = 6** (por ejemplo). En la pantalla de **Distribución** verás automáticamente:

- Por asesor (mes): Volumen ≈ $47.991.246, Crédito ≈ $7.352.000, etc.
- Por asesor (día): Volumen ≈ $2.181.420, Celulares ≈ 0,5/día.

Tocá **Compartir** / **WhatsApp** / **Email** para enviar el archivo a cada asesor.

---

## 8. Buenas prácticas aplicadas

1. **MVVM puro** — la UI nunca habla con Room directamente; pasa por el `Repository`.
2. **State holder único por pantalla** — un `ViewModel` por flujo (Asesor / Gerencia) y `data class` inmutable como UiState.
3. **Reactividad con Flow / StateFlow** — `combine` y `flatMapLatest` para recomputar al vuelo.
4. **Single Source of Truth** — Room es la única fuente de verdad; el estado local del Manager es UI-only y se persiste con `saveBranchBudget()`.
5. **Sin hardcoded strings de UI** — todos los textos visibles están en strings.xml o como literales en Compose (Spanish-AR), listos para localizar.
6. **Manejo defensivo de inputs** — `Formatters.toDouble` parsea con tolerancia a `,` y `.`.
7. **Permisos modernos** — `POST_NOTIFICATIONS` solicitado runtime para Android 13+.
8. **FileProvider seguro** — los archivos compartidos usan URIs `content://` con permisos efímeros.
9. **Compatibilidad amplia** — `minSdk 24` (Android 7) y soporte hasta API 35.
10. **Compose tooling** — preview-friendly, `collectAsStateWithLifecycle` para no consumir flow en background.

---

## 9. Posibles mejoras futuras

- Multi-asesor por usuario (usuario gerencial cambia entre vistas)
- Sincronización en la nube (Firebase / Supabase)
- Gráficos históricos (semana / mes / variable)
- Backup/restore manual del JSON completo
- Modo oscuro AMOLED
- Widget de inicio rápido para carga diaria
- Soporte multi-idioma (en, pt-BR)

---

## 10. Solución de problemas comunes

| Problema | Solución |
|----------|---------|
| Gradle sync falla | Ejecutar **File → Invalidate Caches and Restart** |
| `JAVA_HOME` no configurado | Usar el JDK embebido de Android Studio (Settings → Build Tools → Gradle → Gradle JDK) |
| No aparece notificación | Verificar permiso de notificaciones en Ajustes del sistema |
| WhatsApp no abre | Necesita WhatsApp instalado; si no, usar **Compartir** general |
| Importar archivo no funciona | Aceptar el permiso del FileProvider; archivo debe ser JSON o CSV válido |

---

¡Listo! Abrí el proyecto en Android Studio, esperá la sincronización, conectá tu celular y dale **Run** ▶️.
