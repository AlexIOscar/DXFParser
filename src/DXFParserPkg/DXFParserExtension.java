package DXFParserPkg;

import DXFParserPkg.DXFPrimitives.Arc;
import DXFParserPkg.DXFPrimitives.Circle;
import DXFParserPkg.DXFPrimitives.Line;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static DXFParserPkg.DXFComponentParser.isCCW;
import static java.lang.Math.*;

//специальный вспомогательный класс для работы с линиями
public abstract class DXFParserExtension {

    public static List<DStVLine> fromDXFArcToDStvLine(double xCentrePoint, double yCentrePoint,
                                                      double radius, double stAng, double endAng) {
        Arc arc = new Arc(xCentrePoint, yCentrePoint, radius, stAng, endAng);
        //контейнер для вывода
        List<DStVLine> outPare = new ArrayList<>();
        //центральная точка
        DStVLine.Point centerP = new DStVLine.Point(xCentrePoint, yCentrePoint);
        // поскольку построение дуги всегда идет CCW, при взгляде из первой точки во вторую дуга
        // всегда будет справа, а значит ее радиус положителен по правилам DStV
        // если разность конечного и стартового углов меньше 180, то дугу можно представить в
        // виде одной DStV-линии, иначе нужно разбить на две
        DStVLine.Point stP = rotatePoint(centerP, radius, stAng);
        if (arc.getSweepAngle() <= 180) {
            DStVLine.Point endP = rotatePoint(centerP, radius, endAng);
            outPare.add(new DStVLine(stP, endP, radius));
        } else {
            DStVLine.Point endP = rotatePoint(centerP, radius, stAng + 180);
            outPare.add(new DStVLine(stP, endP, radius));
            DStVLine.Point endP2 = rotatePoint(centerP, radius, endAng);
            outPare.add(new DStVLine(endP, endP2, radius));
        }
        return outPare;
    }

    public static List<DStVLine> fromDXFArcToDStvLine(Arc arc) {
        return fromDXFArcToDStvLine(arc.getxCoordCentre(), arc.getyCoordCentre(), arc.getRadius()
                , arc.getStartAngle(), arc.getEndAngle());
    }

    public static DStVLine fromDXFLineToDStVLine(Line line) {
        DStVLine.Point stP = new DStVLine.Point(line.getxCoordSt(), line.getyCoordSt());
        DStVLine.Point endP = new DStVLine.Point(line.getxCoordEnd(), line.getyCoordEnd());
        return new DStVLine(stP, endP, 0);
    }

    public static Arc fromDStVLineToArc(DStVLine line) {
        // если радиус нулевой, то это отрезок
        if (line.getRadius() == 0) return null;
        double x1 = line.getStPoint().xCoord;
        double x2 = line.getEndPoint().xCoord;
        double y1 = line.getStPoint().yCoord;
        double y2 = line.getEndPoint().yCoord;

        double d = sqrt(pow((x1 - x2), 2) + pow((y1 - y2), 2));
        double h = sqrt(line.getRadius() * line.getRadius() - (d / 2) * (d / 2));
        double xC;
        double yC;

        xC = x1 + (x2 - x1) / 2 - h * (y2 - y1) / d;
        yC = y1 + (y2 - y1) / 2 + h * (x2 - x1) / d;

        return new Arc(xC, yC, line.getRadius(), toDegrees(atan2(y1 - yC, x1 - xC)),
                toDegrees(atan2(y2 - yC,
                        x2 - xC)));
    }

    public static List<Arc> fromCircleToArcs(Circle circle) {
        Arc arc1 = new Arc(circle.getxCoordCentre(),
                circle.getyCoordCentre(), circle.getRadius(), 0, 180);
        Arc arc2 = new Arc(circle.getxCoordCentre(),
                circle.getyCoordCentre(), circle.getRadius(), 180, 0);
        return Stream.of(arc1, arc2).collect(Collectors.toList());
    }

    private static DStVLine.Point rotatePoint(DStVLine.Point center, double rad, double angle) {
        DStVLine.Point point = new DStVLine.Point(0, 0);
        point.xCoord = (rad * Math.cos(Math.toRadians(angle))) + center.xCoord;
        point.yCoord = (rad * Math.sin(Math.toRadians(angle))) + center.yCoord;
        return point;
    }

    //разворот линии
    public static void flipLine(DStVLine ln) {
        DStVLine.Point bufPoint = new DStVLine.Point(ln.getStPoint().xCoord, ln.getStPoint().yCoord);
        ln.setStPoint(ln.getEndPoint());
        ln.setEndPoint(bufPoint);
        if(ln.getRadius() != 0){
            ln.setRadius(-ln.getRadius());
        }
    }

    //метод проверки "отношения" между любыми двумя окружностями. На выходе true, если окружность
    // может быть отверстием, будучи на чертеже в паре с отверстием-базой.
    public static boolean checkHolesPare(Circle candidate, Circle base, StringBuffer log) {
        //расстояние между центрами
        double distBetwCentres =
                Math.sqrt(Math.pow(Math.abs(candidate.getxCoordCentre() - base.getxCoordCentre())
                        , 2) +
                        Math.pow(Math.abs(candidate.getyCoordCentre() - base.getyCoordCentre()),
                                2));
        //сумма радиусов
        double radSum = candidate.getRadius() + base.getRadius();
        //фактор-1 (см пояснительную записку с теорией aka "ТЗ")
        double factor1 = base.getRadius() - candidate.getRadius();
        //реализация условий из "ТЗ":
        if (distBetwCentres >= radSum || factor1 > distBetwCentres) {
            return true;
        } else if (factor1 < 0 && -factor1 >= distBetwCentres) {
            return false;
        } else {
            log.append("отверстия соприкасаются - ошибка данных\n");
            return false;
        }
    }

    // метод возвращает удаленность максимально далекой точки дуги от ее центра в указанном
    // (в аргументе direction) направлении
    // Красивее и яснее было бы получить DStV-линию из дуги (метод fromDXFArcToDStvLine), затем,
    // имея центр и две точки концов дуги, составить два вектора от центра к концам, создать
    // вектор направления (весьма тривиально), и между всеми этими векторами получать углы и
    // проекции друг на друга в любом порядке базовыми векторными операциями.
    public static double getMaxReach(Arc arc, double direction) {
        //получаем конечный угол
        double endAngle = arc.getEndAngle();
        //получаем начальный угол
        double stAngle = arc.getStartAngle();
        //если начальный больше конечного, нужно выполнить "выравнивание порядка" одним полным
        // его оборотом назад
        if (stAngle > endAngle) {
            //если направление превышает тот максимальный угол, который мы "откатываем" на
            // оборот, то и его нужно "откатить", поскольку важной частью алгоритма является
            // сравнение углов и направления
            if (direction > stAngle) {
                direction -= 360;
            }
            stAngle -= 360;
        }

        // проверяем, находится ли направление между стартовым и конечным углами, если да (false в
        // условном операторе), то в этом месте лежит сегмент дуги, и значит удаленность
        // максимально возможна и равна ее радиусу, иначе заходим внутрь блока вычислений.
        if ((stAngle < direction && endAngle < direction)
                || (stAngle > direction && endAngle > direction)) {

            // получаем угол между направлением и углом, берем его по модулю, потому что
            // направление безразлично
            double angle1 = ((Math.abs(direction - stAngle)));
            // если угол больше 180, его следует как бы "отразить" относительно направления, для
            // этого он вычитается из 360
            if (angle1 > 180) angle1 = 360 - angle1;
            //вычисляем удаленность точки дуги от центра в направлени direction
            double reach1 = arc.getRadius() * Math.cos(Math.toRadians(angle1));

            //то же самое для второго угла
            double angle2 = ((Math.abs(direction - endAngle)));
            if (angle2 > 180) angle2 = 360 - angle2;
            double reach2 = arc.getRadius() * Math.cos(Math.toRadians(angle2));

            //возвращаем наибольшее из двух
            return Math.max(reach1, reach2);
        } else {
            return arc.getRadius();
        }
    }

    public static String getHoleRecord(Circle cir, String location) {
        StringBuilder outRecord = new StringBuilder("  ").append(location);

        String[] xCVParts = getSplittedValue(cir.getxCoordCentre());
        String[] yCVParts = getSplittedValue(cir.getyCoordCentre());
        String[] diamParts = getSplittedValue(cir.getRadius() * 2);


        for (int i = 0; i < 6 - xCVParts[0].length(); i++) {
            outRecord.append(" ");
        }
        outRecord.append(xCVParts[0]).append(".").append(xCVParts[1]).append("u");

        for (int i = 0; i < 6 - yCVParts[0].length(); i++) {
            outRecord.append(" ");
        }
        outRecord.append(yCVParts[0]).append(".").append(yCVParts[1]);

        for (int i = 0; i < 4 - diamParts[0].length(); i++) {
            outRecord.append(" ");
        }
        //нет способа определеить, сквозное ли отверстие, так что в конце всегда нули
        outRecord.append(diamParts[0]).append(".").append(diamParts[1]).append("   0.00");

        return outRecord.toString();
    }

    private static String getCornerRecord(DStVLine line, String location) {
        StringBuilder outRecord = new StringBuilder("  ").append(location);
        String[] pointXParts = getSplittedValue(line.getStPoint().xCoord);
        String[] pointYParts = getSplittedValue(line.getStPoint().yCoord);
        String[] radParts = getSplittedValue(line.getRadius());

        for (int i = 0; i < 6 - pointXParts[0].length(); i++) {
            outRecord.append(" ");
        }
        outRecord.append(pointXParts[0]).append(".").append(pointXParts[1]).append("u");

        for (int i = 0; i < 6 - pointYParts[0].length(); i++) {
            outRecord.append(" ");
        }
        outRecord.append(pointYParts[0]).append(".").append(pointYParts[1]);

        for (int i = 0; i < 4 - radParts[0].length(); i++) {
            outRecord.append(" ");
        }
        outRecord.append(radParts[0]).append(".").append(radParts[1]);

        return outRecord.toString();
    }

    //TODO тестировать разворот контура в зависимости от типа
    public static String getContourRecord(DStVContour cont, ContourType type, String loc) {
        //заголовок блока, в зависимости от типа контура
        StringBuilder outRecord = new StringBuilder(type.strRep);

        //если направление внутри контура не соответствует типу, разворачиваем все линии контура и меняем порядок
        // следования точек на обратный
        if ((type == ContourType.AK && isCCW(cont.lineSet)) ||
                (type == ContourType.IK && !isCCW(cont.lineSet))) {
            for (DStVLine line : cont.lineSet) {
                flipLine(line);
            }
            Collections.reverse(cont.lineSet);
        }

        for (DStVLine line : cont.lineSet) {
            outRecord.append(getCornerRecord(line, loc)).append("\n");
        }
        //замыкаем контур
        if (cont.lineSet.size() > 0) {
            outRecord.append(getCornerRecord(cont.lineSet.get(0), loc));
        }
        return outRecord.toString();
    }

    private static String[] getSplittedValue(Double val) {
        String value = String.format("%.2f", val);
        return value.split(",");
    }

    public enum ContourType {
        AK("AK"), IK("IK");
        String strRep;

        ContourType(String rep) {
            strRep = rep + "\n";
        }
    }
}
