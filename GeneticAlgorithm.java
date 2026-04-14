import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GeneticAlgorithm {

    int POPULATION_SIZE = 100;
    int MAX_CAPACITY = -1;
    int NUM_ITEMS = -1;
    int TOURNAMENT_SIZE = 3;

    String filename;
    static LinkedHashMap<Integer, Integer> weightValues = new LinkedHashMap<>();
    List<Item> items = new ArrayList<>();
    List<Individual> initPop = new ArrayList<>();

    List<Individual> fitIndivuals = new ArrayList<>();

    GeneticAlgorithm(String filename) throws FileNotFoundException, IOException {
        this.filename = filename;
        loadFromFile();
        initialisePopulation();
        printStuff();
    }

    void printStuff() {

        for (Individual indv : initPop) {
            System.out.println(indv.toString());
        }

    }

    void initialisePopulation() {

        for (int i = 0; i < MAX_CAPACITY; i++) {
            List<Integer> list = new ArrayList<>();
            for (int j = 0; j < NUM_ITEMS; j++) {
                list.add(randomizeBit());
            }
            Individual indv = new Individual(list);
            initPop.add(indv);
            computeValidity(indv);
            computeFitnessScore(indv);

        }
    }

    int randomizeBit() {
        return (int) (Math.random() * 2);
    }

    void computeValidity(Individual individual_1) {
        List<Integer> chromosome = individual_1.chromosome;
        int sumWeight = 0;
        for (int i = 0; i < chromosome.size(); i++) {
            if (chromosome.get(i).equals(1)) {
                sumWeight += items.get(i).getWeight();
                if (sumWeight > MAX_CAPACITY) {
                    individual_1.setValidity(false);
                }
            }
        }
        individual_1.totalWeight = sumWeight;
        System.out.println("\nsumWeight " + sumWeight);

    }

    int computeTotalValues(Individual individual_1) {
        List<Integer> chromosome = individual_1.chromosome;
        int sumValues = 0;
        for (int i = 0; i < chromosome.size(); i++) {
            if (chromosome.get(i).equals(1)) {
                sumValues += items.get(i).getValue();
            }
        }

        return sumValues;
    }

    void computeFitnessScore(Individual individual_1) {
        List<Integer> chromosome = individual_1.chromosome;
        int sumValues = 0;
        if (individual_1.isValid) {

            individual_1.setFitnessScore(computeTotalValues(individual_1));
        } else {
            // ftnesscore = total value - (penaltycoeeficient * exceedvalue)
            final int penaltyCoefficient = Collections.max(weightValues.values())
                    / Collections.min(weightValues.keySet());
            Integer score = computeTotalValues(individual_1)
                    - (penaltyCoefficient * (individual_1.totalWeight - MAX_CAPACITY));
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

            Integer secondValue = Integer.parseInt(line.substring(indx).trim());
            Integer firstValue = Integer.parseInt(line.substring(0, indx).trim());

            if (index == 1) {

                MAX_CAPACITY = secondValue;
                NUM_ITEMS = firstValue;

            } else {
                weightValues.put(secondValue,
                        firstValue);

                items.add(new Item(secondValue, firstValue));
            }

        }
        br.close();

    }

    int randomIndex() {
        return (int) Math.random() * (100);
    }

    Individual fitnessTournament() {
        int[] players = { randomIndex(), randomIndex(), randomIndex() };
        Individual indv = initPop.get(players[0]);
        Individual fittest = indv;

        for (int i = 1; i < 3; i++) {
            Individual indv2 = initPop.get(players[i]);
            if (indv2.fitnessScore > fittest.fitnessScore) {
                fittest = indv2;
            }
        }

        return fittest;
    }
}
