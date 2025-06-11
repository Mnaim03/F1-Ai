package scr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SimpleDriver extends Controller {

	/* Gear Changing Constants */
	final int[] gearUp = { 5000, 6000, 6000, 6500, 7000, 0 };
	final int[] gearDown = { 0, 2500, 3000, 3000, 3500, 3500 };

	public void reset() {
		System.out.println("Restarting the race!");
	}

	public void shutdown() {
		System.out.println("Bye bye!");
	}

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

	private float currBrake = (float) 0.1;
	private float brake(boolean reset) {
		if(reset){
			currBrake = (float) 0.1;
		} else {
			currBrake += 0.1;
		}
		return currBrake > 1 ? 1 : currBrake;
	}

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

	private void writeDataToCSV(SensorModel sensors, Action action, int classe) {
		String filePath = "dataset.csv";
		File file = new File(filePath);

		try {
			// Controlla se il file esiste e se è vuoto
			boolean isEmpty = !file.exists() || file.length() == 0;

			BufferedWriter writer = new BufferedWriter(new FileWriter(file, true)); // 'true' per modalità append

			if (isEmpty) {
				// Scrivi l'intestazione solo se il file è vuoto
				writer.write("ANGLE;SPEED_X;SPEED_Y;TRACK_-90;TRACK_-45;TRACK_-20;TRACK_0;TRACK_20;TRACK_45;TRACK_90;TRACK_POS;CLASS\n");
			}

			StringBuilder line = new StringBuilder();

			// Append sensor and action data to line
			line.append(sensors.getAngleToTrackAxis()).append(";");
			line.append(sensors.getSpeed()).append(";");
			line.append(sensors.getLateralSpeed()).append(";");
			line.append(sensors.getTrackEdgeSensors()[0]).append(";");
			line.append(sensors.getTrackEdgeSensors()[3]).append(";");
			line.append(sensors.getTrackEdgeSensors()[5]).append(";");
			line.append(sensors.getTrackEdgeSensors()[9]).append(";");
			line.append(sensors.getTrackEdgeSensors()[13]).append(";");
			line.append(sensors.getTrackEdgeSensors()[15]).append(";");
			line.append(sensors.getTrackEdgeSensors()[18]).append(";");
			line.append(sensors.getTrackPosition()).append(";");
			line.append(classe).append(";");

			// Write the line to the file
			writer.write(line.toString());
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} 

	public Action control(SensorModel sensors, String input) {

		System.out.println("SimpleDriver.control ha ricevuto come input: " + input);

		Action action = new Action();
		action.gear = getGear(sensors);
		int classe;

		if (input.equals("w")) {
			action.accelerate = 1;
			brake(true);
			steer(true, false);
			classe = 1;
		}

		else if (input.equals("q")) {
			action.accelerate = 1;
			brake(true);
			action.steering = steer(false, false);
			classe = 2;
		}

		else if (input.equals("e")) {
			action.accelerate = 1;
			brake(true);
			action.steering = steer(false, true);
			classe = 3;
		}
		
		else if (input.equals("a")) {
			brake(true);
			action.steering = steer(false, false);
			classe = 4;
		}
		
		else if (input.equals("s")) {
			float tmp = brake(false);
			if (tmp > 0.5) {
				if(sensors.getSpeed() > 85) action.brake = 0.5;
				else if(sensors.getSpeed() > 65) action.brake = 0.75;
				else action.brake = tmp;
			}
			else action.brake = tmp;
			steer(true, false);
			classe = 5;
		}
		
		else if (input.equals("d")) {
			brake(true);
			action.steering = steer(false, true);
			classe = 6;
		}

		else if (input.equals("i")) {
			action.gear = -1;
			action.accelerate = 1;
			action.steering = 0.80;
			brake(true);
			steer(true, false);
			classe = 7;
		}

		else if (input.equals("o")) {
			action.gear = -1;
			action.accelerate = 1;
			brake(true);
			steer(true, false);
			classe = 8;
		}

		else if (input.equals("p")) {
			action.gear = -1;
			action.accelerate = 1;
			action.steering = -0.80;
			brake(true);
			steer(true, false);
			classe = 9;
		}
		
		else {
			brake(true);
			steer(true, false);
			classe = 0;
		}
		
		if(Client.printFlag){
			writeDataToCSV(sensors, action, classe);
			System.out.println("Printed to CSV");
		} else {
			System.out.println("Not printing");
		}

		return action;
	}

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
