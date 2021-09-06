package DXFParserUI;

import DXFParserPkg.DStVLine;
import DXFParserPkg.DXFPrimitives.Circle;

import javax.swing.*;
import java.awt.*;

public class GraphicField extends JFrame {

    public GraphicField() throws HeadlessException {
        this.setSize(1200, 800);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    GrComponent lc = new GrComponent();

    public void drawLine(DStVLine line){
       lc.addLine(line);
       this.getContentPane().add(lc);
    }

    public void drawCircle(Circle cir){
        lc.addCircle(cir);
        this.getContentPane().add(lc);
    }
}
