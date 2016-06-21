package serverskiDeo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerNit extends Thread {

	ServerSocket serverSoket;
	int port;
	static int brojac = 0;

	public ServerNit(int port) {
		super();
		this.port = port;
	}

	@Override
	public void run() {
		
		try {
			
			serverSoket = new ServerSocket(port);
			Socket klijentSoket;
			
			while (true) {
				
				klijentSoket = serverSoket.accept();
				KlijentServerNit klijentServer = new KlijentServerNit(klijentSoket);
				klijentServer.start();
				
			}
			

		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

}
