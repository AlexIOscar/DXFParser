package DXFParserPkg;

import DXFParserPkg.DXFPrimitives.Circle;

import java.util.ArrayList;
import java.util.List;

import static DXFParserPkg.DXFComponentParser.getIndexOfExternal;

public class DStVContainer {
    public ArrayList<Circle> circleList;
    public ArrayList<DStVContour> externalContour;
    public ArrayList<DStVContour> internalContours;

    public DStVContainer(ArrayList<Circle> circleList, ArrayList<DStVContour> externalContour, ArrayList<DStVContour> internalContours) {
        this.circleList = circleList;
        this.externalContour = externalContour;
        this.internalContours = internalContours;
    }

    public static DStVContainer getContainer(String addr) {
        //получаем объект парсера
        DXFComponentParser dxfcp = new DXFComponentParser(addr);

        //получаем контейнер DXF-примитивов
        DXFContainer c = dxfcp.getComponentContainer();

        //создаем список отверстий
        ArrayList<Circle> byProductCList = new ArrayList<>();
        //получаем список контуров в виде листов
        List<List<DStVLine>> contours = dxfcp.getAllContours(c, byProductCList);

        DStVContour ext;
        //находим индекс наружного контура
        int index = getIndexOfExternal(contours);

        //создаем наружный контур и получаем в него контур с вычисленным индексом
        if (contours.size() != 0 && index != -1) {
            ext = new DStVContour(contours.get(index));
        } else ext = null;

        ArrayList<DStVContour> internals = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            if (i != index) {
                internals.add(new DStVContour(contours.get(i)));
            }
        }
        ArrayList<DStVContour> extContours = new ArrayList<>();
        if (ext != null) {
            extContours.add(ext);
        }
        return new DStVContainer(byProductCList, extContours, internals);
    }
}
