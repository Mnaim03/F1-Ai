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

    /**
     * Costruttore: legge i dati da un file CSV, calcola parametri e costruisce l'albero KD
     * @param filename nome file è traning.csv.
     */
    public KNearestNeighbor(String filename) {
        this.trainingData = new ArrayList<>();
        this.classCounts = new int[10]; //le classi di appartenenza sono 10
        this.readPointsFromCSV(filename);

        //Calcolo il numero di features in base alla lunghezza del vettore features di un singolo Sample 
        numFeatures = trainingData.get(0).features.length;

        this.means = new double[numFeatures];
        this.stds = new double[numFeatures];

      //  scalingTrainingData(); // scalo il trainingData
        this.kdtree = new KDTree(trainingData); // Initialize the KDTree using the read points  
    }

    /**
     * Calcola la media di ogni feature tra tutti i campioni del training set
     */
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

    /**
     * Calcola la deviazione standard di ogni feature tra tutti i campioni del training set
     */
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

    /**
     * Scala un singolo campione utilizzando le medie e le deviazioni standard calcolate
     * @param sample il campione da scalare
     * @return il campione scalato
     */
    public Sample scalingSample(Sample sample) {
        for (int i = 0; i < numFeatures; i++) {
            sample.features[i] = (sample.features[i] - means[i]) / stds[i];
        }
        return sample;
    }

    /**
     * Scala tutti i campioni del training set utilizzando le medie e le deviazioni standard calcolate
     */
    public void scalingTrainingData() {
        calculateStds(); // calcolo la deviazione standard per ogni features

        for (int i = 0; i < trainingData.size(); i++) {
            Sample sample = trainingData.get(i);
            Sample scaledSample = scalingSample(sample);
            trainingData.set(i, scaledSample);
        }
    }

    /**
     * Legge i punti da un file CSV e li aggiunge alla lista di trainingData.
     * Il file deve avere un'intestazione che viene saltata.
     * @param filename il nome del file CSV da leggere
     */
    private void readPointsFromCSV(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            line = reader.readLine(); // Legge la prima riga (intestazione) e la salta
            while ((line = reader.readLine()) != null) {
                // Salta le righe vuote, quelle che iniziano con "ANGLE" o quelle che non contengono numeri
                if (line.trim().isEmpty() || line.startsWith("ANGLE") || !line.matches(".*\\d.*")) {
                    continue;
                }
                // Crea un nuovo Sample a partire dalla riga letta
                trainingData.add(new Sample(line));
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Trova i k vicini più prossimi di un punto di test utilizzando l'albero KD.
     * @param testPoint il punto di test per il quale trovare i vicini
     * @param k il numero di vicini da trovare
     * @return una lista dei k vicini più prossimi
     */
    public List<Sample> findKNearestNeighbors(Sample testPoint, int k) {
        return kdtree.kNearestNeighbors(testPoint, k);
    }

    /**
     * Classifica un punto di test utilizzando il metodo dei k vicini più prossimi.
     * @param testPoint il punto di test da classificare
     * @param k il numero di vicini da considerare per la classificazione
     * @return la classe prevista per il punto di test
     */
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

    /**
     * Restituisce i dati di addestramento.
     * @return la lista dei campioni di addestramento
     */
    public List<Sample> getTrainingData() {
        return trainingData;
    }
}
