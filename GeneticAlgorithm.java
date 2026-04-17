import java.io.*;
import java.util.*;

public class GeneticAlgorithm {

    // Parameters
    private static final int POPULATION_SIZE = 100;
    private static final int TOURNAMENT_SIZE = 3;
    private static final int REPLACE_COUNT = 40;
    private static final int GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.01;
    private static final double CROSSOVER_RATE = 0.6;
    private static final int ELITE_COUNT = 5;

    private final List<Item> items;
    private final double maxCapacity;
    private final int numItems;
    private final Random rand;
    private final double penaltyCoefficient;

    private List<Individual> population = new ArrayList<>();
    private List<Individual> offspring = new ArrayList<>();

    public GeneticAlgorithm(KnapsackInstance instance, long seed) {
        this.items = instance.items;
        this.maxCapacity = instance.capacity;
        this.numItems = items.size();
        this.rand = new Random(seed);

        double maxValue = items.stream().mapToDouble(Item::getValue).max().orElse(1);
        double minWeight = items.stream().mapToDouble(Item::getWeight).min().orElse(1);
        this.penaltyCoefficient = Math.max(1, maxValue / Math.max(1, minWeight));
    }

    public double run() {
        initialisePopulation();
        Individual best = null;

        for (int gen = 0; gen < GENERATIONS; gen++) {
            offspring.clear();

            // Produce offspring
            while (offspring.size() < REPLACE_COUNT) {
                Individual p1 = hostTournament();
                Individual p2 = hostTournament();
                crossover(p1, p2);
            }

            // Mutate and evaluate fitness
            for (Individual child : offspring) {
                mutate(child);
                computeFitness(child);
            }
            // replacemnt strategy
            replace();

            // Track best valid individual
            Individual bestIndividual = population.stream()
                    .filter(ind -> ind.isValid)
                    .max(Comparator.comparingDouble(ind -> ind.fitnessScore))
                    .orElse(null);

            if (bestIndividual != null && (best == null || bestIndividual.fitnessScore > best.fitnessScore)) {
                best = bestIndividual;
            }
        }

        return best != null ? best.totalValues : 0.0;
    }

    // Pupulation Initialisation
    private void initialisePopulation() {
        for (int i = 0; i < POPULATION_SIZE; i++) {
            List<Boolean> chromosome = new ArrayList<>(Collections.nCopies(numItems, false));
            List<Integer> indices = new ArrayList<>();
            for (int j = 0; j < numItems; j++)
                indices.add(j);
            Collections.shuffle(indices, rand);

            double currentWeight = 0;
            for (int idx : indices) {
                if (currentWeight + items.get(idx).getWeight() <= maxCapacity) {
                    chromosome.set(idx, true);
                    currentWeight += items.get(idx).getWeight();
                }
            }
            Individual ind = new Individual(chromosome);
            computeFitness(ind);
            population.add(ind);
        }
    }

    // Selection
    private Individual hostTournament() {
        Individual best = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Individual candidate = population.get(rand.nextInt(population.size()));
            if (best == null || candidate.fitnessScore > best.fitnessScore)
                best = candidate;
        }
        return best;
    }

    // Crossover
    private void crossover(Individual p1, Individual p2) {
        if (rand.nextDouble() > CROSSOVER_RATE) {
            offspring.add(new Individual(new ArrayList<>(p1.chromosome)));
            offspring.add(new Individual(new ArrayList<>(p2.chromosome)));
            return;
        }
        int point = rand.nextInt(p1.chromosome.size() + 1);

        List<Boolean> child2 = new ArrayList<>(p2.chromosome.subList(0, point));
        child2.addAll(p1.chromosome.subList(point, p1.chromosome.size()));

        List<Boolean> child1 = new ArrayList<>(p1.chromosome.subList(0, point));
        child1.addAll(p2.chromosome.subList(point, p2.chromosome.size()));

        offspring.add(new Individual(child1));
        offspring.add(new Individual(child2));
    }

    // Mutation
    private void mutate(Individual ind) {
        for (int i = 0; i < ind.chromosome.size(); i++) {
            if (rand.nextDouble() < MUTATION_RATE) {
                ind.chromosome.set(i, !ind.chromosome.get(i));
            }
        }
    }

    // Fitness evaluation
    private void computeFitness(Individual ind) {
        double totalWeight = 0, totalValue = 0;

        for (int i = 0; i < ind.chromosome.size(); i++) {
            if (ind.chromosome.get(i)) {
                totalWeight += items.get(i).getWeight();
                totalValue += items.get(i).getValue();
            }
        }
        ind.totalWeight = totalWeight;
        ind.totalValues = totalValue;
        ind.isValid = totalWeight <= maxCapacity;
        if (ind.isValid) {
            ind.fitnessScore = totalValue;
        } else {
            ind.fitnessScore = totalValue - penaltyCoefficient * (totalWeight - maxCapacity);
        }
    }

    // Replacement strategy (elitism + steady state)
    private void replace() {
        if (offspring.isEmpty())
            return;

        population.sort(Comparator.comparingDouble(a -> a.fitnessScore));
        offspring.sort((a, b) -> Double.compare(b.fitnessScore, a.fitnessScore));

        List<Individual> newPop = new ArrayList<>();

        // Elites
        for (int i = POPULATION_SIZE - ELITE_COUNT; i < POPULATION_SIZE; i++) {
            newPop.add(population.get(i));
        }
        // Best offspring
        for (int i = 0; i < REPLACE_COUNT; i++) {
            newPop.add(offspring.get(i));
        }
        // Remaining old individuals
        for (int i = REPLACE_COUNT; i < POPULATION_SIZE - ELITE_COUNT; i++) {
            newPop.add(population.get(i));
        }
        population = newPop;
    }

}