package DXFParserPkg;

//описываем точку по типу, принятому в DStV
public class DStVLine {
    private double radius;
    private Point stPoint;
    private Point endPoint;

    public DStVLine(Point stP, Point endP, double radius) {
        this.stPoint = stP;
        this.endPoint = endP;
        this.radius = radius;
    }

    public Point getStPoint() {
        return stPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public void setStPoint(Point stPoint) {
        this.stPoint = stPoint;
    }

    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
    }

    public static class Point {
        double xCoord;
        double yCoord;

        public double getxCoord() {
            return xCoord;
        }

        public double getyCoord() {
            return yCoord;
        }

        public Point(double xCoord, double yCoord) {
            this.xCoord = xCoord;
            this.yCoord = yCoord;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DStVLine.Point)) return false;
            return (this.xCoord == ((DStVLine.Point) obj).xCoord &&
                    this.yCoord == ((DStVLine.Point) obj).yCoord);
        }

        //перегруженная версия, на случай неточных совпадений точек
        public boolean equals(DStVLine.Point pt, double accuracy) {
            return (Math.abs(this.xCoord - pt.xCoord) <= accuracy &&
                    Math.abs(this.yCoord - pt.yCoord) <= accuracy);
        }
    }
}