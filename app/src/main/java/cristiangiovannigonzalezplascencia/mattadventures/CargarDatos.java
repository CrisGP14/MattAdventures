package cristiangiovannigonzalezplascencia.mattadventures;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Carga las preguntas en la base de datos la primera vez que se juega y abre el juego. */
public class CargarDatos extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cargar_datos);

        SharedPreferences preferencia = getSharedPreferences("preferencia", Context.MODE_PRIVATE);
        boolean yaCargadas = preferencia.getBoolean("Cargando", false);

        if (yaCargadas)
        {
            abrirJuego();
        }
        else
        {
            // La inserción se hace fuera del hilo principal (antes se usaba AsyncTask, ya obsoleto)
            final Context app = getApplicationContext();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Bdpreguntas helper = new Bdpreguntas(app);
                    helper.insertarPreguntas(crearPreguntas());
                    app.getSharedPreferences("preferencia", Context.MODE_PRIVATE)
                            .edit().putBoolean("Cargando", true).apply();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing()) abrirJuego();
                        }
                    });
                }
            }).start();
        }
    }

    private void abrirJuego()
    {
        startActivity(new Intent(CargarDatos.this, Tablero.class));
        finish();
    }

    private static List<EPreguntas> crearPreguntas()
    {
        return new ArrayList<EPreguntas>(Arrays.asList(
                new EPreguntas("Si un granjero tiene 8 gallinas y adquirio 7 ¿Cuantas tiene ahora?","15","Para sumar de una manera mas facil recuerda comenzar a sumar los numeros de izquierda a derecha"),
                new EPreguntas("¿Cuanto habra que pagar si tienes que pagar 203 de una camisa y 453 de un pantalon?","656","Para hacer mas facil la suma intenalo sumando primero las decenas, luego las centenas y luego los millares"),
                new EPreguntas("¿A cuanto equivale una centena?","100","Vale 10 veces mas que una decena"),
                new EPreguntas("¿A cuanto equivale una decena?","10","Vale 10 veces mas que una unidad"),
                new EPreguntas("Natalia compro 30 sandias y se comio 6, cuantas le quedan?","24","Realiza una resta"),
                new EPreguntas("Si Gonzalo tiene que sacar 210 puntos para aprobar y saco 77 y 69, ¿cuantos puntos le faltan ?","64","Resta los 210 menos la suma de los 2 numeros"),
                new EPreguntas("¿A cuanto equivale un millar?","1000","Vale 10 veces mas que una centena"),
                new EPreguntas("Si Cynthia tiene $60 y quiere comprar paletas de $4, ¿para cuantas paletas le ajusta ?","15","Para realizarlo de una manera mas rapida, porque no intentas dividir"),
                new EPreguntas("¿Cual es la mitad de 2 mas 2?","2","Pregunta logica, estas no contienen ayuda"),
                new EPreguntas("Si tengo ciento cincuenta sillas y siento cincuenta cuyos, ¿cuantas sillas quedan libres?","100","Pregunta logica, estas no contienen ayuda"),
                new EPreguntas("Para construir un barco se utilizaron 1252 kg de madera y 230 kg de lamina, tornillos y clavos. Si el motor pesa 164 kg, ¿cual sera el peso total de la embarcación?","1646","Suma el peso de todos los elementos de la embarcación"),
                new EPreguntas("El producto de 3 numeros es 162. Si dos de ellos son 9 y 3, ¿Cual sera el otro numero?","6","Multiplica los dos numeros que ya tienes (9 x 3) y divide 162 entre ese resultado"),
                new EPreguntas("¿Cuanto es 2 centenas mas 3 decenas?","230","Recuerda, la centena vale 10 veces mas que una decena"),
                new EPreguntas("Un automovil recorre aproximadamente 7 km por cada litro de gasolina. ¿Cuantos kilimetros recorrera con 20 litros de gasolina?","140","Toma el numero de kilometros que recorre con 1 litro y multiplicalo por los litros de gasolina"),
                new EPreguntas("Ingresa el numero que falta para que sea correcta la siguiente operacion (8 x ? )/2+4-1=35","8","Usa la tabla del 8 para poder encontrar el numero faltante"),
                new EPreguntas("¿Cual seria el perimetro de un jardin de forma cuadrangular que mide 12 m por lado?","48","El perímetro de un cuadrado es igual a la suma de las longitudes de sus cuatro lados iguales."),
                new EPreguntas("¿Cual seria el perimetro de un triangulo equilatero que mide 6 m por lado?","18","El perímetro de un triangulo equilatero es igual a la suma de las longitudes de sus 3 lados iguales."),
                new EPreguntas("¿Cuanto es el perimetro de un terreno pentagonal que mide 19 cm de lado?","95"," El pentágono regular tiene sus cinco lados iguales, por lo que su perímetro es cinco veces uno de sus lados"),
                new EPreguntas("Una llave vierte 50 litros de agua por minuto. Si ya ha vertido 12 000 litros, ¿Cuantas horas ha estado abierta?","4","Primero resuelve cuantos litros de agua se vierten en una hora, teniendo eso divide los 12 000 l entre los litros por hora"),
                new EPreguntas("Si a Salvador entro al CETI en 2012 y este se graduara en 2017. ¿Cuantos años tardo en terminar el CETI?","5","Resta el año en que entro contra el año en que egresara"),
                new EPreguntas("Si cada caja de jabones contiene 20 jabones, ¿Cuantos jabones habra en 36 cajas?","720","Para realizar la operacion de manera mas rapida multiplica ambos numeros, recuera el orden de los factores no faceta el producto"),
                new EPreguntas("Se dividio un liston de 30 m en tiras de 15 cm. ¿Cuantas tiras de esa medida se obtuvieron?","200","Recuerda 1 m equivale a 100 cm"),
                new EPreguntas("¿A cuantos años equivale un centenario?","100","Es equivalente a una centena"),
                new EPreguntas("Si se pagaron $800 por 200 plumas iguales, ¿cuanto costo cada pluma?","4","¿Por que no intentas dividir :D?"),
                new EPreguntas("Fernando trabaja en una fabrica de camisas. Para cada camisa necesita 15 botones. ¿Cuantos botones necesita para 25 camisas?","375","Multiplica la cantidad de botones por la cantidad de camisas"),
                new EPreguntas("¿Cuantos minutos hay de diferencia entre las 2:30 y las 4:30?","120","Una hora equivale a 60 minutos"),
                new EPreguntas("¿Cuantos kilogramos tiene una tonelada (t)?","1000","Esta pregunta no contiene ayuda :D"),
                new EPreguntas("¿A cuantos años equivale un lustro?","5","Es la mitad de una decada"),
                new EPreguntas("¿A cuantos años equivale una decada","10","Es el doble de un lustro"),
                new EPreguntas("¿Cual es el área de un rectángulo de 10 cm de base y 6 cm de altura?","60","El área del rectángulo es igual a base por altura")
        ));
    }
}
