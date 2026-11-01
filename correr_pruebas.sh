#!/bin/sh
# Simulaciones del motor del juego y de las preguntas. Solo necesita un JDK (no Android).
# Uso:  sh correr_pruebas.sh
# Comprueba: subidas de velocidad (30/60/90 s, maximo 3), que cada obstaculo se pueda saltar
# en todas las velocidades, que los huecos entre obstaculos sean justos y las reglas de vidas.
SRC=app/src/main/java/cristiangiovannigonzalezplascencia/mattadventures
OUT=/tmp/mattadventures_pruebas
rm -rf $OUT && mkdir -p $OUT || exit 1
javac -nowarn -d $OUT $SRC/MotorJuego.java $SRC/EPreguntas.java $SRC/PreguntaJuego.java $SRC/BolsaPreguntas.java pruebas/*.java || exit 1
java -cp $OUT cristiangiovannigonzalezplascencia.mattadventures.TestMotor || exit 1
java -cp $OUT cristiangiovannigonzalezplascencia.mattadventures.TestPreguntas || exit 1
java -cp $OUT cristiangiovannigonzalezplascencia.mattadventures.TestDemo || exit 1
echo "TODO OK"
