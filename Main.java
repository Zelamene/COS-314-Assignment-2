import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter random seed: ");
        long userSeed = scanner.nextLong();
        scanner.nextLine();

        System.out.print("Enter directory path that contains instances: ");
        String dirPath = scanner.nextLine();

        scanner.close();

        Map<String, Double> knownOptima = new HashMap<>();
        knownOptima.put("f1_l-d_kp_10_269", 295.0);
        knownOptima.put("f2_l-d_kp_20_878", 1024.0);
        knownOptima.put("f3_l-d_kp_4_20", 35.0);
        knownOptima.put("f4_l-d_kp_4_11", 23.0);
        knownOptima.put("f5_l-d_kp_15_375", 481.0694);
        knownOptima.put("f6_l-d_kp_10_60", 52.0);
        knownOptima.put("f7_l-d_kp_7_50", 107.0);
        knownOptima.put("f8_l-d_kp_23_10000", 9767.0);
        knownOptima.put("f9_l-d_kp_5_80", 130.0);
        knownOptima.put("f10_l-d_kp_20_879", 1025.0);
        knownOptima.put("knapPI_1_100_1000_1", 9147.0);
        ;

        List<File> instanceFiles = getInstanceFiles(dirPath);
        if (instanceFiles.isEmpty()) {
            System.err.println("No instance files found in " + dirPath);
            return;
        }

        Map<String, Map<String, Result>> allResults = new LinkedHashMap<>();

        for (File instanceFile : instanceFiles) {
            String instanceName = instanceFile.getName();
            double knownOpt = knownOptima.getOrDefault(instanceName, -1.0);

            try {
                KnapsackInstance instance = KnapsackParser.parse(instanceFile.getAbsolutePath());

                // ILS
                IteratedLocalSearch ils = new IteratedLocalSearch(instance);
                ils.setMaxIterations(100);
                ils.setPerturbationStrength(3);
                ils.setUseAdaptiveStrength(true);
                ils.setRandomSeed(userSeed);

                Solution initial = InitialSolutionGenerator.generate(instance);
                long ilsStart = System.nanoTime();
                Solution bestILS = ils.run(initial);
                long ilsEnd = System.nanoTime();
                double ilsRuntime = (ilsEnd - ilsStart) / 1e9;

                // GA
                GeneticAlgorithm ga = new GeneticAlgorithm(instance, userSeed);
                long gaStartTime = System.nanoTime();
                double bestGAValue = ga.run();
                long gaEndTime = System.nanoTime();
                double gaRuntime = (gaEndTime - gaStartTime) / 1e9;

                // Store
                Map<String, Result> algoResults = new HashMap<>();
                algoResults.put("ILS", new Result(bestILS.totalValue, ilsRuntime));
                algoResults.put("GA", new Result(bestGAValue, gaRuntime));
                allResults.put(instanceName, algoResults);

            } catch (Exception e) {
                System.err.println("Error processing " + instanceName + ": " + e.getMessage());
            }
        }

        printFinalTable(allResults, knownOptima, userSeed);
    }

    private static List<File> getInstanceFiles(String dirPath) {
        List<File> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dirPath))) {
            for (Path entry : stream) {
                if (!entry.toString().toLowerCase().endsWith(".xlsx")) {
                    File f = entry.toFile();
                    files.add(f);
                }

            }
        } catch (IOException e) {
            System.err.println("Error reading directory: " + e.getMessage());
        }
        files.sort(Comparator.comparing(File::getName));
        return files;
    }

    private static void printFinalTable(Map<String, Map<String, Result>> allResults,
            Map<String, Double> knownOptima,
            long seed) {
        System.out.println("\n");
        System.out.println("Table: Comparison of GA and Iterated Local Search");
        System.out.println(
                "=========================================================================================================");
        System.out.printf("%-35s | %-10s | %-12s | %-15s | %-15s | %-10s%n",
                "Problem Instance", "Algorithm", "Seed Value", "Best Solution", "Known Optimum", "Runtime (s)");
        System.out.println("");

        for (Map.Entry<String, Map<String, Result>> entry : allResults.entrySet()) {
            String instanceName = entry.getKey();
            Map<String, Result> algoRes = entry.getValue();
            double knownOpt = knownOptima.getOrDefault(instanceName, -1.0);
            String optStr = knownOpt >= 0 ? String.format("%.4f", knownOpt) : "N/A";

            // ILS row
            Result ilsRes = algoRes.get("ILS");
            System.out.printf("%-35s | %-10s | %-12d | %-15.4f | %-15s | %-10.3f%n",
                    instanceName, "ILS", seed, ilsRes.bestValue, optStr, ilsRes.runtime);

            // GA row
            Result gaRes = algoRes.get("GA");
            System.out.printf("%-35s | %-10s | %-12d | %-15.4f | %-15s | %-10.3f%n",
                    instanceName, "GA", seed, gaRes.bestValue, optStr, gaRes.runtime);

            System.out.println("");
        }
        System.out.println(
                "=========================================================================================================");
    }

    static class Result {
        double bestValue;
        double runtime;

        Result(double bestValue, double runtime) {
            this.bestValue = bestValue;
            this.runtime = runtime;
        }
    }
}