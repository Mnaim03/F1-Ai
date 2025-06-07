package scr;// Assumiamo che le classi Action e MessageBasedSensorModel siano definite nel pacchetto scr
// come mostrato nello schema del tuo PDF.
import scr.Action;
import scr.Controller;
import scr.MessageBasedSensorModel;


public abstract class SimpleDriver extends Controller {

	//--------------------------------------------------------------------------------
	// --- COSTANTI DA TARARE (IL CUORE DEL TUO LAVORO DI MIGLIORAMENTO) ---
	//--------------------------------------------------------------------------------

	// --- Sterzata ---
	// Guadagno proporzionale: quanto bruscamente sterzare in base a quanto sei fuori centro.
	private final float Kp = 0.4f;
	// Guadagno derivativo: quanto smorzare la sterzata per evitare oscillazioni.
	private final float Kd = 0.3f;

	// --- Velocità ---
	// La velocità massima desiderata in un rettilineo (in km/h).
	private final float MAX_SPEED_STRAIGHT = 200.0f;
	// La velocità massima desiderata in una curva media (in km/h).
	private final float MAX_SPEED_CURVE = 110.0f;
	// Distanza (in metri) che i sensori frontali devono "vedere" per considerare la strada un rettilineo.
	private final float STRAIGHT_THRESHOLD = 80.0f;


	// --- Cambio ---
	// Giri motore per salire di marcia.
	private final int RPM_UPSHIFT = 8500;
	// Giri motore per scalare marcia.
	private final int RPM_DOWNSHIFT = 2500;

	// --- Frenata ---
	// Un fattore per rendere la frenata più o meno aggressiva.
	private final float BRAKE_FACTOR = 06.f;


	@Override
	public Action control(MessageBasedSensorModel sensors) {

		//================================================================================
		// 1. LEGGI I SENSORI PRINCIPALI
		//================================================================================
		float speed = (float) sensors.getSpeed(); // Velocità attuale in km/h
		float trackPos = (float) sensors.getTrackPosition(); // Posizione rispetto al centro
		float angle = (float) sensors.getAngleToTrackAxis(); // Angolo rispetto alla pista
		double[] trackSensors = sensors.getTrackEdgeSensors(); // Array dei 19 sensori di distanza
		float rpm = (float) sensors.getRPM(); // Giri motore
		int currentGear = sensors.getGear(); // Marcia attuale


		//================================================================================
		// 2. LOGICA DI RECUPERO D'EMERGENZA
		//================================================================================
		// Se siamo completamente fuori pista o quasi fermi, tentiamo un recupero.
		if (Math.abs(trackPos) > 1.2 || speed < 5) {
			Action recoveryAction = new Action();
			recoveryAction.gear = 1; // Metti la prima (o la retro se necessario)
			recoveryAction.steering = -trackPos; // Sterza verso il centro della pista
			recoveryAction.accelerate = 0.3; // Accelera dolcemente
			recoveryAction.brake = 0;
			return recoveryAction;
		}


		//================================================================================
		// 3. LOGICA DI VELOCITÀ: RETTILINEO O CURVA?
		//================================================================================
		// Controlliamo il sensore frontale (indice 9) per decidere se siamo su un rettilineo.
		float targetSpeed;
		if (trackSensors[9] > STRAIGHT_THRESHOLD) {
			// Siamo su un rettilineo
			targetSpeed = MAX_SPEED_STRAIGHT;
		} else {
			// Siamo in prossimità di una curva
			// Riduciamo la velocità obiettivo. Una logica più avanzata potrebbe calcolare
			// la velocità esatta in base alla strettezza della curva.
			targetSpeed = MAX_SPEED_CURVE;
		}


		//================================================================================
		// 4. LOGICA DI STERZATA (CONTROLLORE PD)
		//================================================================================
		float steer = (trackPos * Kp) + (angle * Kd);


		//================================================================================
		// 5. LOGICA DEL CAMBIO
		//================================================================================
		int newGear = currentGear;
		if (currentGear <= 0) { // Se siamo in folle o retromarcia, mettiamo la prima per partire
			newGear = 1;
		} else if (rpm > RPM_UPSHIFT && currentGear < 6) {
			newGear++; // Sali di marcia
		} else if (rpm < RPM_DOWNSHIFT && currentGear > 1) {
			newGear--; // Scala marcia
		}


		//================================================================================
		// 6. LOGICA FINALE DI ACCELERAZIONE E FRENO
		//================================================================================
		double accel = 0;
		double brake = 0;

		if (speed < targetSpeed) {
			// Se andiamo più piano della velocità obiettivo, acceleriamo.
			// Acceleriamo in modo proporzionale a quanto ci manca per arrivare alla velocità target.
			accel = 0.8f; // Puoi anche fare (targetSpeed - speed) / targetSpeed per un'accelerazione più dolce.
			brake = 0;
		} else {
			// Se andiamo più veloci della velocità obiettivo, freniamo.
			accel = 0;
			brake = (speed - targetSpeed) * BRAKE_FACTOR; // Frena in modo proporzionale
		}


		//================================================================================
		// 7. COSTRUISCI E RESTITUISCI L'AZIONE FINALE
		//================================================================================
		Action finalAction = new Action();
		finalAction.gear = newGear;
		finalAction.steering = steer;
		finalAction.accelerate = accel;
		finalAction.brake = brake;

		// Stampa di debug (utile per la taratura)
		// System.out.println("Speed: " + speed + " | Target: " + targetSpeed + " | Steer: " + steer);

		return finalAction;
	}

	@Override
	public void reset() {
		System.out.println("Restarting the race!");
	}

	@Override
	public void shutdown() {
		System.out.println("Bye bye!");
	}

}