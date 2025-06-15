package scr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleDriver extends Controller {

	/* Gear Changing Constants */
	final int[] gearUp = { 5000, 6000, 6000, 6500, 7000, 0 };
	final int[] gearDown = { 0, 2500, 3000, 3000, 3500, 3500 };

	// Lista per memorizzare i dati durante l'esecuzione
	private List<DataEntry> dataEntries = new ArrayList<>();

	// Classe per memorizzare i dati di una singola entry
	private static class DataEntry {
		double angleToTrackAxis;
		double speed;
		double lateralSpeed;
		double[] trackEdgeSensors;
		double trackPosition;
		int classe;

		public DataEntry(double angleToTrackAxis, double speed, double lateralSpeed, 
						double[] trackEdgeSensors, double trackPosition, int classe) {
			this.angleToTrackAxis = angleToTrackAxis;
			this.speed = speed;
			this.lateralSpeed = lateralSpeed;
			this.trackEdgeSensors = trackEdgeSensors.clone(); // Clone per evitare riferimenti
			this.trackPosition = trackPosition;
			this.classe = classe;
		}
	}

	//oggetto del tipo K-NN
	private KNearestNeighbor knnFinder;

	/* Metodo chiamato a inizio gara o dopo una collisione per azzerare lo stato */
	public void reset() {
		System.out.println("Restarting the race!");
	}

	/**
	 * Metodo chiamato alla fine della gara per chiedere se salvare i dati raccolti.
	 * Se l'utente sceglie di salvare, i dati vengono scritti in un file CSV.
	 */
	public void shutdown() {
		System.out.println("Bye bye!");
		if (askUserToSave()) {
			writeAllDataToCSV();
		} else {
			System.out.println("Dati non salvati come richiesto dall'utente.");
		}
	}

	/* 
	 * Metodo chiamato per chiedere all'utente se salvare i dati raccolti.
	 * Chiede all'utente se vuole salvare i dati raccolti in un file CSV.
	 * @return true se l'utente ha scelto di salvare, false altrimenti.
	 */
	private boolean askUserToSave() {
		if (dataEntries.isEmpty()) {
			System.out.println("Nessun dato da salvare.");
			return false;
		}
		
		try {
			char response;
			
			do {
				System.out.print("Vuoi salvare " + dataEntries.size() + " record nel CSV? (y/n): ");
				response = (char) System.in.read();
				
				// Consuma eventuali caratteri rimanenti (come \n)
				while (System.in.available() > 0) {
					System.in.read();
				}
				
				if (response == 'y' || response == 'Y') {
					return true;
				} else if (response == 'n' || response == 'N') {
					return false;
				} else {
					System.out.println("Risposta non valida. Inserisci 'y' per sì o 'n' per no.");
				}
			} while (true);
			
		} catch (IOException e) {
			System.err.println("Errore nella lettura dell'input: " + e.getMessage());
			// In caso di errore, non salvare per sicurezza
			return false;
		}
	}

	/**
	 * Metodo per raccogliere i dati da salvare nel CSV.
	 * Crea una nuova entry con i dati attuali e la aggiunge alla lista.
	 * @param features le caratteristiche del campione corrente
	 * @param action l'azione corrente
	 * @param classe la classe di appartenenza del campione
	 */
	private void collectDataForCSV(double[] features, Action action, int classe) {
		// Crea una nuova entry con i dati attuali

		DataEntry entry = new DataEntry(
			features[0],
			features[1],
			features[2],
			Arrays.copyOfRange(features, 3, 22),
			features[22],
			classe
		);
		
		// Aggiungi l'entry alla lista
		dataEntries.add(entry);
	}

	/**
	 * Metodo per scrivere tutti i dati raccolti in un file CSV.
	 * Se il file esiste, i nuovi dati vengono aggiunti alla fine.
	 * Se il file non esiste, viene creato e viene scritta l'intestazione.
	 */
	private void writeAllDataToCSV() {
		if (dataEntries.isEmpty()) {
			System.out.println("Nessun dato da salvare nel CSV.");
			return;
		}

		String filePath = "training.csv";
		File file = new File(filePath);

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) { 
			
			// Scrivi l'intestazione con tutti i 19 angoli
			if (!file.exists()) {
				file.createNewFile(); // Crea il file se non esiste
			}
			// Scrivi l'intestazione solo se il file è vuoto
			StringBuilder header = new StringBuilder();
			// Controlla se il file è vuoto
			// Se il file è vuoto, scrivi l'intestazione
			if (file.length() == 0) {
				header.append("ANGLE;SPEED_X;SPEED_Y;");
			}
			
			// Aggiungi le colonne per tutti i 19 sensori di traccia
			for (int i = 0; i < 19; i++) {
				header.append("TRACK_").append(i).append(";");
			}
			
			// Aggiungi le colonne per la posizione della traccia e la classe
			header.append("TRACK_POS;CLASS\n");
			// Scrivi l'intestazione nel file
			writer.write(header.toString());

			// Scrivi tutti i dati raccolti
			for (DataEntry entry : dataEntries) {
				StringBuilder line = new StringBuilder();

				// Append sensor data
				line.append(entry.angleToTrackAxis).append(";");
				line.append(entry.speed).append(";");
				line.append(entry.lateralSpeed).append(";");
				
				// Aggiungi tutti i 19 sensori di traccia
				for (int i = 0; i < 19; i++) {
					line.append(entry.trackEdgeSensors[i]).append(";");
				}
				
				// Aggiungi la posizione della traccia e la classe
				line.append(entry.trackPosition).append(";");
				line.append(entry.classe).append("\n");

				// Scrivi la riga nel file
				writer.write(line.toString());
			}
			
			System.out.println("Salvati " + dataEntries.size() + " record nel CSV: " + filePath);
			
		} catch (IOException e) {
			System.err.println("Errore durante la scrittura del file CSV: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Errore inaspettato durante la scrittura del CSV: " + e.getMessage());
			e.printStackTrace();
		}
	}


	/**
	 * Metodo per determinare il cambio di marcia in base ai sensori.
	 * 
	 * @param sensors i sensori del modello della macchina
	 * @return il numero della marcia da utilizzare
	 */
	private int getGear(SensorModel sensors) {
		int gear = sensors.getGear();
		double rpm = sensors.getRPM();

		// if gear is 0 (N) or -1 (R) just return 1
		if (gear < 1)
			return 1;
		// check if the RPM value of car is greater than the one suggested
		// to shift up the gear from the current one
		if (gear < 6 && rpm >= gearUp[gear - 1])
			return gear + 1;
		else
		// check if the RPM value of car is lower than the one suggested
		// to shift down the gear from the current one
		if (gear > 1 && rpm <= gearDown[gear - 1])
			return gear - 1;
		else // otherwhise keep current gear
			return gear;
	}


	// Queste variabili vengono utilizzate per mantenere lo stato corrente del freno e dello sterzo.
	private float currBrake = (float) 0.3;
	/**
	 * Metodo per controllare il freno.
	 * Se reset è true, il freno viene azzerato, altrimenti viene incrementato di 0.1 fino a un massimo di 1.
	 * 
	 * @param reset se true, azzera il freno, altrimenti lo incrementa
	 * @return il valore corrente del freno
	 */
	private float brake(boolean reset) {
		if(reset){
			currBrake = 0;
		} else {
			currBrake += 0.1;
		}
		return currBrake > 1 ? 1 : currBrake;
	}

	/**
	 * Metodo per controllare lo sterzo.
	 * Se reset è true, lo sterzo viene azzerato, altrimenti viene incrementato o decrementato di 0.01 a seconda della direzione.
	 * 
	 * @param reset se true, azzera lo sterzo, altrimenti lo modifica in base alla direzione
	 * @param right se true, sterza a destra, altrimenti a sinistra
	 * @return il valore corrente dello sterzo
	 */
	private float currSteer = (float) 0;
	private float steer(boolean reset, boolean right) {
		if(reset){
			currSteer = 0;
		} else if (right){
			if (currSteer>0) currSteer = (float) -0.1;
			else currSteer -= 0.01;
		} else {
			if (currSteer<0) currSteer = (float) 0.1;
			else currSteer += 0.01;
		}
		return currSteer > 0.2 ? (float) 0.2 : (currSteer < -0.2 ? (float) -0.2 : currSteer);
	}

	/**
 		* Metodo principale che controlla il comportamento del veicolo in base ai sensori.
 		* Questo metodo utilizza un classificatore K-NN per determinare l'azione da intraprendere.
 		* @param sensors il modello dei sensori che fornisce le informazioni sullo stato del veicolo
 		* @return l'azione da eseguire, che include il cambio di marcia, l'accelerazione, il freno e lo sterzo
 	*/
	public Action control(SensorModel sensors) {
		
		Action action = new Action();
		action.gear = getGear(sensors); //implementa il cambio automatico -- è limitato a usare valori dell'istante precedente
		
		//Creo il sample attuale
		double[] features = new double[]{
			sensors.getAngleToTrackAxis(), 
			sensors.getSpeed(), 
			sensors.getLateralSpeed(),
			sensors.getTrackEdgeSensors()[0],
			sensors.getTrackEdgeSensors()[1],
			sensors.getTrackEdgeSensors()[2],
			sensors.getTrackEdgeSensors()[3],
			sensors.getTrackEdgeSensors()[4],
			sensors.getTrackEdgeSensors()[5],
			sensors.getTrackEdgeSensors()[6],
			sensors.getTrackEdgeSensors()[7],
			sensors.getTrackEdgeSensors()[8],
			sensors.getTrackEdgeSensors()[9],
			sensors.getTrackEdgeSensors()[10],
			sensors.getTrackEdgeSensors()[11],
			sensors.getTrackEdgeSensors()[12],
			sensors.getTrackEdgeSensors()[13],
			sensors.getTrackEdgeSensors()[14],
			sensors.getTrackEdgeSensors()[15],
			sensors.getTrackEdgeSensors()[16],
			sensors.getTrackEdgeSensors()[17],
			sensors.getTrackEdgeSensors()[18],
			sensors.getTrackPosition()
		};
		Sample attuale = new Sample(features);

		//Trovo la classe di appartenenza con il K-NN
		int cls = knnFinder.classify(attuale, 3);


		switch (cls) {
			case 0:
				brake(true);
				steer(true, false);
				break;

			case 1:
				action.accelerate = 1;
				brake(true);
				steer(true, false);
				break;
			
			case 2:
				action.accelerate = 1;
				brake(true);
				action.steering = steer(false, false);
				break;
			
			case 3:
				action.accelerate = 1;
				brake(true);
				action.steering = steer(false, true);
				break;
			
			case 4:
				brake(true);
				action.steering = steer(false, false);
				break;
			
			case 5:
				float tmp = brake(false);
				if (tmp > 0.5) {
					if (sensors.getSpeed() > 85) action.brake = 0.5;
					else if (sensors.getSpeed() > 65) action.brake = 0.75;
				} else {
					action.brake = brake(false);
				}
				steer(true, false);
				break;
				
			case 6:
				brake(true);
				action.steering = steer(false, true);
				break;
			
			case 7:
				action.gear = -1;
				action.accelerate = 1;
				action.steering = 0.80;
				brake(true);
				steer(true, false);
				break;
			
			case 8:
				action.gear = -1;
				action.accelerate = 1;
				brake(true);
				steer(true, false);
				break;
			
			case 9:
				action.gear = -1;
				action.accelerate = 1;
				action.steering = -0.80;
				brake(true);
				steer(true, false);
				break;

			default:
				//unreachable
				System.out.println("\n\n\n\n\nErrore: nessuna classe di appartenenza trovata\n\n\n\n\n\n");
				break;
		}

		collectDataForCSV(features, action, cls);

		return action;
	}


	/**
 		* Metodo per inizializzare gli angoli di sterzata.
 		* Questo metodo restituisce un array di 19 angoli che rappresentano le posizioni di sterzata.
 		* * Gli angoli sono distribuiti uniformemente tra -90 e 90 gradi, con valori specifici per le posizioni di sterzata.
 		* * @return un array di float contenente gli angoli di sterzata
 	*/
	public float[] initAngles() {

		float[] angles = new float[19];

		/*
		 * set angles as
		 * {-90,-75,-60,-45,-30,-20,-15,-10,-5,0,5,10,15,20,30,45,60,75,90}
		 */
		for (int i = 0; i < 5; i++) {
			angles[i] = -90 + i * 15;
			angles[18 - i] = 90 - i * 15;
		}

		for (int i = 5; i < 9; i++) {
			angles[i] = -20 + (i - 5) * 5;
			angles[18 - i] = 20 - (i - 5) * 5;
		}
		angles[9] = 0;
		return angles;
	}
}
