package unidad3.echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class EchoClient {
	public static void main(String[] args) throws UnknownHostException, IOException {

		try (Socket socket = new Socket("10.140.43.200", 9001)) {
			// Preparamos la salida con 'true' para enviar los datos inmediatamente (autoFlush)
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			// Obtenemos la IP local que tiene asignada este cliente
			String miIp = socket.getLocalAddress().getHostAddress();
			
			out.println("IP: " + miIp);
			System.out.println("Se ha enviado la IP (" + miIp + ") al servidor correctamente.");
		}
	}
}