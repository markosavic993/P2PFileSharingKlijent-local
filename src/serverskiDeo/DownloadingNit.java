package serverskiDeo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DownloadingNit implements Callable<byte[]> {

	int redniBrojSegmenta;
	String[] ipAdrese;
	String nazivFajla;

	public DownloadingNit(int redniBrojSegmenta, String[] ipAdrese, String nazivFajla) {
		super();
		this.redniBrojSegmenta = redniBrojSegmenta;
		this.ipAdrese = ipAdrese;
		this.nazivFajla = nazivFajla;
	}

	@Override
	public byte[] call() throws Exception {
		int brojac;
		
		//postavljanje vrednosti brojaca u zavisnosti od broja segmenta
		if(redniBrojSegmenta < ipAdrese.length) {
			brojac = redniBrojSegmenta - 1;
		} else {
			if(redniBrojSegmenta % ipAdrese.length == 0) {
				brojac = ipAdrese.length - 1;
			} else {
				brojac = redniBrojSegmenta % ipAdrese.length - 1;
			}
		}
		
		while(brojac < ipAdrese.length){
			ExecutorService service = Executors.newSingleThreadExecutor();
			//brojac mora biti u opsegu ip adresa
			if(ipAdrese[brojac] == null) {
				brojac = 0;
			}
			String ipA = ipAdrese[brojac];
			
			
			// try {
			Callable<byte[]> c = new Callable<byte[]>() {
				@Override
				public byte[] call() throws IOException {
					
					Socket soketZaKomunikaciju = new Socket(ipA, 7878);
					// ulaz i izlaz za linijski tekst
					BufferedReader ulazZaLinijskiTekst = new BufferedReader(
							new InputStreamReader(soketZaKomunikaciju.getInputStream()));
					PrintStream izlazZaLinijskiTekst = new PrintStream(soketZaKomunikaciju.getOutputStream());
					InputStream dIn = soketZaKomunikaciju.getInputStream();
					
					String potvrda = ulazZaLinijskiTekst.readLine(); // da li je prihvacen zahtev od strane drugog klijenta

					if (potvrda.equals("DA")) {
						izlazZaLinijskiTekst.println(nazivFajla);
						izlazZaLinijskiTekst.println(redniBrojSegmenta);
						int velicina = Integer.parseInt(ulazZaLinijskiTekst.readLine());

						byte[] message = new byte[velicina];
						dIn.read(message, 0, velicina); // read the message
						
						soketZaKomunikaciju.close();
						return message;
				
					}
					soketZaKomunikaciju.close();
					return null;
				}
			};

			Future<?> f = service.submit(c);

			byte[] izlaz;
			try {
				izlaz = (byte[]) f.get();
				if (izlaz != null) {
					return izlaz;
				}
			} catch (Exception e) {
			//	System.out.println("prelazi se na naredni");
			//	e.printStackTrace();
			} 
			service.shutdown();
			brojac++;
		
		}
		return null;
	}

}
