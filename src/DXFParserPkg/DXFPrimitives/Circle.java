package DXFParserPkg.DXFPrimitives;

public class Circle implements DXFComponent {

    private double xCoordCentre;
    private double yCoordCentre;
    private double radius;

    @FactoryConstructor
    public Circle(double xCoordCentre, double yCoordCentre, double radius) {
        this.xCoordCentre = xCoordCentre;
        this.yCoordCentre = yCoordCentre;
        this.radius = radius;
    }

    public void printCircle() {
        System.out.println("Х координата: " + xCoordCentre + ", Y координата: " + yCoordCentre +
                ", Диаметр: " + radius * 2);
    }

    @Override
    public String toString() {
        return ("Х координата: " + xCoordCentre + ", Y координата: " + yCoordCentre +
                ", Диаметр: " + radius * 2);
    }

    public double getxCoordCentre() {
        return xCoordCentre;
    }

    public void setxCoordCentre(double xCoordCentre) {
        this.xCoordCentre = xCoordCentre;
    }

    public double getyCoordCentre() {
        return yCoordCentre;
    }

    public void setyCoordCentre(double yCoordCentre) {
        this.yCoordCentre = yCoordCentre;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }
}
