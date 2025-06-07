package scr;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import scr.Action;
import scr.Controller;
import scr.MessageBasedSensorModel;

public abstract class SimpleDriver extends Controller {

	// ===================== PARAMETRI DI CONTROLLO =====================
	private final float Kp = 2.0f;
	private final float Kd = 4.0f;
	private final float MAX_SPEED_STRAIGHT = 350.0f;
	private final float MAX_SPEED_CURVE = 200.0f;
	private final float STRAIGHT_THRESHOLD = 25.0f;
	private final int RPM_UPSHIFT = 7000;
	private final int RPM_DOWNSHIFT = 1500;
	private final float BRAKE_FACTOR = 10.f;

	// ===================== LOGGING =====================
	private List<String[]> logData = new ArrayList<>();
	private boolean headerWritten = false;
	private String baseFileName;
	private long startTimeMs;

	@Override
	public Action control(MessageBasedSensorModel sensors) {

		// Inizializza tempo e file al primo tick
		if (baseFileName == null) {
			startTimeMs = System.currentTimeMillis();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			baseFileName = "torcs_logs/torcs_log_" + sdf.format(new Date(startTimeMs));
			File dir = new File("torcs_logs");
			if (!dir.exists()) dir.mkdirs();
		}

		float speed = (float) sensors.getSpeed();
		float trackPos = (float) sensors.getTrackPosition();
		float angle = (float) sensors.getAngleToTrackAxis();
		double[] trackSensors = sensors.getTrackEdgeSensors();
		float rpm = (float) sensors.getRPM();
		int currentGear = sensors.getGear();
		float curLapTime = (float) sensors.getCurrentLapTime();
		float distRaced = (float) sensors.getDistanceRaced();

		// Recupero in caso di uscita di pista
		if (Math.abs(trackPos) > 1.2 || speed < 5) {
			Action recoveryAction = new Action();
			recoveryAction.gear = 1;
			recoveryAction.steering = -trackPos;
			recoveryAction.accelerate = 0.3;
			recoveryAction.brake = 0;
			return recoveryAction;
		}

		// Velocità target
		float avgFront = (float)((trackSensors[8] + trackSensors[9] + trackSensors[10]) / 3.0);
		float targetSpeed = (avgFront > STRAIGHT_THRESHOLD) ? MAX_SPEED_STRAIGHT : MAX_SPEED_CURVE;

		// Sterzata (controllore PD)
		float steer = (trackPos * Kp) + (angle * Kd);

		// Cambio marcia
		int newGear = currentGear;
		if (currentGear <= 0) newGear = 1;
		else if (rpm > RPM_UPSHIFT && currentGear < 6) newGear++;
		else if (rpm < RPM_DOWNSHIFT && currentGear > 1) newGear--;

		// Accelerazione/frenata
		double accel = 0, brake = 0;
		brake = Math.min((speed - targetSpeed) * BRAKE_FACTOR, 1.0);

		// Azione finale
		Action finalAction = new Action();
		finalAction.gear = newGear;
		finalAction.steering = steer;
		finalAction.accelerate = accel;
		finalAction.brake = brake;

		// Log della riga
		logData.add(new String[] {
				String.format(Locale.US, "%.3f", curLapTime),
				String.format(Locale.US, "%.3f", distRaced),
				String.format(Locale.US, "%.3f", speed),
				String.format(Locale.US, "%.3f", trackPos),
				String.format(Locale.US, "%.3f", angle),
				String.valueOf(currentGear),
				String.format(Locale.US, "%.1f", rpm),
				String.format(Locale.US, "%.4f", steer),
				String.format(Locale.US, "%.2f", accel),
				String.format(Locale.US, "%.2f", brake),
				String.valueOf(newGear)
		});

		return finalAction;
	}

	@Override
	public void reset() {
		System.out.println("===> Gara terminata. Scrittura dei file di log...");

		// Nome dei file associati
		String csvFile = baseFileName + ".csv";
		String infoFile = baseFileName + "_info.txt";

		// Calcolo tempo di gara
		long totalTimeMs = System.currentTimeMillis() - startTimeMs;
		float totalTimeSeconds = totalTimeMs / 1000f;

		// Salva log CSV
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, true))) {
			if (!headerWritten) {
				writer.write("curLapTime,distRaced,speed,trackPos,angle,gear,rpm,steer,accel,brake,newGear\n");
				headerWritten = true;
			}
			for (String[] row : logData) {
				writer.write(String.join(",", row));
				writer.newLine();
			}
			System.out.println("Log CSV salvato: " + csvFile);
		} catch (IOException e) {
			System.err.println("Errore CSV: " + e.getMessage());
		}

		// Salva file di info
		try (BufferedWriter info = new BufferedWriter(new FileWriter(infoFile))) {
			info.write("== PARAMETRI UTILIZZATI ==\n");
			info.write("Kp = " + Kp + "\n");
			info.write("Kd = " + Kd + "\n");
			info.write("MAX_SPEED_STRAIGHT = " + MAX_SPEED_STRAIGHT + "\n");
			info.write("MAX_SPEED_CURVE = " + MAX_SPEED_CURVE + "\n");
			info.write("STRAIGHT_THRESHOLD = " + STRAIGHT_THRESHOLD + "\n");
			info.write("RPM_UPSHIFT = " + RPM_UPSHIFT + "\n");
			info.write("RPM_DOWNSHIFT = " + RPM_DOWNSHIFT + "\n");
			info.write("BRAKE_FACTOR = " + BRAKE_FACTOR + "\n\n");
			info.write("== STATISTICHE GARA ==\n");
			info.write("Durata gara: " + totalTimeSeconds + " secondi\n");
			info.write("Campioni raccolti: " + logData.size() + "\n");
			System.out.println("File parametri salvato: " + infoFile);
		} catch (IOException e) {
			System.err.println("Errore info: " + e.getMessage());
		}

		// Reset buffer
		logData.clear();
		baseFileName = null;
	}

	@Override
	public void shutdown() {
		System.out.println("Bye bye!");
	}
}