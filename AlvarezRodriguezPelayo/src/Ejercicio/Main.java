package Ejercicio;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class Main {

	public static void main(String[] args) {

		// --- 1. Crear la GUI (JFrame, JPanel, JTextArea) ---
		JFrame frame = new JFrame("Log de Simulación de Ascensores");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(900, 700);

		// El JPanel principal
		JPanel panel = new JPanel(new BorderLayout());

		// El JTextArea donde irá el log
		JTextArea logTextArea = new JTextArea();
		logTextArea.setEditable(false); // No se puede escribir en él
		logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

		// Un JScrollPane para que el JTextArea sea scrollable
		JScrollPane scrollPane = new JScrollPane(logTextArea);

		// Añadimos el scrollPane (que contiene el JTextArea) al panel
		panel.add(scrollPane, BorderLayout.CENTER);

		// Añadimos el panel al frame
		frame.add(panel);

		// --- 2. Redirigir System.out al JTextArea ---
		// Creamos una instancia de nuestra clase auxiliar
		TextAreaOutputStream taOutputStream = new TextAreaOutputStream(logTextArea);
		PrintStream printStream = new PrintStream(taOutputStream, true); // true = autoFlush

		// ¡Aquí ocurre la magia!
		System.setOut(printStream);
		System.setErr(printStream); // Opcional: redirigir también los errores

		// --- 3. Mostrar la GUI ---
		// Hacemos visible la ventana
		frame.setVisible(true);

		// --- 4. Iniciar la simulación (código de antes) ---

		System.out.println(">>> Iniciando simulación... Salida redirigida a esta ventana.");

		ControladorEdificio controlador = new ControladorEdificio(2);

		// Iniciar Hilos de Ascensores
		ExecutorService poolAscensores = Executors.newFixedThreadPool(2);
		Ascensor ascensor0 = new Ascensor(0, controlador);
		Ascensor ascensor1 = new Ascensor(1, controlador);
		controlador.registrarAscensor(0, ascensor0);
		controlador.registrarAscensor(1, ascensor1);

		poolAscensores.submit(ascensor0);
		poolAscensores.submit(ascensor1);

		// Iniciar Hilos de Personas
		ExecutorService poolPersonas = Executors.newCachedThreadPool();
		for (int i = 0; i < ControladorEdificio.PERSONAS_TOTALES; i++) {
			poolPersonas.submit(new Persona(i + 1, controlador));
		}

		System.out.println(
				">>> Simulación iniciada con 2 ascensores y " + ControladorEdificio.PERSONAS_TOTALES + " personas.");

		// --- 5. Eliminar control por consola ---
		// El control por Scanner(System.in) ya no funcionará porque
		// la consola estándar ya no está conectada.
		// Si quisieras pausar/reanudar, tendrías que añadir
		// JButtons a la GUI que llamen a controlador.pausar() y controlador.reanudar().

		// (El hilo principal 'main' terminará aquí, pero la aplicación
		// seguirá viva gracias al hilo de la GUI y los hilos de los pools)
	}
}