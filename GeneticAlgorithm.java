import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;

public class GeneticAlgorithm {

    Integer POPULATION_SIZE = 100;
    Integer MAX_CAPACITY = -1;
    Integer NUM_ITEMS = -1;

    String filename;
    LinkedHashMap<Integer, Integer> weightValues = new LinkedHashMap<>();

    GeneticAlgorithm(String filename) throws FileNotFoundException, IOException {
        this.filename = filename;
        loadFromFile();
        printStuff();
    }

    void printStuff() {
        System.out.println("Number of items -> " + NUM_ITEMS);
        System.out.println("Max weight -> " + MAX_CAPACITY);
        for (Integer iterable_element : weightValues.keySet()) {
            System.out.print(iterable_element + " -> " + weightValues.get(iterable_element) + "\n");
        }

    }

    void loadFromFile() throws FileNotFoundException, IOException {
        String filePath = "Knapsack Instances/" + filename;
        BufferedReader br = new BufferedReader(new FileReader(filePath));

        String line = null;
        int index = 0;

        while ((line = br.readLine()) != null) {
            index++;

            int indx = line.indexOf(" ") + 1;
            if (index == 1) {

                MAX_CAPACITY = Integer.parseInt(line.substring(indx).trim());
                NUM_ITEMS = Integer.parseInt(line.substring(0, indx).trim());

            } else {
                weightValues.put(Integer.parseInt(line.substring(indx).trim()),
                        Integer.parseInt(line.substring(0, indx).trim()));

            }
        }
    }
}
