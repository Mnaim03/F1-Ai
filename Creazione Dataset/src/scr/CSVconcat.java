package scr;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class CSVconcat {

    private static String firstLineOfTheFile = "ANGLE;SPEED_X;SPEED_Y;TRACK_-90;TRACK_-45;TRACK_-20;TRACK_0;TRACK_20;TRACK_45;TRACK_90;TRACK_POS;CLASS";

    private static void readPointsFromCSV(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\andre\\OneDrive\\Desktop\\datasetRecupero.csv"));
            BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\andre\\OneDrive\\Desktop\\dataset.csv", true));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(firstLineOfTheFile)) {
                    continue; // Skip header
                }
                writer.write(line.toString());
                writer.newLine();
            }
            writer.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        readPointsFromCSV("dataset.csv");
        System.out.println("Completato");
    }
}


