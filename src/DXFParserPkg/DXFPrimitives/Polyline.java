package DXFParserPkg.DXFPrimitives;

public class Polyline implements DXFComponent {
    private final double xCoord;
    private final double yCoord;
    private final double bulge;
    private final double closed;

    @FactoryConstructor
    public Polyline(double xCoord, double yCoord, double bulge, double closed) {
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.bulge = bulge;
        this.closed = closed;
    }

    public double getxCoord() {
        return xCoord;
    }

    public double getyCoord() {
        return yCoord;
    }

    public double getBulge() {
        return bulge;
    }

    public double getClosed() {
        return closed;
    }

    @Override
    public String toString() {
        return ("Х координата: " + xCoord + ", Y координата: " + yCoord +
                ", Bulge: " + bulge + ", closed: " + closed);
    }
}
