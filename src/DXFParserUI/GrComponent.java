package DXFParserUI;

import DXFParserPkg.DStVLine;
import DXFParserPkg.DXFPrimitives.Circle;
import DXFParserPkg.DXFPrimitives.Line;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class GrComponent extends JComponent {

    private java.util.List<DStVLine> lines = new ArrayList<>();

    public void addLine(DStVLine line) {
        lines.add(line);
    }

    private java.util.List<Circle> circles = new ArrayList<>();

    public void addCircle(Circle cir) {
        circles.add(cir);
    }

    double mas = 3;
    double xTrans = 40;
    double yTrans = 40;

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Color.BLUE.darker());
        super.paintComponent(g);
        for (DStVLine line : lines) {
            g.drawLine((int) (line.getStPoint().getxCoord() * mas + xTrans),
                    (int) (line.getStPoint().getyCoord() * mas + yTrans),
                    (int) (line.getEndPoint().getxCoord() * mas + xTrans),
                    (int) (line.getEndPoint().getyCoord() * mas + yTrans));
        }

        g.setColor(Color.BLACK);
        for (Circle cir : circles) {
            g.drawArc((int) ((cir.getxCoordCentre() - cir.getRadius()) * mas + xTrans),
                    (int) ((cir.getyCoordCentre() - cir.getRadius()) * mas + yTrans),
                    (int) (cir.getRadius() * 2 * mas),
                    (int) (cir.getRadius() * 2 * mas),
                    0, 360);
        }
    }
}
