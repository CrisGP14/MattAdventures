# Matt Adventures (versión actualizada)

Juego educativo de matemáticas para Android (Java). Esta versión es el proyecto original de 2016,
migrado para compilar y ejecutarse en teléfonos actuales (Android 5.0 a Android 16).

## Cómo compilarlo
1. Instala **Android Studio** reciente (Narwhal o más nuevo). Trae el JDK que necesita.
2. `File > Open` y elige esta carpeta. Espera a que termine el *Sync* (descarga Gradle 8.13 y el plugin 8.13.0).
   Si Studio pide instalar "Android SDK Platform 36", acepta.
3. Conecta el teléfono (depuración USB activada) o crea un emulador y pulsa **Run**.

APK por consola: `./gradlew assembleDebug` (en Windows `gradlew.bat assembleDebug`).
El APK queda en `app/build/outputs/apk/debug/`.

## Qué cambió respecto al original
| Antes | Ahora |
|---|---|
| Android Gradle Plugin 3.0 / Gradle 4.1 | AGP 8.13.0 / Gradle 8.13 / JDK 17 |
| compileSdk 23, targetSdk 23 | compileSdk 36, targetSdk 36 (lo que exige Google Play) |
| `jcenter()`, `compile`, appcompat, appindexing | `google()` + `mavenCentral()`, `implementation`, **sin dependencias** |
| Manifest con `package`, sin `exported` | `namespace` en Gradle, `exported` declarado, `supportsRtl=false` |
| `getActionBar().hide()` (fallo al iniciar) | Se quita; pantalla completa inmersiva con la API actual |
| `onBackPressed()` (Android 16 ya no lo llama) | `OnBackInvokedCallback` en `BaseActivity` |
| Hilos moviendo vistas / `Thread.sleep(70)` en la UI | `Handler` en el hilo principal y control de tiempo entre pasos |
| `AsyncTask` + `ProgressDialog` | Hilo simple + transacción SQLite |
| SQL con texto concatenado | Parámetros / `ContentValues` |
| Música WAV (19 MB), nunca liberada | OGG (1.8 MB), pausa/reanuda y se libera sola |
| Sensor de proximidad contaba "lejos" como "cerca" en muchos teléfonos | Compara con el alcance máximo del sensor |
| Acelerómetro solo funcionaba en una orientación | Se adapta a la rotación de la pantalla |
| Pregunta "producto de 3 números es 162" con respuesta 150 | Respuesta correcta: 6 |

Las pantallas, niveles, preguntas, puntajes y la lógica del juego se conservan.

## Estructura
- `BaseActivity`: pantalla completa, botón atrás, medidas de pantalla y música (todas las pantallas la extienden).
- `MainActivity` splash · `Menu` · `MenuNiveles` · `Tablero` (juego) · `Actividades` (preguntas) ·
  `CargarDatos` · `ComoJugar` · `Credito`
- `Bdpreguntas`, `BdUsuarios`, `EPreguntas`: base de datos SQLite.

## Créditos de audio
Música de Kevin MacLeod (incompetech.com), licencia CC BY 3.0: Adventure Meme, Amazing Grace 2011, Ambler, Breakdown.
