public class Item {
    private double weight;
    private double value;

    public Item(double weight, double value) {
        this.value = value;
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }

    public double getValue() {
        return value;
    }

}