import java.io.IOException;

public class Main {
    
    public static void main(String[] args) {
        try {
            GeneticAlgorithm ga = new GeneticAlgorithm("f1_l-d_kp_10_269");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println(e.getMessage());
            
        }
    }
}
