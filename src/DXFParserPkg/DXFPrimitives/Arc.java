package DXFParserPkg.DXFPrimitives;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Arc implements DXFComponent {

    private final double xCoordCentre;
    private final double yCoordCentre;
    private final double radius;
    private double startAngle;
    private double endAngle;

    @FactoryConstructor
    public Arc(double xCoordCentre, double yCoordCentre, double radius, double startAngle, double endAngle) {
        this.xCoordCentre = xCoordCentre;
        this.yCoordCentre = yCoordCentre;
        this.radius = radius;
        this.startAngle = startAngle;
        this.endAngle = endAngle;
    }

    public double getSweepAngle() {
        if (startAngle == 360d) startAngle = 0d;
        if (endAngle == 0d) endAngle = 360d;
        if (endAngle > startAngle) {
            return (endAngle - startAngle);
        } else {
            return (360 - startAngle + endAngle);
        }
    }

    public double getxCoordCentre() {
        return xCoordCentre;
    }

    public double getyCoordCentre() {
        return yCoordCentre;
    }

    public double getRadius() {
        return radius;
    }

    public double getStartAngle() {
        return startAngle;
    }

    public double getEndAngle() {
        return endAngle;
    }

    @Override
    public String toString() {
        return ("Х(центр): " + xCoordCentre + ", Y(центр): " + yCoordCentre +
                ", Радиус: " + radius + ", Нач.угол:" + startAngle + ", Конеч.угол:" + endAngle);
    }
}
