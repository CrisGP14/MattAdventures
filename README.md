# MattAdventures

Aplicación Android desarrollada en Java para ofrecer una experiencia de juego educativa y divertida basada en matemáticas, niveles y retos interactivos.

## Descripción
MattAdventures es un proyecto de videojuego educativo para Android que combina lógica, preguntas, progresión por niveles y una interfaz visual atractiva. El objetivo principal es aprender mientras se juega, reforzando conceptos matemáticos a través de actividades y desafíos.

La versión actual del proyecto ha sido adaptada para compilar y ejecutarse en dispositivos modernos de Android, manteniendo la lógica y estructura original del juego, pero ajustando compatibilidad, rendimiento y soporte de versiones recientes del SDK.

## Características
- Juego educativo con niveles progresivos
- Preguntas y retos matemáticos
- Menú principal, niveles y pantallas de juego
- Base de datos SQLite para almacenamiento local
- Uso de recursos multimedia (sonidos e imágenes)
- Compatibilidad con Android moderno
- Estructura de proyecto desarrollada en Android Studio

## Tecnologías utilizadas
- Java
- Android SDK
- SQLite
- Gradle
- Android Studio

## Requisitos
- Android Studio (versión reciente)
- JDK 17
- Android SDK Platform 36
- Dispositivo físico o emulador Android

## Instalación y ejecución
1. Clona el repositorio:
   ```bash
   git clone https://github.com/CrisGP14/MattAdventures.git
   ```
2. Abre la carpeta en Android Studio.
3. Espera a que termine la sincronización de Gradle.
4. Si Android Studio solicita instalar componentes del SDK, aceptalos.
5. Conecta un dispositivo o inicia un emulador y ejecuta la aplicación.

### Compilar desde consola
En macOS/Linux:
```bash
./gradlew assembleDebug
```

En Windows:
```bash
gradlew.bat assembleDebug
```

El APK generado quedará en:
```bash
app/build/outputs/apk/debug/
```

## Estructura del proyecto
```text
MattAdventures/
├── app/
│   ├── src/
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/
├── build.gradle
├── gradlew
├── gradlew.bat
├── settings.gradle
├── gradle.properties
├── README.md
└── local.properties
```

## Componentes principales
- `MainActivity`: pantalla inicial / splash
- `Menu`: menú principal
- `MenuNiveles`: selección de niveles
- `Tablero`: lógica principal del juego
- `Actividades`: pantalla de preguntas y actividades
- `CargarDatos`: carga inicial de contenido
- `ComoJugar`: instrucciones
- `Credito`: créditos del proyecto
- `BdUsuarios`, `Bdpreguntas`, `EPreguntas`: manejo de la base de datos SQLite
- `BaseActivity`: base común para pantallas con configuración visual y control de música

## Actualizaciones realizadas
Se modernizó la aplicación para funcionar con versiones actuales de Android, incluyendo ajustes de Gradle, compatibilidad con SDK modernos, correcciones de comportamiento en nuevas versiones del sistema y mejoras en estabilidad y rendimiento.

## Créditos de audio
La música del proyecto usa contenido de Kevin MacLeod (incompetech.com), bajo licencia CC BY 3.0, incluidos temas como:
- Adventure Meme
- Amazing Grace 2011
- Ambler
- Breakdown

## Nota
Este proyecto fue desarrollado con fines educativos y de aprendizaje, especialmente enfocado en programación móvil con Android, lógica de juegos y manejo de interfaces de usuario.

## Licencia
Este proyecto no incluye una licencia específica en el repositorio. Si deseas usarlo con fines académicos o personales, puedes contactarme para acordar su uso.
