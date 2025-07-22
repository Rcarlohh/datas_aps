# Data Simulator - Simulador de Datos SQLite

Una aplicación Android que simula el envío de datos a una base de datos SQLite local con una interfaz oscura moderna y iconos coloridos.

## Características

- 🎨 **Interfaz Oscura Moderna**: Diseño elegante con tema oscuro
- 🌈 **Iconos Coloridos**: Diferentes colores para cada tipo de dato
- 📊 **Base de Datos SQLite**: Almacenamiento local de datos
- 📱 **Simulación de Datos**: Genera datos aleatorios de diferentes tipos
- 🔄 **Tiempo Real**: Actualización automática de la interfaz
- 📋 **Lista de Datos**: Visualización en tiempo real de todos los datos

## Tipos de Datos Simulados

- **Temperatura**: 15C - 35 (Icono rojo)
- **Humedad**: 30 - 90 (Icono azul)
- **Presión**:1000 - 1100hPa (Icono verde)
- **Velocidad**: 0 -120 km/h (Icono naranja)
- **Aceleración**: 0 - 100/s² (Icono púrpura)

## Funcionalidades

###1viar Datos
- Genera datos aleatorios de diferentes tipos
- Los almacena automáticamente en SQLite
- Muestra confirmación de envío exitoso

### 2. Ver Datos
- Muestra todos los datos almacenados
- Ordenados por timestamp (más recientes primero)
- Cada dato muestra tipo, valor y timestamp

### 3mpiar Datos
- Elimina todos los datos de la base de datos
- Confirma la acción con un mensaje

## Requisitos del Sistema

- Android 7.0 (API 24) o superior
- Android 12 compatible
- 50MB de espacio libre

## Instalación

1. Abre el proyecto en Android Studio
2. Sincroniza el proyecto con Gradle
3. Conecta tu dispositivo Android o usa un emulador4resiona "Run para instalar la aplicación

## Estructura del Proyecto

```
app/src/main/
├── java/com/example/datasimulator/
│   ├── MainActivity.kt          # Actividad principal
│   ├── DatabaseHelper.kt        # Manejo de SQLite
│   ├── DataItem.kt             # Modelo de datos
│   └── DataAdapter.kt          # Adaptador del RecyclerView
├── res/
│   ├── layout/
│   │   ├── activity_main.xml   # Layout principal
│   │   └── item_data.xml       # Layout de items
│   ├── values/
│   │   ├── colors.xml          # Colores del tema
│   │   ├── strings.xml         # Textos
│   │   └── themes.xml          # Tema de la app
│   └── drawable/
│       └── icon_background.xml # Fondo de iconos
└── AndroidManifest.xml         # Configuración de la app
```

## Tecnologías Utilizadas

- **Kotlin**: Lenguaje de programación principal
- **SQLite**: Base de datos local
- **RecyclerView**: Lista de datos
- **Material Design**: Componentes de UI
- **ViewBinding**: Binding de vistas
- **ConstraintLayout**: Layout principal

## Base de Datos

La aplicación utiliza SQLite con la siguiente estructura:

```sql
CREATE TABLE data_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,
    value REAL NOT NULL,
    timestamp INTEGER NOT NULL
);
```

## Personalización

Puedes personalizar la aplicación modificando:

- **Colores**: Edita `colors.xml`
- **Tipos de datos**: Modifica la lista en `MainActivity.kt`
- **Rangos de valores**: Ajusta los valores en `sendSimulatedData()`
- **Iconos**: Cambia los iconos en `DataAdapter.kt`

## Licencia

Este proyecto es de código abierto y está disponible bajo la licencia MIT. 