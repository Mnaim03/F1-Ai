package scr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KNearestNeighbor {

    private List<Sample> trainingData;
    private KDTree kdtree;
    private int[] classCounts;
    private String firstLineOfTheFile;

    private double[] means;
    private double[] stds;
    private int numFeatures;

    public KNearestNeighbor(String filename) {
        this.trainingData = new ArrayList<>();
        this.classCounts = new int[10]; //le classi di appartenenza sono 10
        this.firstLineOfTheFile = "ANGLE;SPEED_X;SPEED_Y;TRACK_0;TRACK_1;TRACK_2;TRACK_3;TRACK_4;TRACK_5;TRACK_6;TRACK_7;TRACK_8;TRACK_9;TRACK_10;TRACK_11;TRACK_12;TRACK_13;TRACK_14;TRACK_15;TRACK_16;TRACK_17;TRACK_18;TRACK_POS;CLASS";
        this.readPointsFromCSV(filename);

        //Calcolo il numero di features in base alla lunghezza del vettore features di un singolo Sample 
        numFeatures = trainingData.get(0).features.length;

        this.means = new double[numFeatures];
        this.stds = new double[numFeatures];

      //  scalingTrainingData(); // scalo il trainingData
        this.kdtree = new KDTree(trainingData); // Initialize the KDTree using the read points  
    }

    public void calculateMeans() {
        // Inizializza l'array delle medie con zeri
        for (int i = 0; i < numFeatures; i++) {
            means[i] = 0.0;
        }

        // Calcola la somma di ciascuna feature
        for (Sample sample : trainingData) {
            for (int i = 0; i < sample.features.length; i++) {
                means[i] += sample.features[i];
            }
        }

        // Calcola la media dividendo per il numero di campioni
        for (int i = 0; i < numFeatures; i++) {
            means[i] /= trainingData.size();
        }
    }

    public void calculateStds() {
        calculateMeans(); // calcolo le medie

        // Inizializza l'array delle deviazioni standard con zeri
        for (int i = 0; i < numFeatures; i++) {
            stds[i] = 0.0;
        }

        // Calcola la somma dei quadrati delle differenze
        for (Sample sample : trainingData) {
            for (int i = 0; i < numFeatures; i++) {
                stds[i] += Math.pow(sample.features[i] - means[i], 2);
            }
        }

        // Calcola la radice quadrata della media dei quadrati delle differenze
        for (int i = 0; i < numFeatures; i++) {
            stds[i] = Math.sqrt(stds[i] / trainingData.size());
        }
    }

    public Sample scalingSample(Sample sample) {
        for (int i = 0; i < numFeatures; i++) {
            sample.features[i] = (sample.features[i] - means[i]) / stds[i];
        }
        return sample;
    }

    public void scalingTrainingData() {
        calculateStds(); // calcolo la deviazione standard per ogni features

        for (int i = 0; i < trainingData.size(); i++) {
            Sample sample = trainingData.get(i);
            Sample scaledSample = scalingSample(sample);
            trainingData.set(i, scaledSample);
        }
    }

    private void readPointsFromCSV(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(firstLineOfTheFile)) {
                    continue; // Skip header
                }
                // Add the sample by calling the constructor that takes the string input
                trainingData.add(new Sample(line));
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Sample> findKNearestNeighbors(Sample testPoint, int k) {
        return kdtree.kNearestNeighbors(testPoint, k);
    }

    public int classify(Sample testPoint, int k) {
        //bisogna scalare il testPoint per poterlo confrontare con i punti delle classi (già precedentemente scalati)
       // Sample testPointScaled = scalingSample(testPoint);
        Sample testPointScaled = testPoint;

        List<Sample> kNearestNeighbors = findKNearestNeighbors(testPointScaled, k);

        // Reset class counts
        for (int i = 0; i < classCounts.length; i++) {
            classCounts[i] = 0;
        }

        // Count the occurrences of each class in the k nearest neighbors
        for (Sample neighbor : kNearestNeighbors) {
            classCounts[neighbor.cls]++;
        }

        // Find the class with the maximum count
        int maxCount = -1;
        int predictedClass = -1;
        for (int i = 0; i < classCounts.length; i++) {
            if (classCounts[i] > maxCount) {
                maxCount = classCounts[i];
                predictedClass = i;
            }
        }

        return predictedClass;
    }

    public List<Sample> getTrainingData() {
        return trainingData;
    }
}
