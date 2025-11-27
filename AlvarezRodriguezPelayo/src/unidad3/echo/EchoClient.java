package unidad3.echo;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class EchoClient {
	public static void main(String[] args) throws UnknownHostException, IOException {
		try (Socket socket = new Socket("10.140.43.200", 9001)) {
			
		}
	}
}
