import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class GeneticAlgorithm {

    int POPULATION_SIZE = 100;
    double MAX_CAPACITY = -1;
    double NUM_ITEMS = -1;
    int TOURNAMENT_SIZE = 3;
    int FIT_PARENTS_SIZE = 40;

    double MUTATION_RATE = 0.01;
    double CROSSOVER_RATE = 0.6;
    int RECPLACE_COUNT = 40;

    String filename;
    List<Item> items = new ArrayList<>();
    List<Individual> initPop = new ArrayList<>();

    List<Individual> offspring = new ArrayList<>();

    double maxValue;
    double minWeight;
    final double penaltyCoefficient;

    Random rValue;
    long seed;

    GeneticAlgorithm(String filename, long seed) throws FileNotFoundException, IOException {
        this.filename = filename;
        this.seed = seed;
        this.rValue = new java.util.Random(seed);
        loadFromFile();
        maxValue = items.stream().mapToDouble(Item::getValue).max().getAsDouble();
        minWeight = items.stream().mapToDouble(Item::getWeight).min().getAsDouble();
        penaltyCoefficient = Math.max(1, maxValue / Math.max(1, minWeight));
        runGA(100);
    }

    void runGA(int generations) {
        long startTime = System.currentTimeMillis();
        initialisePopulation();

        Individual bestOne = null;

        for (int gen = 0; gen < generations; gen++) {
            offspring.clear();
            // produce new childern

            for (int i = 0; i < FIT_PARENTS_SIZE; i++) {
                Individual parent1 = fitnessTournament();
                Individual parent2 = fitnessTournament();

                crossover(parent1, parent2);
            }

            // mutate offspring

            for (Individual child : offspring) {
                mutate(child);
                computeFitnessScore(child);

            }
            replace();
            Individual genBest = initPop.stream()
                    .filter(ind -> ind.isValid)
                    .max((a, b) -> a.fitnessScore - b.fitnessScore)
                    .orElse(null);

            if (genBest != null && (bestOne == null || genBest.fitnessScore > bestOne.fitnessScore)) {
                bestOne = genBest;
            }
        }
        long runtime = (System.currentTimeMillis() - startTime);
        writeResults(bestOne, runtime);
    }

    void writeResults(Individual best, long runtimeSeconds) {
        String outputPath = "results.txt";
        int bestScore = best != null ? best.fitnessScore : 0;
        Double optimum = KNOWN_OPTIMUMS.getOrDefault(filename, -1.0);

        try (FileWriter fw = new FileWriter(outputPath, true);
                BufferedWriter bw = new BufferedWriter(fw)) {

            bw.write(String.format("%-35s %-15s %-12s %-15s %-15s %s%n",
                    filename, "GA", seed, bestScore, optimum, runtimeSeconds));

        } catch (IOException e) {
            System.err.println("Failed to write results: " + e.getMessage());
        }
    }

    void initialisePopulation() {

        for (int i = 0; i < POPULATION_SIZE; i++) {
            List<Integer> list = new ArrayList<>();
            for (int j = 0; j < NUM_ITEMS; j++) {
                list.add(randomizeBit());
            }
            Individual indv = new Individual(list);
            initPop.add(indv);
            computeFitnessScore(indv);

        }
    }

    int randomizeBit() {
        return rValue.nextInt(2);
    }

    void mutate(Individual indv) {
        for (int i = 0; i < indv.chromosome.size(); i++) {
            if (random() < MUTATION_RATE) {
                indv.chromosome.set(i, (indv.chromosome.get(i) == 1) ? 0 : 1);
            }
        }
    }

    void crossover(Individual indv1, Individual indv2) {

        if (random() > CROSSOVER_RATE)
            return;

        int i = 1 + (int) (random() * (indv1.chromosome.size() - 1));
        List<Integer> child2 = new ArrayList<>();
        child2.addAll(indv2.chromosome.subList(0, i));
        child2.addAll(indv1.chromosome.subList(i, indv1.chromosome.size()));

        List<Integer> child1 = new ArrayList<>();
        child1.addAll(indv1.chromosome.subList(0, i));
        child1.addAll(indv2.chromosome.subList(i, indv2.chromosome.size()));

        offspring.add(new Individual(child1));
        offspring.add(new Individual(child2));

    }

    void computeValidity(Individual individual_1) {
        individual_1.setValidity(true);
        List<Integer> chromosome = individual_1.chromosome;
        double sumWeight = 0;
        for (int i = 0; i < chromosome.size(); i++) {
            if (chromosome.get(i).equals(1)) {
                sumWeight += items.get(i).getWeight();
                if (sumWeight > MAX_CAPACITY) {
                    individual_1.setValidity(false);
                }
            }
        }
        individual_1.totalWeight = sumWeight;

    }

    int computeTotalValues(Individual individual_1) {
        List<Integer> chromosome = individual_1.chromosome;
        double sumValues = 0;
        for (int i = 0; i < chromosome.size(); i++) {
            if (chromosome.get(i).equals(1)) {
                sumValues += items.get(i).getValue();
            }
        }

        return (int) Math.round(sumValues);
    }

    void computeFitnessScore(Individual individual_1) {
        computeValidity(individual_1);
        if (individual_1.isValid) {

            individual_1.setFitnessScore(computeTotalValues(individual_1));
        } else {
            // ftnesscore = total value - (penaltycoeeficient * exceedvalue)

            int score = (int) (computeTotalValues(individual_1)
                    - (penaltyCoefficient * (individual_1.totalWeight - MAX_CAPACITY)));
            individual_1.setFitnessScore(score);
        }
    }

    void loadFromFile() throws FileNotFoundException, IOException {
        String filePath = "Knapsack Instances/" + filename;
        BufferedReader br = new BufferedReader(new FileReader(filePath));

        String line = null;
        int index = 0;

        while ((line = br.readLine()) != null && line.length() != 0) {
            index++;

            int indx = line.indexOf(" ") + 1;

            double secondValue = parseValue(line.substring(indx).trim());
            double firstValue = parseValue(line.substring(0, indx).trim());

            if (index == 1) {

                MAX_CAPACITY = secondValue;
                NUM_ITEMS = firstValue;

            } else {

                items.add(new Item(secondValue, firstValue));
            }

        }
        br.close();

    }

    double random() {
        return rValue.nextDouble();
    }

    int randomIndex() {
        return rValue.nextInt(initPop.size());

    }

    Individual fitnessTournament() {
        List<Individual> players = new ArrayList<>();

        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            players.add(initPop.get(randomIndex()));
        }

        return Collections.max(players, (a, b) -> a.fitnessScore - b.fitnessScore);
    }

    double parseValue(String s) {
        return Double.parseDouble(s);
    }

    void replace() {
        // steady state with eilism
        // replace unfit 40 inaavlid? or valid, or dont care??
        if (offspring.isEmpty())
            return;

        // worst first
        initPop.sort((a, b) -> a.fitnessScore - b.fitnessScore);

        // sort offsring
        offspring.sort((a, b) -> b.fitnessScore - a.fitnessScore);

        Individual best = initPop.get(POPULATION_SIZE - 1); // sort offspring by best
        int replaceCount = Math.min(offspring.size(), RECPLACE_COUNT);
        for (int i = 0; i < replaceCount; i++) {
            if (offspring.get(i).fitnessScore > initPop.get(i).fitnessScore) {
                initPop.set(i, offspring.get(i));
            }
        }
        initPop.set(POPULATION_SIZE - 1, best);
    }

    private static final Map<String, Double> KNOWN_OPTIMUMS = new HashMap<>();
    static {
        KNOWN_OPTIMUMS.put("f1_l-d_kp_10_269", 295.0);
        KNOWN_OPTIMUMS.put("f2_l-d_kp_20_878", 1024.0);
        KNOWN_OPTIMUMS.put("f3_l-d_kp_4_20", 35.0);
        KNOWN_OPTIMUMS.put("f4_l-d_kp_4_11", 23.0);
        KNOWN_OPTIMUMS.put("f5_l-d_kp_15_375", 481.0694);
        KNOWN_OPTIMUMS.put("f6_l-d_kp_10_60", 52.0);
        KNOWN_OPTIMUMS.put("f7_l-d_kp_7_50", 107.0);
        KNOWN_OPTIMUMS.put("knapPI_1_100_1000_1", 9147.0);
        KNOWN_OPTIMUMS.put("f8_l-d_kp_23_10000", 9767.0);
        KNOWN_OPTIMUMS.put("f9_l-d_kp_5_80", 130.0);
        KNOWN_OPTIMUMS.put("f10_l-d_kp_20_879", 1025.0);
    }
}
