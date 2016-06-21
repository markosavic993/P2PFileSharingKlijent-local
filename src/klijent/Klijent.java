package klijent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import serverskiDeo.DownloadingNit;
import serverskiDeo.ServerNit;

public class Klijent {

	static Socket soketZaCentralniServer = null;
	static BufferedReader ulazniTokOdCentralnogServera = null;
	static PrintStream izlazniTokKaCentralnomServeru = null;
	static BufferedReader ulazKonzola = null;
	public static LinkedList<String> listaFajlovaZaSeedovanje = new LinkedList<String>();
	
	//metoda za zaokruzivanje, koristimo kod prikaza napredka download-a
	public static double round(double value,int places){
		if(places<0){
			throw new IllegalArgumentException();
		}
		long factor = (long)Math.pow(10, places);
		value = value*factor;
		long tmp = Math.round(value);
		return (double)tmp/factor;
	}

	public static void main(String[] args) throws InterruptedException, ExecutionException {

		try {
			//povezivanje na centralni servis
			soketZaCentralniServer = new Socket("127.0.0.1", 9876);
			ulazKonzola = new BufferedReader(new InputStreamReader(System.in));
			
			//pokretanje serverske niti
			ServerNit server = new ServerNit(7878);
			server.start();

			ulazniTokOdCentralnogServera = new BufferedReader(
					new InputStreamReader(soketZaCentralniServer.getInputStream()));
			izlazniTokKaCentralnomServeru = new PrintStream(soketZaCentralniServer.getOutputStream());

			System.out.println(ulazniTokOdCentralnogServera.readLine());

			String osnovniPodaci = "";
			String nasaTrenutnaIPAdresa = ((InetSocketAddress) soketZaCentralniServer.getLocalSocketAddress())
					.getHostString();
			File file = new File("src/klijent/adrese.txt"); //za cuvanje prethodno koriscene ip adrese
			BufferedReader citac = new BufferedReader(new FileReader(file.getAbsolutePath())); //za proveru trenutne ip adrese u odnosu na proslu
			PrintWriter fajlUpisivanje = new PrintWriter(new FileWriter(file.getAbsolutePath()));
			
			//azuriranje ip adrese
			if (file.length() == 0) { // prazan
				osnovniPodaci = "nov" + "#" + nasaTrenutnaIPAdresa;
				fajlUpisivanje.println(nasaTrenutnaIPAdresa);
				fajlUpisivanje.close();

			} else {
				String proslaAdresa = citac.readLine();
				osnovniPodaci = proslaAdresa + "#" + nasaTrenutnaIPAdresa;
				fajlUpisivanje.print(nasaTrenutnaIPAdresa);
				fajlUpisivanje.close();
			}

			izlazniTokKaCentralnomServeru.println(osnovniPodaci);

			citac.close();
			System.out.println(ulazniTokOdCentralnogServera.readLine()); // da li zelite da seedujete
																			 
			String daLiSeedujemo = ulazKonzola.readLine();
			
			// provera unosa 
			while (true) {
				if (daLiSeedujemo.toUpperCase().equals("DA") || daLiSeedujemo.toUpperCase().equals("NE")) {
					izlazniTokKaCentralnomServeru.println(daLiSeedujemo);
					break;
				} else {
					System.out.println("Nepravilan unos: unesite DA ili NE!");
					daLiSeedujemo = ulazKonzola.readLine();
				}
			}

			if (daLiSeedujemo.toUpperCase().equals("DA")) {

				boolean daLiJeKraj = false;
				while (!daLiJeKraj) {
					System.out.println(ulazniTokOdCentralnogServera.readLine()); // unesite putanju
																				 
					while (true) {
						String putanja = ulazKonzola.readLine();
						listaFajlovaZaSeedovanje.add(putanja);
						File fajlZaSeed = new File(putanja);
						
						//provera postojanja fajla
						if (fajlZaSeed.exists()) {
							String nazivFajla = fajlZaSeed.getName();
							long velicinaFajla = fajlZaSeed.length();
							String checksum = "";
							try {
								checksum = MD5Checksum.getMD5Checksum(fajlZaSeed);

							} catch (Exception e) {
								
								e.printStackTrace();
							}

							System.out.println("Da li zelite da seed-ujete jos neki fajl?");

							String nastavakSedovanja = ulazKonzola.readLine();
							
							//provera unosa
							while (true) {
								if (nastavakSedovanja.toUpperCase().equals("DA")
										|| nastavakSedovanja.toUpperCase().equals("NE")) {
									break;
								} else {
									System.out.println("Nepravilan unos: unesite DA ili NE!");
									nastavakSedovanja = ulazKonzola.readLine();
								}
							}
							if (nastavakSedovanja.toUpperCase().equals("NE")) {
								daLiJeKraj = true;
							}

							izlazniTokKaCentralnomServeru.println(
									nazivFajla + "#" + velicinaFajla + "#" + checksum + "#" + nastavakSedovanja);
							break;

						} else {
							System.out.println("Nepravilan unos, ponovo unesite putanju!");
						}
					}

				}

			}

			boolean kraj = false;
			while (!kraj) {
				System.out.println(ulazniTokOdCentralnogServera.readLine()); //unesite naziv fajla za download
				String unos = ulazKonzola.readLine();
				izlazniTokKaCentralnomServeru.println(unos);
				//izlaz iz programa
				if (unos.equals("/quit")) {
					System.out.println(ulazniTokOdCentralnogServera.readLine());
					soketZaCentralniServer.close();
					citac.close();
					fajlUpisivanje.close();
					return;
				}
				String promenljiva = ulazniTokOdCentralnogServera.readLine();
				if(promenljiva.equals("NE")) {
					System.out.println("Trazeni fajl ne postoji!");
					continue;
				}

				System.out.println("Izaberite redni broj fajla koji zelite da download-ujete!");
				String[] odgovarajuciFajlovi = promenljiva.split("\\#");
				String nazivFajlaZaDownload = "";
				//prikaz odgovarajucih fajlova
				for (int i = 0; i < odgovarajuciFajlovi.length; i++) {
					String[] podaciOFajlu = odgovarajuciFajlovi[i].split("/");
					System.out.println(podaciOFajlu[0] + "	" + podaciOFajlu[1] + "	" + podaciOFajlu[2] + "	"
							+ podaciOFajlu[3] + "\n");
				}
				
				//TODO mora se obezbediti unos odgovarajuceg broja
				String klijentovIzbor = ulazKonzola.readLine();
				for (int i = 0; i < odgovarajuciFajlovi.length; i++) {
					String[] podaciOFajlu = odgovarajuciFajlovi[i].split("/");
					if(klijentovIzbor.equals(podaciOFajlu[0])) {
						nazivFajlaZaDownload = podaciOFajlu[1];
					}
				}

				izlazniTokKaCentralnomServeru.println(klijentovIzbor); // klijentov izbor r.br.
																				
				String spisakSegmenataIAdresa = ulazniTokOdCentralnogServera.readLine();
				String[] ipAdresePoSegmentima = spisakSegmenataIAdresa.split("\\#");
				FileOutputStream output = new FileOutputStream("D://" + nazivFajlaZaDownload, true);
				
				System.out.println("Zapoceto preuzimanje...");
				int downloadovaniSegmenti = 0;
				for (int i = 0; i < ipAdresePoSegmentima.length;) {
					
					//obrada preuzimanja i pokretanja downloading niti u zavisnosti od toga koliko ce se segmenata preuzimati odjednom
					if((ipAdresePoSegmentima.length - downloadovaniSegmenti) % 4 == 0 || ipAdresePoSegmentima.length - downloadovaniSegmenti > 4) {
						
						String[] prvaNit = ipAdresePoSegmentima[i].split(":"); //brojseg ipa1/ipa2
						String[] drugaNit = ipAdresePoSegmentima[i + 1].split(":"); 
						String[] trecaNit = ipAdresePoSegmentima[i + 2].split(":"); 
						String[] cetvrtaNit = ipAdresePoSegmentima[i + 3].split(":");

						byte[] glavniBafer = new byte[4*1300];

						ExecutorService service = Executors.newSingleThreadExecutor();

						DownloadingNit nit1 = new DownloadingNit(Integer.parseInt(prvaNit[0]), prvaNit[1].split("/"), nazivFajlaZaDownload);
						DownloadingNit nit2 = new DownloadingNit(Integer.parseInt(drugaNit[0]), drugaNit[1].split("/"), nazivFajlaZaDownload);
						DownloadingNit nit3 = new DownloadingNit(Integer.parseInt(trecaNit[0]), trecaNit[1].split("/"), nazivFajlaZaDownload);
						DownloadingNit nit4 = new DownloadingNit(Integer.parseInt(cetvrtaNit[0]), cetvrtaNit[1].split("/"), nazivFajlaZaDownload);

						Future<?> f1 = service.submit(nit1);
						Future<?> f2 = service.submit(nit2);
						Future<?> f3 = service.submit(nit3);
						Future<?> f4 = service.submit(nit4);


						while(true) {
							int manje = -1;
							if(f1.isDone()) {
								//upisati na odgovarajuce mesto u glavnom baferu
								byte[] bafer = (byte[]) f1.get();
								for (int j = 0; j < bafer.length; j++) {
									glavniBafer[j] = bafer[j];
								}
							}
							
							if(f2.isDone()) {
								byte[] bafer = (byte[]) f2.get();
								for (int j = 0; j < bafer.length; j++) {
									glavniBafer[j+1300] = bafer[j];
								}
							}
							
							if(f3.isDone()) {
								byte[] bafer = (byte[]) f3.get();
								for (int j = 0; j < bafer.length; j++) {
									glavniBafer[j + 2600] = bafer[j];
								}
							}
							
							if(f4.isDone()) {
								byte[] bafer = (byte[]) f4.get();
								for (int j = 0; j < bafer.length; j++) {
									glavniBafer[j + 3900] = bafer[j];
								}
								if(bafer.length<1300){
								manje = bafer.length;	
								}
							}
							
							if(f1.isDone() && f2.isDone() && 
									f3.isDone() && f4.isDone()) {
								if(manje!=-1){
									byte[] noviBafer = new byte[3900+manje];
									for (int j = 0; j < noviBafer.length; j++) {
										noviBafer[j]=glavniBafer[j];
									}
									output.write(noviBafer);
								}else{
								output.write(glavniBafer);}
								downloadovaniSegmenti+=4;
								double procenat = ((double)downloadovaniSegmenti/ipAdresePoSegmentima.length)*100;
								System.out.println(round(procenat, 2)+" %");
								i+=4;
								break;
							}
						}
						
					}
					
					if((ipAdresePoSegmentima.length - downloadovaniSegmenti) % 4 == 3) {
						
						String[] prvaNit = ipAdresePoSegmentima[i].split(":"); //brojseg ipa1/ipa2
						String[] drugaNit = ipAdresePoSegmentima[i + 1].split(":"); 
						String[] trecaNit = ipAdresePoSegmentima[i + 2].split(":"); 
						

						byte[] glavniBafer = new byte[3*1300];

						ExecutorService service = Executors.newSingleThreadExecutor();

						DownloadingNit nit1 = new DownloadingNit(Integer.parseInt(prvaNit[0]), prvaNit[1].split("/"), nazivFajlaZaDownload);
						DownloadingNit nit2 = new DownloadingNit(Integer.parseInt(drugaNit[0]), drugaNit[1].split("/"), nazivFajlaZaDownload);
						DownloadingNit nit3 = new DownloadingNit(Integer.parseInt(trecaNit[0]), trecaNit[1].split("/"), nazivFajlaZaDownload);

						Future<?> f1 = service.submit(nit1);
						Future<?> f2 = service.submit(nit2);
						Future<?> f3 = service.submit(nit3);


						while(true) {
							int manje = -1;
							if(f1.isDone()) {
								//upisati na odgovarajuce mesto u glavnom baferu
								byte[] bafer = (byte[]) f1.get();
								for (int j = 0; j < bafer.length; j++) {
									glavniBafer[j] = bafer[j];
								}
							}
							
							if(f2.isDone()) {
								byte[] bafer = (byte[]) f2.get();
								for (int j = 0; j < bafer.length; j++) {
									glavniBafer[j+1300] = bafer[j];
								}
							}
							
							if(f3.isDone()) {
								byte[] bafer = (byte[]) f3.get();
								for (int j = 0; j < bafer.length; j++) {
									glavniBafer[j + 2600] = bafer[j];
								}
								if(bafer.length<1300){
									manje=bafer.length;
								}
							}
							
							
							if(f1.isDone() && f2.isDone() && 
									f3.isDone()) {
								if(manje!=-1){
									byte[] noviBafer = new byte[2600+manje];
									for (int j = 0; j < noviBafer.length; j++) {
										noviBafer[j]=glavniBafer[j];
									}
									output.write(noviBafer);
								}else{
								output.write(glavniBafer);}
								downloadovaniSegmenti+=3;
								double procenat = ((double)downloadovaniSegmenti/ipAdresePoSegmentima.length)*100;
								System.out.println(round(procenat, 2)+" %");
								i+=3;
								break;
							}
						}
						
					}
					
					if((ipAdresePoSegmentima.length - downloadovaniSegmenti) % 4 == 2) {
						
						String[] prvaNit = ipAdresePoSegmentima[i].split(":"); //brojseg ipa1/ipa2
						String[] drugaNit = ipAdresePoSegmentima[i + 1].split(":"); 
						

						byte[] glavniBafer = new byte[2*1300];

						ExecutorService service = Executors.newSingleThreadExecutor();

						DownloadingNit nit1 = new DownloadingNit(Integer.parseInt(prvaNit[0]), prvaNit[1].split("/"), nazivFajlaZaDownload);
						DownloadingNit nit2 = new DownloadingNit(Integer.parseInt(drugaNit[0]), drugaNit[1].split("/"), nazivFajlaZaDownload);
						

						Future<?> f1 = service.submit(nit1);
						Future<?> f2 = service.submit(nit2);


						while(true) {
							int manje=-1;
							if(f1.isDone()) {
								//upisati na odgovarajuce mesto u glavnom baferu
								byte[] bafer = (byte[]) f1.get();
								for (int j = 0; j < bafer.length; j++) {
									glavniBafer[j] = bafer[j];
								}
							}
							
							if(f2.isDone()) {
								byte[] bafer = (byte[]) f2.get();
								for (int j = 0; j < bafer.length; j++) {
									glavniBafer[j+1300] = bafer[j];
								}
								if(bafer.length<1300){
									manje=bafer.length;
								}
							}
							
							if(f1.isDone() && f2.isDone()) {
								if(manje!=-1){
									byte[] noviBafer = new byte[1300+manje];
									for (int j = 0; j < noviBafer.length; j++) {
										noviBafer[j]=glavniBafer[j];
									}
									output.write(noviBafer);
								}else{
								output.write(glavniBafer);}
								downloadovaniSegmenti+=2;
								double procenat = ((double)downloadovaniSegmenti/ipAdresePoSegmentima.length)*100;
								System.out.println(round(procenat, 2)+" %");
								i+=2;
								break;
							}
						}
						
					}
					
					if((ipAdresePoSegmentima.length - downloadovaniSegmenti) % 4 == 1) {
						
						String[] prvaNit = ipAdresePoSegmentima[i].split(":"); //brojseg ipa1/ipa2
						
						ExecutorService service = Executors.newSingleThreadExecutor();

						DownloadingNit nit1 = new DownloadingNit(Integer.parseInt(prvaNit[0]), prvaNit[1].split("/"), nazivFajlaZaDownload);
						

						Future<?> f1 = service.submit(nit1);
						


						while(true) {
							
							if(f1.isDone()) {
								//upisati na odgovarajuce mesto u glavnom baferu
								byte[] bafer = (byte[]) f1.get();
								output.write(bafer);
								downloadovaniSegmenti++;
								double procenat = ((double)downloadovaniSegmenti/ipAdresePoSegmentima.length)*100;
								System.out.println(round(procenat, 2)+" %");
								i++;
								break;
							}
							
						}
						
					}
					
					
					
				}
				System.out.println("Zavrseno preuzimanje");
				izlazniTokKaCentralnomServeru.println("OK");
				listaFajlovaZaSeedovanje.add("D://"+nazivFajlaZaDownload); //azuriranje klijentove liste fajlova
				
				output.close();
				
				

			}

		} catch (UnknownHostException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	
}

