import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GeneticAlgorithm {

    int POPULATION_SIZE = 100;
    double MAX_CAPACITY = -1;
    int NUM_ITEMS = -1;

    int TOURNAMENT_SIZE = 3;
    int REPLACE_COUNT = 40;
    int GENERATIONS = 100;

    double MUTATION_RATE = 0.01;
    double CROSSOVER_RATE = 0.6;

    final int ELITE_COUNT = 5;

    String filename;
    List<Item> items = new ArrayList<>();
    List<Individual> population = new ArrayList<>();
    List<Individual> offspring = new ArrayList<>();

    Random rand;
    double penaltyCoefficient;
    long seed;

    GeneticAlgorithm(String filename, long seed) throws FileNotFoundException, IOException {
        this.filename = filename;
        this.seed = seed;
        this.rand = new Random(seed);
        loadFromFile();
        double maxValue = items.stream().mapToDouble(Item::getValue).max().getAsDouble();
        double minWeight = items.stream().mapToDouble(Item::getWeight).min().getAsDouble();
        penaltyCoefficient = Math.max(1, maxValue / Math.max(1, minWeight));
        runGA(GENERATIONS);
    }

    void runGA(int generations) {
        long startTime = System.currentTimeMillis();
        initialisePopulation();

        Individual bestOne = null;

        for (int gen = 0; gen < generations; gen++) {
            offspring.clear();

            // produce new childern
            while (offspring.size() < REPLACE_COUNT) {
                Individual p1 = fitnessTournament();
                Individual p2 = fitnessTournament();
                crossover(p1, p2);
            }

            // mutate offspring

            for (Individual child : offspring) {
                mutate(child);
                computeFitnessScore(child);

            }
            replace();
            // find best indivisual in current pouplaiton , only valid ones
            Individual genBest = population.stream()
                    .filter(ind -> ind.isValid)
                    .max((a, b) -> Double.compare(a.fitnessScore, b.fitnessScore))
                    .orElse(null);

            if (genBest != null && (bestOne == null || genBest.fitnessScore > bestOne.fitnessScore)) {
                bestOne = genBest;
            }
        }
        long runtime = (System.currentTimeMillis() - startTime) / 1000;
        writeResults(bestOne, runtime);
    }

    void writeResults(Individual best, long runtimeSeconds) {
        String outputPath = "results.txt";
        double bestScore = best != null ? best.fitnessScore : 0;
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
            List<Boolean> chromosome = new ArrayList<>(Collections.nCopies(NUM_ITEMS, false));
            List<Integer> indices = new ArrayList<>();
            for (int j = 0; j < NUM_ITEMS; j++)
                indices.add(j);
            Collections.shuffle(indices, rand);

            double currentWeight = 0;
            for (int idx : indices) {
                if (currentWeight + items.get(idx).getWeight() <= MAX_CAPACITY) {
                    chromosome.set(idx, true);
                    currentWeight += items.get(idx).getWeight();
                }
            }
            Individual ind = new Individual(chromosome);
            computeFitnessScore(ind);
            population.add(ind);
        }
    }

    void mutate(Individual indv) {
        for (int i = 0; i < indv.chromosome.size(); i++) {
            if (rand.nextDouble() < MUTATION_RATE) {
                indv.chromosome.set(i, (indv.chromosome.get(i)) ? false : true);
            }
        }
    }

    void crossover(Individual indv1, Individual indv2) {

        if (rand.nextDouble() > CROSSOVER_RATE) {
            offspring.add(new Individual(new ArrayList<>(indv1.chromosome)));
            offspring.add(new Individual(new ArrayList<>(indv2.chromosome)));
            return;

        }

        int point = rand.nextInt(indv1.chromosome.size() + 1);
        List<Boolean> child2 = new ArrayList<>();
        child2.addAll(indv2.chromosome.subList(0, point));
        child2.addAll(indv1.chromosome.subList(point, indv1.chromosome.size()));

        List<Boolean> child1 = new ArrayList<>();
        child1.addAll(indv1.chromosome.subList(0, point));
        child1.addAll(indv2.chromosome.subList(point, indv2.chromosome.size()));

        offspring.add(new Individual(child1));
        offspring.add(new Individual(child2));

    }

    void computeValidity(Individual individual_1) {
        List<Boolean> chromosome = individual_1.chromosome;
        double sumWeight = 0;
        for (int i = 0; i < chromosome.size(); i++) {
            if (chromosome.get(i)) {
                sumWeight += items.get(i).getWeight();
            }
        }
        individual_1.totalWeight = sumWeight;
        individual_1.setValidity(sumWeight <= MAX_CAPACITY);

    }

    double computeTotalValues(Individual individual_1) {
        List<Boolean> chromosome = individual_1.chromosome;
        double sumValues = 0;
        for (int i = 0; i < chromosome.size(); i++) {
            if (chromosome.get(i)) {
                sumValues += items.get(i).getValue();
            }
        }

        individual_1.totalValues = sumValues;
        return sumValues;
    }

    void computeFitnessScore(Individual individual_1) {
        computeValidity(individual_1);
        if (individual_1.isValid) {

            individual_1.setFitnessScore(computeTotalValues(individual_1));
        } else {
            // ftnesscore = total value - (penaltycoeeficient * exceedvalue)

            double score = (computeTotalValues(individual_1)
                    - (penaltyCoefficient * (individual_1.totalWeight - MAX_CAPACITY)));
            individual_1.setFitnessScore(score);
        }
    }

    void loadFromFile() throws FileNotFoundException, IOException {
        String filePath = "Knapsack Instances/" + filename;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // First line: numItems capacity
            String header = br.readLine();
            if (header == null)
                throw new IOException("Empty file");
            String[] headerParts = header.trim().split("\\s+");
            NUM_ITEMS = Integer.parseInt(headerParts[0]);
            MAX_CAPACITY = Double.parseDouble(headerParts[1]);

            // Subsequent lines: value weight
            int idx = 0;
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                String[] parts = line.split("\\s+");
                double value = Double.parseDouble(parts[0]);
                double weight = Double.parseDouble(parts[1]);
                items.add(new Item(idx++, value, weight));
            }
        }
    }

    Individual fitnessTournament() {
        Individual best = null;

        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Individual candidate = population.get(rand.nextInt(population.size()));
            if (best == null || candidate.fitnessScore > best.fitnessScore)
                best = candidate;
        }

        return best;
    }

 void replace() {
    if (offspring.isEmpty())
        return;

    // Sort population worst first
    population.sort(Comparator.comparingDouble(a -> a.fitnessScore));

    // Sort offspring best first
    offspring.sort((a, b) -> Double.compare(b.fitnessScore, a.fitnessScore));

    List<Individual> newPop = new ArrayList<>();

    // 1. Preserve elites
    for (int i = POPULATION_SIZE - ELITE_COUNT; i < POPULATION_SIZE; i++) {
        newPop.add(population.get(i));
    }

    // 2. Add the best REPLACE_COUNT offspring
    for (int i = 0; i < REPLACE_COUNT; i++) {
        newPop.add(offspring.get(i));
    }

    // 3. Add the remaining old individuals
    for (int i = REPLACE_COUNT; i < POPULATION_SIZE - ELITE_COUNT; i++) {
        newPop.add(population.get(i));
    }

    population = newPop;
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
