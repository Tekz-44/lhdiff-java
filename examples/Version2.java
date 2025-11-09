public class Calculator {
    // Added documentation
    public int add(int a, int b) {
        return a + b;
    }
    
    public int subtract(int a, int b) {
        return a - b;
    }
    
    // New method
    public int multiply(int a, int b) {
        return a * b;
    }
    
    public static void main(String[] args) {
        Calculator calc = new Calculator();
        System.out.println("Sum: " + calc.add(5, 3));
        System.out.println("Difference: " + calc.subtract(5, 3));
        System.out.println("Product: " + calc.multiply(5, 3));
    }
}