import java.util.List;

public class Individual {

    List<Boolean> chromosome;
    Boolean isValid = true;
    double fitnessScore = 0;
    double totalWeight = 0;
    double totalValues = 0;

    Individual(List<Boolean> chromosome) {
        this.chromosome = chromosome;
    }

    void setValidity(Boolean isValid) {
        this.isValid = isValid;
    }

    void setFitnessScore(double fitness) {
        this.fitnessScore = fitness;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Boolean gene : chromosome) {
            sb.append(gene + " ");

        }
        sb.append(isValid ? "\nvalid individual" : "\ninvalid individual");
        sb.append("\nFitness score: " + fitnessScore);
        sb.append("\ntotal weight: " + totalWeight + "\n");

        return sb.toString();
    }
}
