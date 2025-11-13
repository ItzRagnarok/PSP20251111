package Ejercicio;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class TextAreaOutputStream extends OutputStream {

	private final JTextArea textArea;
	private final StringBuilder sb = new StringBuilder();

	public TextAreaOutputStream(final JTextArea textArea) {
		this.textArea = textArea;
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() {
	}

	@Override
	public void write(int b) throws IOException {
		// Ignora retornos de carro
		if (b == '\r')
			return;

		// Si es un salto de línea, añade el texto acumulado al JTextArea
		if (b == '\n') {
			final String text = sb.toString() + "\n";

			// IMPORTANTE: Actualiza la GUI en el Hilo de Despacho de Eventos (EDT)
			SwingUtilities.invokeLater(() -> {
				textArea.append(text);
				// Mueve el cursor al final para auto-scroll
				textArea.setCaretPosition(textArea.getDocument().getLength());
			});
			sb.setLength(0); // Limpia el buffer
		} else {
			// Acumula caracteres
			sb.append((char) b);
		}
	}
}
