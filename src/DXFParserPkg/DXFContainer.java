package DXFParserPkg;
import DXFParserPkg.DXFPrimitives.*;

import java.util.ArrayList;
import java.util.List;

public class DXFContainer {
    public List<Circle> circleList = new ArrayList<>();
    public List<Arc> arcList = new ArrayList<>();
    public List<Line> lineList = new ArrayList<>();
    public List<PolylineBlock> polylineBlocks = new ArrayList<>();
    //легаси-мусор
    public List<List<Polyline>> polylineList = new ArrayList<>();


    public void addCircle(Circle c){
        circleList.add(c);
    }
    public void  addArc(Arc a){
        arcList.add(a);
    }
    public void addLine(Line l){
        lineList.add(l);
    }
    public void addPolyLine(PolylineBlock plb){
        polylineBlocks.add(plb);
    }
    public void addPolyline(List<Polyline> lpl){
        polylineList.add(lpl);
    }
}
