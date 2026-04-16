import java.io.*;
import java.util.*;

//Represents a single object I might put in the knapsacj
 class Item {
    public final int index; // The item's ID number
    public final double value; // How much this item is worth
    public final double weight; // How heavy is the utem

    public Item(int index, double value, double weight) {
        this.index = index;
        this.value = value;
        this.weight = weight;
    }

    public double getValue(){
        return value;
    }
    public double getWeight(){
        return weight;
    }
}

/*
 * Holds all the information about one knapsack problme
 * Simple analogy: Like a shoppinh container with a weight limit, and a
 * warehouse full of
 * boxes you can choose from
 */
class KnapsackInstance {
    public final String filename; // where the data came from
    public final int numItems; // how many items total are available
    public final double capacity; // Maximum weight the knapsack can hold
    public final List<Item> items; // A list of all available items

    public KnapsackInstance(String filename, int numItems,
            double capacity, List<Item> items) {
        this.filename = filename;
        this.numItems = numItems;
        this.capacity = capacity;
        this.items = items;
    }
}

/*
 * Reads knapsack problem data from a tect file
 * How it works:
 * 1) Opens the file
 * 2) Reads first line to get numnber of items and capacity
 * 3) Reads each following line to get item values and weightts
 * 4) Creates Item objkects for each line
 * 5) Packages everything into a KnapsackInstance
 * Simple analohy: Likw a waiter reading your order from a notepad and
 * converting it into actual
 * food items in the kitchen
 */
class KnapsackParser {

    public static KnapsackInstance parse(String filepath) throws IOException {
        File file = new File(filepath);
        if (!file.exists())
            throw new FileNotFoundException("File not found: " + filepath);

        List<Item> items = new ArrayList<>();
        int numItems = 0;
        double capacity = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String[] header = reader.readLine().trim().split("\\s+");
            numItems = Integer.parseInt(header[0]);
            capacity = Double.parseDouble(header[1]);

            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                String[] parts = line.split("\\s+");
                double value = Double.parseDouble(parts[0]);
                double weight = Double.parseDouble(parts[1]);
                items.add(new Item(index++, value, weight));
            }
        }
        return new KnapsackInstance(file.getName(), numItems, capacity, items);
    }
}

/*
 * Represents one possible way to fill the knapsack
 * . selected[]: A true/false array where selected[i] - true means item i is in
 * the knapsack
 * . totalValue: Sum of values of all selected items
 * . totalWeight: Sum of weights of all selected items
 * . capacity: The weight limit
 * What it should do:
 * .Check if the solution is feasible
 * .Count how many items are selected
 * .Create a new solution by flipping one item
 * .Create a new solution by swapping items
 */
class Solution {
    public final boolean[] selected;
    public final double totalValue;
    public final double totalWeight;

    private final double capacity;

    public Solution(boolean[] selected, double totalValue, double totalWeight) {
        this.selected = selected.clone();
        this.totalValue = totalValue;
        this.totalWeight = totalWeight;
        this.capacity = -1;
    }

    private Solution(boolean[] selected, double totalValue, double totalWeight, double capacity) {
        this.selected = selected.clone();
        this.totalValue = totalValue;
        this.totalWeight = totalWeight;
        this.capacity = capacity;
    }

    public boolean isFeasible(double capacity) {
        return totalWeight <= capacity;
    }

    public boolean isFeasible() {
        return isFeasible(capacity);
    }

    public int countSelected() {
        int count = 0;
        for (boolean b : selected)
            if (b)
                count++;
        return count;
    }

    // Create a new solution by flipping one item (add or remove)
    public Solution flip(int index, List<Item> items, double capacity) {
        boolean[] newSelected = selected.clone();
        double newValue = totalValue;
        double newWeight = totalWeight;

        if (newSelected[index]) {
            // Remove item
            newValue -= items.get(index).value;
            newWeight -= items.get(index).weight;
            newSelected[index] = false;
        } else {
            // Add item
            newValue += items.get(index).value;
            newWeight += items.get(index).weight;
            newSelected[index] = true;
        }

        return new Solution(newSelected, newValue, newWeight, capacity);
    }

    // Create a new solution by swapping two items (remove i, add j)
    public Solution swap(int removeIdx, int addIdx, List<Item> items, double capacity) {
        if (removeIdx == addIdx)
            return this;

        boolean[] newSelected = selected.clone();
        double newValue = totalValue;
        double newWeight = totalWeight;

        // Remove item at removeIdx if it's selected
        if (newSelected[removeIdx]) {
            newValue -= items.get(removeIdx).value;
            newWeight -= items.get(removeIdx).weight;
            newSelected[removeIdx] = false;
        }

        // Add item at addIdx if it's not already selected
        if (!newSelected[addIdx]) {
            newValue += items.get(addIdx).value;
            newWeight += items.get(addIdx).weight;
            newSelected[addIdx] = true;
        }

        return new Solution(newSelected, newValue, newWeight, capacity);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Value : %.4f%n", totalValue));
        sb.append(String.format("Weight: %.4f%n", totalWeight));
        sb.append("Items : [");
        boolean first = true;
        for (int i = 0; i < selected.length; i++) {
            if (selected[i]) {
                if (!first)
                    sb.append(", ");
                sb.append(i);
                first = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public Solution clone() {
        return new Solution(selected, totalValue, totalWeight, capacity);
    }
}

/*
 * Creates the FIRST solution before ILS begins its improvement process.
 * Strategy I should use: Greedy by value/weigt rtatio
 * How it should work:
 * - Calculate value/weight ration for every item
 * - Sort items from best ratio to worst
 * - Go through items in order:
 * -it item fits, add it to knapsack
 * -if item doesn't fit, skip it and try next one
 * Why not just use a random solution?: A good starting point means ILS doesn;t
 * waste time climbing out of a terrible solution
 */
class InitialSolutionGenerator {
    public static Solution generate(KnapsackInstance instance) {
        int n = instance.numItems;
        double capacity = instance.capacity;

        List<Item> sorted = new ArrayList<>(instance.items);
        sorted.sort((a, b) -> {
            double ratioA = a.value / a.weight;
            double ratioB = b.value / b.weight;
            return Double.compare(ratioB, ratioA);
        });

        boolean[] selected = new boolean[n];
        double totalWeight = 0;
        double totalValue = 0;

        for (Item item : sorted) {
            if (totalWeight + item.weight <= capacity) {
                selected[item.index] = true;
                totalWeight += item.weight;
                totalValue += item.value;
            }
        }

        return new Solution(selected, totalValue, totalWeight);
    }
}

/*
 * Takes a solution and makes it better by exploring nearby solutions
 * Strategy: First-improvement hill climbing
 * How it should work:
 * - Start with a solution
 * - Try flipping one item at a time
 * -As soon as you find a flip that improves value AND keeps feasibility, accept
 * it
 * - Repeat from step 2 until no improvement is possible
 */
class LocalSearch {

    private final KnapsackInstance instance;
    private int improvements;
    private int evaluations;

    public LocalSearch(KnapsackInstance instance) {
        this.instance = instance;
        this.improvements = 0;
        this.evaluations = 0;
    }

    public Solution improve(Solution solution) {
        Solution current = solution;
        boolean improved;

        do {
            improved = false;

            for (int i = 0; i < instance.numItems; i++) {
                Solution neighbor = current.flip(i, instance.items, instance.capacity);
                evaluations++;

                if (neighbor.isFeasible(instance.capacity) &&
                        neighbor.totalValue > current.totalValue) {
                    current = neighbor;
                    improvements++;
                    improved = true;
                    break;
                }
            }

        } while (improved);

        return current;
    }

    public int getImprovements() {
        return improvements;
    }

    public int getEvaluations() {
        return evaluations;
    }

    public void reset() {
        improvements = 0;
        evaluations = 0;
    }
}

/*
 * Takes a good solution and "shakes it" to escape local optima.
 * Local search can get stuck on a goob but not best solution Perturbation jumps
 * to a different area of the search space.
 */
class Perturbation {

    private final KnapsackInstance instance;
    private final Random random;

    public enum Strategy {
        RANDOM_FLIPS,
        DOUBLE_FLIPS,
        K_OPT,
        MUTATION_RATE
    }

    private Strategy strategy = Strategy.RANDOM_FLIPS;
    private int perturbationStrength = 3; // Number of changes

    public Perturbation(KnapsackInstance instance) {
        this.instance = instance;
        this.random = new Random();
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public void setStrength(int strength) {
        this.perturbationStrength = Math.max(1, strength);
    }

    public Solution perturb(Solution solution) {
        switch (strategy) {
            case RANDOM_FLIPS:
                return randomFlips(solution);
            case DOUBLE_FLIPS:
                return doubleFlips(solution);
            case K_OPT:
                return kOpt(solution);
            case MUTATION_RATE:
                return mutationRate(solution);
            default:
                return randomFlips(solution);
        }
    }

    private Solution randomFlips(Solution solution) {
        Solution current = solution;

        for (int i = 0; i < perturbationStrength; i++) {
            int index = random.nextInt(instance.numItems);
            Solution neighbor = current.flip(index, instance.items, instance.capacity);

            // Only accept if feasible
            if (neighbor.isFeasible(instance.capacity)) {
                current = neighbor;
            } else {
                // Try to repair by removing random items
                current = repair(current);
            }
        }

        return current;
    }

    private Solution doubleFlips(Solution solution) {
        Solution current = solution;

        for (int i = 0; i < perturbationStrength; i++) {

            List<Integer> selectedIndices = new ArrayList<>();
            for (int j = 0; j < instance.numItems; j++) {
                if (current.selected[j])
                    selectedIndices.add(j);
            }

            List<Integer> unselectedIndices = new ArrayList<>();
            for (int j = 0; j < instance.numItems; j++) {
                if (!current.selected[j])
                    unselectedIndices.add(j);
            }

            if (!selectedIndices.isEmpty() && !unselectedIndices.isEmpty()) {
                int removeIdx = selectedIndices.get(random.nextInt(selectedIndices.size()));
                int addIdx = unselectedIndices.get(random.nextInt(unselectedIndices.size()));

                Solution neighbor = current.swap(removeIdx, addIdx, instance.items, instance.capacity);
                if (neighbor.isFeasible(instance.capacity)) {
                    current = neighbor;
                }
            }
        }

        return current;
    }

    private Solution kOpt(Solution solution) {
        Solution current = solution;

        for (int k = 0; k < perturbationStrength; k++) {

            int operation = random.nextInt(3);

            switch (operation) {
                case 0: // Single flip
                    current = randomFlips(current);
                    break;
                case 1: // Double flip (swap)
                    current = doubleFlips(current);
                    break;
                case 2: // Remove multiple, add multiple
                    current = multiChange(current);
                    break;
            }
        }

        return current;
    }

    private Solution multiChange(Solution solution) {
        Solution current = solution;

        int toRemove = 1 + random.nextInt(perturbationStrength);
        int toAdd = 1 + random.nextInt(perturbationStrength);

        for (int i = 0; i < toRemove; i++) {
            List<Integer> selected = new ArrayList<>();
            for (int j = 0; j < instance.numItems; j++) {
                if (current.selected[j])
                    selected.add(j);
            }
            if (selected.isEmpty())
                break;

            int removeIdx = selected.get(random.nextInt(selected.size()));
            current = current.flip(removeIdx, instance.items, instance.capacity);
        }

        for (int i = 0; i < toAdd; i++) {
            List<Integer> unselected = new ArrayList<>();
            for (int j = 0; j < instance.numItems; j++) {
                if (!current.selected[j])
                    unselected.add(j);
            }
            if (unselected.isEmpty())
                break;

            int addIdx = unselected.get(random.nextInt(unselected.size()));
            Solution neighbor = current.flip(addIdx, instance.items, instance.capacity);
            if (neighbor.isFeasible(instance.capacity)) {
                current = neighbor;
            }
        }

        return current;
    }

    private Solution mutationRate(Solution solution) {
        double mutationProb = 0.1 * (perturbationStrength / 3.0); // Scale probability

        Solution current = solution;

        for (int i = 0; i < instance.numItems; i++) {
            if (random.nextDouble() < mutationProb) {
                Solution neighbor = current.flip(i, instance.items, instance.capacity);
                if (neighbor.isFeasible(instance.capacity)) {
                    current = neighbor;
                }
            }
        }

        return current;
    }

    private Solution repair(Solution solution) {
        Solution current = solution;

        while (!current.isFeasible(instance.capacity)) {

            List<Integer> selected = new ArrayList<>();
            for (int i = 0; i < instance.numItems; i++) {
                if (current.selected[i])
                    selected.add(i);
            }

            if (selected.isEmpty())
                break;

            int toRemove = selected.get(random.nextInt(selected.size()));
            current = current.flip(toRemove, instance.items, instance.capacity);
        }

        return current;
    }

    public void setRandomSeed(int seed) {
        random.setSeed(seed);
    }
}

/*
 * Main ILS
 * The ILS loop:
 * 1. Start with solution S
 * 2. Perturb S to get S' (shake it)
 * 3. Apply local search to S' to get improved S''
 * 4. If S'' is better than best found, save it
 * 5. Possibly accept S'' even if worse (with some probability)
 * 6. Go to step 1
 * - maxIterations: How many times to repeat the loop
 * - maxNoImprovement: Stop if no improvement after this many iteratiobs
 * - useAdaptiveStrenth: Automatically adjust perturbations strenght
 * Acceptance criteria:
 * - Always accept if solution is better than best
 * - Sometimes accept worse solution to explore
 */
class IteratedLocalSearch {

    private final KnapsackInstance instance;
    private final LocalSearch localSearch;
    private final Perturbation perturbation;

    // ILS parameters
    private int maxIterations = 100;
    private int maxNoImprovement = 20; // Stop if no improvement for this many iterations
    private boolean useAdaptiveStrength = true;

    // Statistics
    private int iterations;
    private int perturbations;
    private double bestValue;
    private long startTime;
    private long endTime;

    public IteratedLocalSearch(KnapsackInstance instance) {
        this.instance = instance;
        this.localSearch = new LocalSearch(instance);
        this.perturbation = new Perturbation(instance);
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public void setMaxNoImprovement(int maxNoImprovement) {
        this.maxNoImprovement = maxNoImprovement;
    }

    public void setPerturbationStrategy(Perturbation.Strategy strategy) {
        perturbation.setStrategy(strategy);
    }

    public void setPerturbationStrength(int strength) {
        perturbation.setStrength(strength);
    }

    public void setUseAdaptiveStrength(boolean useAdaptiveStrength) {
        this.useAdaptiveStrength = useAdaptiveStrength;
    }

    public Solution run(Solution initialSolution) {
        startTime = System.currentTimeMillis();
        iterations = 0;
        perturbations = 0;

        // Start with initial solution
        Solution bestSolution = initialSolution;
        Solution currentSolution = initialSolution;
        bestValue = currentSolution.totalValue;

        System.out.println("\nStarting ILS...");
        System.out.println("Initial solution value: " + bestValue);
        System.out.println("----------------------------------------");

        int noImprovementCount = 0;
        int currentStrength = 3; // Initial perturbation strength

        for (int iter = 0; iter < maxIterations && noImprovementCount < maxNoImprovement; iter++) {
            iterations++;

            // Step 1: Perturb current solution
            if (useAdaptiveStrength) {
                // Increase perturbation strength if stuck
                if (noImprovementCount > maxNoImprovement / 2) {
                    currentStrength = Math.min(10, currentStrength + 1);
                    perturbation.setStrength(currentStrength);
                } else {
                    currentStrength = Math.max(1, currentStrength - 1);
                    perturbation.setStrength(currentStrength);
                }
            }

            Solution perturbedSolution = perturbation.perturb(currentSolution);
            perturbations++;

            // Step 2: Local search from perturbed solution
            Solution improvedSolution = localSearch.improve(perturbedSolution);

            // Step 3: Acceptance criterion (accept if better or with some probability)
            if (improvedSolution.totalValue > bestSolution.totalValue) {
                bestSolution = improvedSolution;
                currentSolution = improvedSolution;
                bestValue = improvedSolution.totalValue;
                noImprovementCount = 0;

                System.out.printf("Iteration %3d: New best = %.4f (improvements: %d, strength: %d)%n",
                        iterations, bestValue, localSearch.getImprovements(), currentStrength);
            } else if (improvedSolution.totalValue > currentSolution.totalValue) {
                currentSolution = improvedSolution;
                noImprovementCount++;
            } else {
                // Random walk acceptance with decreasing probability
                double temperature = 1.0 - (double) iterations / maxIterations;
                if (Math.random() < temperature * 0.1) {
                    currentSolution = improvedSolution;
                }
                noImprovementCount++;
            }

            // Reset local search statistics for next iteration
            localSearch.reset();
        }

        endTime = System.currentTimeMillis();

        return bestSolution;
    }

    public void printStatistics() {
        System.out.println("\n=== ILS Statistics ===");
        System.out.printf("Total iterations: %d%n", iterations);
        System.out.printf("Total perturbations: %d%n", perturbations);
        System.out.printf("Local search improvements: %d%n", localSearch.getImprovements());
        System.out.printf("Local search evaluations: %d%n", localSearch.getEvaluations());
        System.out.printf("Final best value: %.4f%n", bestValue);
        System.out.printf("Execution time: %.3f seconds%n", (endTime - startTime) / 1000.0);
    }

    public void setRandomSeed(long seed) {
        // Pass the seed to the perturbation classperturbation.setRandomSeed(seed);
    }
}


/*
 * How everything works together
 * 1. USER → Enters seed (e.g., 42)
 * 
 * 2. MAIN → For each dataset:
 * 
 * 3. PARSER → Reads file → Creates INSTANCE
 * 
 * 4. INITIAL GENERATOR → Makes first solution (greedy)
 * 
 * 5. ILS → Repeatedly:
 * ├── PERTURBATION → Shakes solution
 * └── LOCAL SEARCH → Improves shaken solution
 * 
 * 6. SOLUTION → Stores best found answer
 * 
 * 7. MAIN → Prints results in table format
 */