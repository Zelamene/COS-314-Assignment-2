import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class Main {
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
            for (String file : files) {
                GeneticAlgorithm ga = new GeneticAlgorithm(file, 29);

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println(e.getMessage());
            e.printStackTrace();

        }
    }
}
