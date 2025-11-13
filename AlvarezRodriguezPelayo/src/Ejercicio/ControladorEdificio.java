package Ejercicio;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ControladorEdificio {

	public static final int PLANTAS = 21;
	public static final int PERSONAS_TOTALES = 100;

	private final Ascensor[] ascensores;

	// --- Gestión de Pausa/Reanudación ---
	private final ReentrantLock pausaLock = new ReentrantLock();
	private final Condition reanudarCond = pausaLock.newCondition();
	private boolean pausado = false;

	// --- Gestión de Llamadas Externas (Botones en cada piso) ---
	// Usamos Sets seguros para concurrencia para registrar qué pisos han llamado.
	private final Set<Integer> llamadasSubir = ConcurrentHashMap.newKeySet();
	private final Set<Integer> llamadasBajar = ConcurrentHashMap.newKeySet();

	// Lock y Condition para notificar a los ascensores IDLES que hay un nuevo
	// trabajo.
	private final ReentrantLock lockLlamadas = new ReentrantLock();
	private final Condition hayNuevasLlamadas = lockLlamadas.newCondition();

	// --- Gestión de Espera en Pisos ---
	// Un Lock y una Condition *por cada piso* donde las personas esperan.
	private final ReentrantLock[] locksPiso = new ReentrantLock[PLANTAS];
	private final Condition[] personasEsperandoEnPiso = new Condition[PLANTAS];

	public ControladorEdificio(int numAscensores) {
		this.ascensores = new Ascensor[numAscensores];
		for (int i = 0; i < PLANTAS; i++) {
			locksPiso[i] = new ReentrantLock();
			personasEsperandoEnPiso[i] = locksPiso[i].newCondition();
		}
	}

	public void registrarAscensor(int id, Ascensor ascensor) {
		this.ascensores[id] = ascensor;
	}

	// --- Lógica de Impresión Sincronizada ---
	// Sincronizado para evitar que los logs de diferentes hilos se mezclen.
	public synchronized void imprimirEstado(String mensaje) {
		System.out.println(mensaje);
	}

	// --- Lógica de Pausa ---
	public void pausar() {
		pausaLock.lock();
		try {
			pausado = true;
			imprimirEstado("--- SIMULACIÓN PAUSADA ---");
		} finally {
			pausaLock.unlock();
		}
	}

	public void reanudar() {
		pausaLock.lock();
		try {
			pausado = false;
			imprimirEstado("--- SIMULACIÓN REANUDADA ---");
			reanudarCond.signalAll(); // Despierta a TODOS los hilos (Personas y Ascensores)
		} finally {
			pausaLock.unlock();
		}
	}

	// Método que cada hilo debe llamar en su bucle principal
	public void comprobarPausa() throws InterruptedException {
		pausaLock.lock();
		try {
			while (pausado) {
				reanudarCond.await();
			}
		} finally {
			pausaLock.unlock();
		}
	}

	// Método público para que Ascensor notifique que hay nuevo trabajo (un destino
	// interno)
	public void notificarNuevoTrabajo() {
		lockLlamadas.lock();
		try {
			// Despierta a cualquier ascensor que esté en modo 'esperarNuevaLlamada'
			hayNuevasLlamadas.signalAll();
		} finally {
			lockLlamadas.unlock();
		}
	}

	// --- Lógica de Llamadas de Personas ---

	public Ascensor llamarYEsperarAscensor(String personaId, int piso, Direccion dir) throws InterruptedException {
		imprimirEstado(String.format(">> %s llama ascensor en piso %d para %s", personaId, piso, dir));

		// 1. Registrar la llamada
		hacerLlamada(piso, dir);

		// 2. Esperar en el piso
		locksPiso[piso].lock();
		try {
			Ascensor ascensorAsignado = null;
			while (ascensorAsignado == null) {
				// Comprueba si hay un ascensor válido AHORA MISMO
				ascensorAsignado = buscarAscensorEnPiso(piso, dir);

				if (ascensorAsignado == null) {
					// Si no, espera a que un ascensor le avise (signalAll)
					personasEsperandoEnPiso[piso].await();
				}
			}
			// Encontró un ascensor y tiene sitio
			imprimirEstado(String.format(">> %s entra en ascensor %d en piso %d", personaId,
					ascensorAsignado.getIdAscensor(), piso));
			return ascensorAsignado;
		} finally {
			locksPiso[piso].unlock();
		}
	}

	private void hacerLlamada(int piso, Direccion dir) {
		if (dir == Direccion.SUBIENDO) {
			llamadasSubir.add(piso);
		} else {
			llamadasBajar.add(piso);
		}

		// Notificar a los ascensores parados que hay trabajo
		lockLlamadas.lock();
		try {
			hayNuevasLlamadas.signalAll();
		} finally {
			lockLlamadas.unlock();
		}
	}

	// Llamado por el Ascensor cuando atiende una llamada
	public void atenderLlamada(int piso, Direccion dir) {
		if (dir == Direccion.SUBIENDO) {
			llamadasSubir.remove(piso);
		} else {
			llamadasBajar.remove(piso);
		}
	}

	// Un ascensor IDLE espera aquí hasta que alguien llame
	public void esperarNuevaLlamada() throws InterruptedException {
		lockLlamadas.lock();
		try {
			while (llamadasSubir.isEmpty() && llamadasBajar.isEmpty()) {
				hayNuevasLlamadas.await();
			}
		} finally {
			lockLlamadas.unlock();
		}
	}

	// Llamado por la Persona para ver si un ascensor ha llegado
	private Ascensor buscarAscensorEnPiso(int piso, Direccion dir) {
		for (Ascensor ascensor : ascensores) {
			if (ascensor.getPisoActual() == piso && ascensor.getSentido() == dir) {
				// ¡El ascensor está aquí y va en mi dirección!
				// Intenta "coger" un sitio. Si tryAcquire() falla, el ascensor está lleno.
				if (ascensor.getCapacidad().tryAcquire()) {
					return ascensor; // Éxito
				}
			}
		}
		return null; // No hay ascensor válido en este momento
	}

	// Llamado por el Ascensor cuando llega a un piso para recoger gente
	public void notificarLlegadaAscensor(int piso) {
		locksPiso[piso].lock();
		try {
			// Despierta a TODAS las personas que esperan en ese piso
			personasEsperandoEnPiso[piso].signalAll();
		} finally {
			locksPiso[piso].unlock();
		}
	}

	// --- Getters de estado para el Ascensor ---
	public Set<Integer> getLlamadasSubir() {
		return llamadasSubir;
	}

	public Set<Integer> getLlamadasBajar() {
		return llamadasBajar;
	}

	public boolean hayLlamadas() {
		return !llamadasSubir.isEmpty() || !llamadasBajar.isEmpty();
	}
}
