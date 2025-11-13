package Ejercicio;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Persona implements Runnable {

	private final String id;
	private final ControladorEdificio controlador;
	private final int pisoOrigen;
	private final int pisoDestino;
	private final Direccion direccion;
	private static final Random rand = new Random();

	public Persona(int idNum, ControladorEdificio controlador) {
		this.id = String.format("P%03d", idNum);
		this.controlador = controlador;

		// Generar origen y destino aleatorios (distintos)
		this.pisoOrigen = rand.nextInt(ControladorEdificio.PLANTAS);
		int dest;
		do {
			dest = rand.nextInt(ControladorEdificio.PLANTAS);
		} while (dest == this.pisoOrigen);
		this.pisoDestino = dest;

		this.direccion = (pisoDestino > pisoOrigen) ? Direccion.SUBIENDO : Direccion.BAJANDO;
	}

	@Override
	public void run() {
		try {
			// 1. Llegada escalonada (0.5 a 2 segundos)
			int retraso = 500 + rand.nextInt(1501);
			TimeUnit.MILLISECONDS.sleep(retraso);

			// 2. Comprobar pausa
			controlador.comprobarPausa();

			// 3. Llamar al ascensor y ESPERAR a que llegue y tenga sitio
			Ascensor ascensor = controlador.llamarYEsperarAscensor(id, pisoOrigen, direccion);

			// 4. Subir al ascensor y seleccionar destino
			ascensor.seleccionarDestino(pisoDestino);
			controlador.imprimirEstado(String.format(">>>> %s (piso %d) selecciona destino %d en ascensor %d", id,
					pisoOrigen, pisoDestino, ascensor.getIdAscensor()));

			// 5. Esperar DENTRO del ascensor hasta llegar al destino
			ascensor.esperarLlegadaADestino(pisoDestino);

			// 6. Bajar del ascensor (liberar capacidad)
			ascensor.getCapacidad().release();
			controlador.imprimirEstado(
					String.format("<<<< %s baja del ascensor en piso %d. (Destino cumplido)", id, pisoDestino));

		} catch (InterruptedException e) {
			System.out.println("Persona " + id + " interrumpida.");
		}
	}
}
