package serverskiDeo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import klijent.Klijent;

public class KlijentServerNit extends Thread {
	
	Socket soketZaKOmunikaciju;
	BufferedReader ulazniLinijskiTok;
	PrintStream izlazniLinjskiTok;
	

	public KlijentServerNit(Socket soketZaKOmunikaciju) {
		super();
		this.soketZaKOmunikaciju = soketZaKOmunikaciju;
	}
	
	@Override
	public void run() {
		ServerNit.brojac++;
		
		
		try {
			ulazniLinijskiTok = new BufferedReader(new InputStreamReader(soketZaKOmunikaciju.getInputStream()));
			izlazniLinjskiTok = new PrintStream(soketZaKOmunikaciju.getOutputStream());
			
			/*if(ServerNit.brojac > 4) {
				izlazniLinjskiTok.println("NE");
				ServerNit.brojac--;
				return;
			}*/
			
			izlazniLinjskiTok.println("DA");
			String nazivFajla = ulazniLinijskiTok.readLine();
			int redniBrojSegmenta = Integer.parseInt(ulazniLinijskiTok.readLine());
			
			String putanja = "";
			for (int i = 0; i < Klijent.listaFajlovaZaSeedovanje.size(); i++) { //pronalazenje fajla u listi fajlova koje seedujemo
				if(Klijent.listaFajlovaZaSeedovanje.get(i).endsWith(nazivFajla)) {
					putanja = Klijent.listaFajlovaZaSeedovanje.get(i);
					break;
				}
			}
			
			Path path = Paths.get(putanja); // kreiranje putanje
			byte[] data = Files.readAllBytes(path); // prevodjenje fajla u niz bajtova
			int velicinaZaSlanje = 0;
			if(redniBrojSegmenta*1300<data.length){
				velicinaZaSlanje=1300;
			}else{
				velicinaZaSlanje=data.length-((redniBrojSegmenta-1)*1300);
			}	
			izlazniLinjskiTok.println(velicinaZaSlanje);
			
			
	
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			OutputStream dOut = soketZaKOmunikaciju.getOutputStream();
			if (redniBrojSegmenta == 1 && data.length < 1300) {
				dOut.write(data, 0, data.length);
			} else if(redniBrojSegmenta == 1) {
				dOut.write(data, 0, 1300);
			} else if((redniBrojSegmenta * 1300) > data.length) {
				dOut.write(data, (redniBrojSegmenta -1)*1300,  data.length-((redniBrojSegmenta-1)*1300));
			} else {
				dOut.write(data, (redniBrojSegmenta -1)*1300, 1300);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		ServerNit.brojac--; //azurirati brojac
	}
	
}
