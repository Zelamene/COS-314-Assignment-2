import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class GeneticAlgorithm {

    int POPULATION_SIZE = 100;
    int MAX_CAPACITY = -1;
    int NUM_ITEMS = -1;
    int TOURNAMENT_SIZE = 3;
    int FIT_PARENTS_SIZE = 40;

    double MUTATION_RATE = 0.01;
    double CROSSOVER_RATE = 0.6;
    double RECPLACE_COUNT = 40;

    String filename;
    static LinkedHashMap<Integer, Integer> weightValues = new LinkedHashMap<>();
    List<Item> items = new ArrayList<>();
    List<Individual> initPop = new ArrayList<>();

    List<Individual> fitIndividuals = new ArrayList<>();
    List<Individual> offspring = new ArrayList<>();

    GeneticAlgorithm(String filename) throws FileNotFoundException, IOException {
        this.filename = filename;
        loadFromFile();
        initialisePopulation();
        hostTournament();
        printStuff();
    }

    void runGA(int generations){
        initialisePopulation();
    }

    void printStuff() {

        for (Individual indv : fitIndividuals) {
            System.out.println(indv.toString());
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
            computeValidity(indv);
            computeFitnessScore(indv);

        }
    }

    int randomizeBit() {
        return (int) (Math.random() * 2);
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

        int i = (int) (random() * indv1.chromosome.size());
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

    double random() {
        return Math.random();
    }

    int randomIndex() {
        return (int) (Math.random() * initPop.size());
    }

    Individual fitnessTournament() {
        int[] players = { randomIndex(), randomIndex(), randomIndex() };
        Individual indv = initPop.get(players[0]);
        Individual fittest = indv;

        for (int i = 1; i < 3; i++) {
            Individual indv2 = initPop.get(players[i]);
            if (indv2.fitnessScore > fittest.fitnessScore) {// ask if they should be allwed to be parents since they did
                                                            // get demotedn by the penatly socre
                fittest = indv2;
            }
        }

        return fittest;
    }

    void hostTournament() {
        for (int i = 0; i < FIT_PARENTS_SIZE; i++) {
            fitIndividuals.add(fitnessTournament());
        }
    }

    void replace() {
        // steady state
        // replace unfit 40 inaavlid? or valid, or dont care??

        //worst first
        initPop.sort((a,b) ->  a.fitnessScore - b.fitnessScore);

        // sort offspring by best
        offspring.sort((a, b) -> b.fitnessScore - a.fitnessScore);\


        int count = 0;
        for (int i = 0; i < RECPLACE_COUNT; i++) {
            initPop.set(i, offspring.get(i));
        }
    }
}
