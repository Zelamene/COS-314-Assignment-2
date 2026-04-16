import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class Main {

    static long userSeed;
    private static final Map<String, Double> KNOWN_OPTIMUMS = new LinkedHashMap<>();
    static {
        KNOWN_OPTIMUMS.put("f1_l-d_kp_10_269", 295.0);
        KNOWN_OPTIMUMS.put("f2_l-d_kp_20_878", 1024.0);
        KNOWN_OPTIMUMS.put("f3_l-d_kp_4_20", 35.0);
        KNOWN_OPTIMUMS.put("f4_l-d_kp_4_11", 23.0);
        KNOWN_OPTIMUMS.put("f5_l-d_kp_15_375", 481.0694);
        KNOWN_OPTIMUMS.put("f6_l-d_kp_10_60", 52.0);
        KNOWN_OPTIMUMS.put("f7_l-d_kp_7_50", 107.0);
        KNOWN_OPTIMUMS.put("f8_l-d_kp_23_10000", 9767.0);
        KNOWN_OPTIMUMS.put("f9_l-d_kp_5_80", 130.0);
        KNOWN_OPTIMUMS.put("f10_l-d_kp_20_879", 1025.0);
        KNOWN_OPTIMUMS.put("knapPI_1_100_1000_1", 9147.0);
    }

    private static final String DATA_DIR = "Knapsack Instances/";
    private static final String[] FILES = {
            "f1_l-d_kp_10_269",
            "f2_l-d_kp_20_878",
            "f3_l-d_kp_4_20",
            "f4_l-d_kp_4_11",
            "f5_l-d_kp_15_375",
            "f6_l-d_kp_10_60",
            "f7_l-d_kp_7_50",
            "f8_l-d_kp_23_10000",
            "f9_l-d_kp_5_80",
            "f10_l-d_kp_20_879",
            "knapPI_1_100_1000_1"
    };

    // ILS configuration
    private static final int ILS_ITERATIONS = 100;
    private static final Perturbation.Strategy PERTURBATION_STRATEGY = Perturbation.Strategy.RANDOM_FLIPS;
    private static final int PERTURBATION_STRENGTH = 3;

    // Class to store results for each instance
    static class ILSResult {
        String instance;
        double bestValue;
        double runtime;
        long seedUsed;

        ILSResult(String instance, double bestValue, double runtime, long seedUsed) {
            this.instance = instance;
            this.bestValue = bestValue;
            this.runtime = runtime;
            this.seedUsed = seedUsed;
        }
    }

    static void writeHeader() throws IOException {
        try (FileWriter fw = new FileWriter("results.txt", false);
                BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(String.format("%-35s %-15s %-12s %-15s %-15s %s%n",
                    "Problem Instance", "Algorithm", "Seed Value",
                    "Best Solution", "Known Optimum", "Runtime (seconds)"));
        }
    }

    public static void main(String[] args) {
        try {
            writeHeader();
            String[] files = { "f1_l-d_kp_10_269", "f2_l-d_kp_20_878", "f3_l-d_kp_4_20", "f4_l-d_kp_4_11",
                    "f5_l-d_kp_15_375", "f6_l-d_kp_10_60", "f7_l-d_kp_7_50", "f8_l-d_kp_23_10000", "f9_l-d_kp_5_80",
                    "f10_l-d_kp_20_879", "knapPI_1_100_1000_1" };
            System.out.print("Enter seed value for random number generator: ");
            Scanner scanner = new Scanner(System.in);
            
            System.out.printf("\nUsing seed: %d%n", userSeed);

            userSeed = scanner.nextLong();
            scanner.close();
            for (String file : files) {
                GeneticAlgorithm ga = new GeneticAlgorithm(file, userSeed);

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println(e.getMessage());
            e.printStackTrace();

        }
        System.out.println("========================================================");
        System.out.println("     ITERATED LOCAL SEARCH FOR 0/1 KNAPSACK            ");
        System.out.println("========================================================");

        System.out.printf("ILS Config: %d iterations, %s perturbation, strength %d%n%n",
                ILS_ITERATIONS, PERTURBATION_STRATEGY, PERTURBATION_STRENGTH);

        List<ILSResult> results = new ArrayList<>();

        for (String filename : FILES) {
            try {
                // Parse dataset once
                KnapsackInstance instance = KnapsackParser.parse(DATA_DIR + filename);

                // Set random seed for reproducibility
                Random random = new Random(userSeed);

                // Generate initial solution (greedy - deterministic)
                Solution initial = InitialSolutionGenerator.generate(instance);

                // Run ILS with user seed
                IteratedLocalSearch ils = new IteratedLocalSearch(instance);
                ils.setMaxIterations(ILS_ITERATIONS);
                ils.setPerturbationStrategy(PERTURBATION_STRATEGY);
                ils.setPerturbationStrength(PERTURBATION_STRENGTH);
                ils.setUseAdaptiveStrength(true);
                ils.setRandomSeed(userSeed);

                long startTime = System.nanoTime();
                Solution best = ils.run(initial);
                long endTime = System.nanoTime();

                double runtime = (endTime - startTime) / 1e9;

                results.add(new ILSResult(filename, best.totalValue, runtime, userSeed));

            } catch (Exception e) {
                System.out.println("\n[ERROR] " + filename + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Print the formatted table
        printTable(results);

    }

    private static void printTable(List<ILSResult> results) {
        System.out.println("\n");
        System.out.println("Table 1: Comparison of GA and Iterated Local Search on 10 knapsack problem instances");
        System.out.println(
                "=========================================================================================================");
        System.out.printf("%-30s | %-10s | %-12s | %-12s | %-12s | %-10s%n",
                "Problem Instance", "Algorithm", "Seed Value", "Best Solution", "Known Optimum", "Runtime (s)");
        System.out.println(
                "---------------------------------------------------------------------------------------------------------");

        for (ILSResult result : results) {
            String instanceName = formatInstanceName(result.instance);
            double knownOpt = KNOWN_OPTIMUMS.getOrDefault(result.instance, -1.0);
            String optString = knownOpt > 0 ? String.format("%.4f", knownOpt) : "N/A";

            System.out.printf("%-30s | %-10s | %-12d | %-12.4f | %-12s | %-10.3f%n",
                    instanceName,
                    "ILS",
                    result.seedUsed,
                    result.bestValue,
                    optString,
                    result.runtime);
        }

        System.out.println(
                "=========================================================================================================");

        // Print gap analysis
        System.out.println("\n");
        System.out.println("Table 2: Gap to Known Optimum");
        System.out.println("================================================");
        System.out.printf("%-30s | %-12s | %-12s%n",
                "Problem Instance", "Best Value", "Gap to Opt (%)");
        System.out.println("------------------------------------------------");

        for (ILSResult result : results) {
            String instanceName = formatInstanceName(result.instance);
            double knownOpt = KNOWN_OPTIMUMS.getOrDefault(result.instance, -1.0);

            if (knownOpt > 0) {
                double gap = (knownOpt - result.bestValue) / knownOpt * 100;
                System.out.printf("%-30s | %-12.4f | %-12.2f%%%n",
                        instanceName, result.bestValue, gap);
            } else {
                System.out.printf("%-30s | %-12.4f | %-12s%n",
                        instanceName, result.bestValue, "N/A");
            }
        }
        System.out.println("================================================");
    }

    private static String formatInstanceName(String filename) {

        return filename.replace("_", " ").replace("f", "f ");
    }
}
