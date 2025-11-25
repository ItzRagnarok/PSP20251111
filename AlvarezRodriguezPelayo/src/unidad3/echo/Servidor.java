package unidad3.echo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Servidor {
	
	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = new ServerSocket(9001);
		ExecutorService service = Executors.newFixedThreadPool(20);
		while (true) {
			Socket socket = serverSocket.accept(); //el metodo accept se queda esperando a una peticion de conexión
			//Si retorna un cliente se ha conectado
			
			service.submit(new Servicio(socket)::run);
		}
		
		
		
	}

}
