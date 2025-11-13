package Ejercicio;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Ascensor implements Runnable {

	private final int id;
	private final ControladorEdificio controlador;

	// --- Estado Propio del Ascensor ---
	private int pisoActual = 0; // Empieza en la planta baja
	private Direccion sentido = Direccion.PARADO;
	private final Semaphore capacidad = new Semaphore(8);

	// Destinos seleccionados por la gente DENTRO del ascensor
	// Usamos TreeSet para que estén ordenados automáticamente
	private final Set<Integer> destinosInternos = new TreeSet<>();

	// --- Sincronización Interna (para gente DENTRO) ---
	// Lock y Conditions para que las personas DENTRO esperen su piso de destino
	private final ReentrantLock lockParadasInternas = new ReentrantLock();
	private final Condition[] paradasInternas = new Condition[ControladorEdificio.PLANTAS];

	public Ascensor(int id, ControladorEdificio controlador) {
		this.id = id;
		this.controlador = controlador;
		for (int i = 0; i < ControladorEdificio.PLANTAS; i++) {
			paradasInternas[i] = lockParadasInternas.newCondition();
		}
	}

	// --- Métodos llamados por la Persona ---

	public void seleccionarDestino(int pisoDestino) {
		synchronized (this) {
			destinosInternos.add(pisoDestino);
		}

		// Si el ascensor estaba PARADO, este nuevo destino lo "despierta"
		// ¡ARREGLADO! Llama al método público del controlador.
		controlador.notificarNuevoTrabajo();
	}

	public void esperarLlegadaADestino(int pisoDestino) throws InterruptedException {
		lockParadasInternas.lock();
		try {
			// Espera hasta que el ascensor llegue al piso Y avise (signalAll)
			while (this.pisoActual != pisoDestino) {
				paradasInternas[pisoDestino].await();
			}
		} finally {
			lockParadasInternas.unlock();
		}
	}

	// --- Lógica Principal del Hilo Ascensor ---

	@Override
	public void run() {
		try {
			while (true) {
				// 1. Comprobar si la simulación está pausada
				controlador.comprobarPausa();

				// 2. Lógica de parada: Bajar y Subir personas
				// Estas acciones deben ocurrir ANTES de decidir el próximo movimiento
				bajarPersonas();
				subirPersonas();

				// 3. Decidir el próximo estado (moverse o pararse)
				// Usamos synchronized(this) para proteger el acceso a 'sentido', 'pisoActual' y
				// 'destinosInternos'
				synchronized (this) {
					decidirProximoMovimiento();
				}

				// 4. Actuar (moverse o esperar)
				if (sentido != Direccion.PARADO) {
					simularMovimiento();
				} else {
					// Si está parado, espera por una nueva llamada (interna o externa)
					controlador.imprimirEstado(
							String.format("Ascensor %d PARADO en piso %d. Esperando llamadas.", id, pisoActual));
					controlador.esperarNuevaLlamada();
				}
			}
		} catch (InterruptedException e) {
			System.out.println("Ascensor " + id + " interrumpido.");
		}
	}

	private void bajarPersonas() {
		if (destinosInternos.contains(pisoActual)) {
			lockParadasInternas.lock();
			try {
				// Avisa a todas las personas DENTRO que querían este piso
				paradasInternas[pisoActual].signalAll();
				controlador.imprimirEstado(String.format("Ascensor %d deja pasajeros en piso %d", id, pisoActual));
			} finally {
				lockParadasInternas.unlock();
			}
			destinosInternos.remove(pisoActual);
		}
	}

	private void subirPersonas() {
		boolean hayLlamadaSubir = controlador.getLlamadasSubir().contains(pisoActual);
		boolean hayLlamadaBajar = controlador.getLlamadasBajar().contains(pisoActual);

		if ((sentido == Direccion.SUBIENDO && hayLlamadaSubir) || (sentido == Direccion.BAJANDO && hayLlamadaBajar)
				|| (sentido == Direccion.PARADO && (hayLlamadaSubir || hayLlamadaBajar))) {

			// Si estaba parado y recoge gente, define su sentido
			if (sentido == Direccion.PARADO) {
				sentido = hayLlamadaSubir ? Direccion.SUBIENDO : Direccion.BAJANDO;
			}

			controlador.imprimirEstado(
					String.format("Ascensor %d para en %d a recoger gente (%s)", id, pisoActual, sentido));

			// Atiende la llamada (apaga el "botón" de llamada)
			controlador.atenderLlamada(pisoActual, sentido);

			// Despierta a las personas que esperan FUERA en este piso
			controlador.notificarLlegadaAscensor(pisoActual);
		}
	}

	private void simularMovimiento() throws InterruptedException {
		// Simula el tiempo que tarda en moverse entre pisos
		TimeUnit.MILLISECONDS.sleep(500);

		synchronized (this) {
			pisoActual += (sentido == Direccion.SUBIENDO) ? 1 : -1;
			controlador
					.imprimirEstado(String.format("... Ascensor %d llega a piso %d (%s) ...", id, pisoActual, sentido));
		}
	}

	// La "IA" del ascensor. Protegida por synchronized(this)
	private void decidirProximoMovimiento() {
		if (sentido == Direccion.PARADO) {
			// Si estaba parado, busca CUALQUIER trabajo
			if (!destinosInternos.isEmpty()) {
				// Prioridad 1: Destinos internos
				int proximoDestino = destinosInternos.iterator().next();
				sentido = (proximoDestino > pisoActual) ? Direccion.SUBIENDO : Direccion.BAJANDO;
			} else if (!controlador.getLlamadasSubir().isEmpty()) {
				// Prioridad 2: Llamadas de subir
				int proximaLlamada = controlador.getLlamadasSubir().iterator().next();
				sentido = (proximaLlamada > pisoActual) ? Direccion.SUBIENDO : Direccion.BAJANDO;
			} else if (!controlador.getLlamadasBajar().isEmpty()) {
				// Prioridad 3: Llamadas de bajar
				int proximaLlamada = controlador.getLlamadasBajar().iterator().next();
				sentido = (proximaLlamada > pisoActual) ? Direccion.SUBIENDO : Direccion.BAJANDO;
			} else {
				sentido = Direccion.PARADO; // Sigue parado, no hay trabajo
			}
			return;
		}

		// Si ya estaba en movimiento (SUBIENDO)
		if (sentido == Direccion.SUBIENDO) {
			// 1. Hay destinos internos por encima?
			Integer proximoDestino = destinosInternos.stream().filter(p -> p > pisoActual).findFirst().orElse(null);
			if (proximoDestino != null)
				return; // Sigue subiendo

			// 2. Hay llamadas externas por encima?
			Integer proximaLlamada = controlador.getLlamadasSubir().stream().filter(p -> p > pisoActual).findFirst()
					.orElse(null);
			if (proximaLlamada != null)
				return; // Sigue subiendo

			// 3. No hay más trabajo "arriba". Busca trabajo "abajo" o para.
			if (!destinosInternos.isEmpty() || controlador.hayLlamadas()) {
				sentido = Direccion.BAJANDO; // Cambia de sentido
			} else {
				sentido = Direccion.PARADO;
			}
		}

		// Si ya estaba en movimiento (BAJANDO)
		if (sentido == Direccion.BAJANDO) {
			// 1. Hay destinos internos por debajo?
			Integer proximoDestino = destinosInternos.stream().filter(p -> p < pisoActual).findFirst().orElse(null);
			if (proximoDestino != null)
				return; // Sigue bajando

			// 2. Hay llamadas externas por debajo?
			Integer proximaLlamada = controlador.getLlamadasBajar().stream().filter(p -> p < pisoActual).findFirst()
					.orElse(null);
			if (proximaLlamada != null)
				return; // Sigue bajando

			// 3. No hay más trabajo "abajo". Busca trabajo "arriba" o para.
			if (!destinosInternos.isEmpty() || controlador.hayLlamadas()) {
				sentido = Direccion.SUBIENDO; // Cambia de sentido
			} else {
				sentido = Direccion.PARADO;
			}
		}
	}

	// --- Getters para estado ---
	public int getIdAscensor() {
		return id;
	}

	public synchronized int getPisoActual() {
		return pisoActual;
	}

	public synchronized Direccion getSentido() {
		return sentido;
	}

	public Semaphore getCapacidad() {
		return capacidad;
	}
}