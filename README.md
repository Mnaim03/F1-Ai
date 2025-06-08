# F1-Ai
Progetto di AI
Comando di avvio: java scr.Client scr.SimpleDriver host:localhost port:3001 verbose:on

✅ Strategia in 5 Fasi

1. Raccolta del Dataset: guida dell’esperto
	•	Guida manualmente per alcuni giri completi.
	•	Registra a ogni step:
	•	Input: vettore track (es. distanza dai bordi della pista in varie direzioni).
	•	Output: comandi effettuati (velocità target e sterzata).
	•	Salva i dati in un CSV o altro formato: features = [track_1, ..., track_n], labels = [steering, acceleration].

📌 Consiglio: Diversifica il dataset. Guida in modo normale, ma includi anche situazioni di curva, frenate, uscite di pista leggere, ecc. per aumentare la robustezza.

⸻

2. Preprocessing dei dati
	•	Normalizza le feature (es. Min-Max o StandardScaler) per aiutare i classificatori.
	•	Eventualmente riduci la dimensionalità se hai troppe feature inutili (PCA o feature selection).
	•	Puoi dividere il dataset in:
	•	Training (80%)
	•	Validation/Test (20%)

⸻

3. Scegliere il modello (inizialmente K-NN)
	•	Il K-NN è semplice, ma ha problemi di scalabilità:
	•	Richiede tutta la memoria per i dati.
	•	La predizione è lenta (calcola distanza da tutti i punti).
	•	Performance cala con molti campioni (>10.000).

👨‍🔬 Soluzioni possibili:

➤ A. Riduzione dei dati
	•	Usa clustering per ridurre la quantità di esempi (es. K-means per rappresentare ogni gruppo).
	•	Oppure usa solo i campioni più informativi, scartando quelli molto simili tra loro.

➤ B. Sostituire K-NN con un modello più leggero
	•	Piccola Rete Neurale Feedforward (MLP) – buona generalizzazione, veloce in test.
	•	Decision Tree o Random Forest – interpretabili e più veloci del K-NN in prediction.

⸻

4. Evitare di uscire dallo stato appreso

Per evitare il problema del covariate shift (cioè lo stato corrente è molto diverso da quello visto in training), puoi:
	•	Data Augmentation: aggiungi piccoli rumori ai comandi nel training (es. sterza leggermente di più o di meno).
	•	DAgger (opzionale): metodo iterativo che aggiorna il dataset includendo anche i casi dove il tuo agente autonomo fa errori e viene corretto dall’esperto.

⸻

5. Implementazione e testing su TORCS
	•	Scegli una pista semplice all’inizio.
	•	Valuta metriche come:
	•	Tempo totale del giro
	•	Numero di uscite di pista
	•	Tempo medio tra correzioni

📌 Debug tip: Salva video o log durante i test per analizzare cosa fa l’agente in tempo reale.

✅ Quando INCLUDERE i giri andati male

✔️ Per rendere il modello robusto agli errori
	•	Se il tuo modello ha visto solo traiettorie perfette, non saprà cosa fare se sbaglia.
	•	Includere situazioni di errore umano (es. sterzate sbagliate, uscite di pista) aiuta a imparare anche da condizioni anomale.

✔️ Se vuoi usare tecniche tipo DAgger in futuro
	•	Ti servirà che il modello sappia riconoscere e correggere anche traiettorie imperfette.

⸻

❌ Quando ESCLUDERE i giri con errori gravi

✖️ Se il dataset contiene errori evidenti non gestiti
	•	Giri dove l’auto rimane ferma, gira su sé stessa, si blocca contro un muro…
	•	In questi casi, il modello potrebbe imitare un comportamento disfunzionale.

✖️ Se vuoi un primo modello stabile e semplice
	•	Per una prima versione, conviene addestrarsi solo su traiettorie valide, poi eventualmente aggiungere casi speciali per aumentare la resilienza.

🎯 Strategia consigliata (ibrida e sicura)
	1.	🧑‍🏫 Fase 1 – Raccolta manuale
	•	Guida umana, dataset bc_dataset_manual_*.csv
	2.	🤖 Fase 2 – Primo modello
	•	Addestra SimpleDriver su questi dati
	3.	🔄 Fase 3 – Giri autonomi monitorati
	•	Esegui giri col tuo agente
	•	Logga i dati (track, steering, accel)
	•	Eventualmente aggiungi una colonna tipo source=auto o source=manual
	4.	🧹 Fase 4 – Pulizia o supervisione
	•	Escludi i tratti dove l’auto era bloccata, ferma, ecc.
	•	Oppure correggi le azioni sbagliate manualmente (approccio DAgger puro)
	5.	🧠 Fase 5 – Riaddestramento
	•	Unisci i dataset selezionati
	•	Riaddestra il modello su un dataset più ampio e realistico
