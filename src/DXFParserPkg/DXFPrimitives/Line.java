package DXFParserPkg.DXFPrimitives;

//Representation of DXF line
public class Line implements DXFComponent{
    private final double xCoordSt;
    private final double yCoordSt;
    private final double xCoordEnd;
    private final double yCoordEnd;

    @FactoryConstructor
    public Line(double xCoordSt, double yCoordSt, double xCoordEnd, double yCoordEnd) {
        this.xCoordSt = xCoordSt;
        this.yCoordSt = yCoordSt;
        this.xCoordEnd = xCoordEnd;
        this.yCoordEnd = yCoordEnd;
    }

    public double getxCoordSt() {
        return xCoordSt;
    }

    public double getyCoordSt() {
        return yCoordSt;
    }

    public double getxCoordEnd() {
        return xCoordEnd;
    }

    public double getyCoordEnd() {
        return yCoordEnd;
    }

    @Override
    public String toString() {
        return ("Х(начало): " + xCoordSt + ", Y(начало): " + yCoordSt +
                ", X(конец): " + xCoordEnd + ", Y(конец):" + yCoordEnd);
    }
}