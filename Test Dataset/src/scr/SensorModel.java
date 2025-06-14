package scr;

/**
 * Interfaccia per il modello dei sensori di un'auto da corsa.
 * Questa interfaccia definisce i metodi per ottenere informazioni sui sensori
 * dell'auto, come velocità, angolo rispetto all'asse della pista, posizione sulla pista, e informazioni sugli avversari.
 * Può essere implementata da classi che rappresentano un'auto da corsa e il suo comportamento in un simulatore di corse.
 * Utilizzata in contesti di intelligenza artificiale per il controllo dell'auto in un ambiente di gara virtuale.
 * * @author Your Name
 */
public interface SensorModel {

	// basic information about your car and the track (you probably should take care
	// of these somehow)

	public double getSpeed();

	public double getAngleToTrackAxis();

	public double[] getTrackEdgeSensors();

	public double[] getFocusSensors();// ML

	public double getTrackPosition();

	public int getGear();

	// basic information about other cars (only useful for multi-car races)

	public double[] getOpponentSensors();

	public int getRacePosition();

	// additional information (use if you need)

	public double getLateralSpeed();

	public double getCurrentLapTime();

	public double getDamage();

	public double getDistanceFromStartLine();

	public double getDistanceRaced();

	public double getFuelLevel();

	public double getLastLapTime();

	public double getRPM();

	public double[] getWheelSpinVelocity();

	public double getZSpeed();

	public double getZ();

	public String getMessage();

}
