import java.util.List;

public class Individual {

    List<Integer> chromosome;
    Boolean isValid = true;
    Integer fitnessScore = 0;
    Integer totalWeight = 0;
    Integer totalValues=0;

    Individual(List<Integer> chromosome) {
        this.chromosome = chromosome;
    }

    void setValidity(Boolean isValid) {
        this.isValid = isValid;
    }

    void setFitnessScore(Integer fitness) {
        this.fitnessScore = fitness;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Integer gene : chromosome) {
            sb.append(gene + " ");

        }
        sb.append(isValid ? "\nvalid individual" : "\ninvalid individual");
        sb.append("\nFitness score: " + fitnessScore);
        sb.append("\ntotal weight: " + totalWeight + "\n");

        return sb.toString();
    }
}
