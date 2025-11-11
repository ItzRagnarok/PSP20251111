package Ejercicio;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class Main {
	private JTextArea text = new JTextArea();
	private Ascensor ascensor;

	public void mostrarMensaje(String mensaje) {
		SwingUtilities.invokeLater(() -> text.append(mensaje + "\n"));
	}
	
	
	
	
}
