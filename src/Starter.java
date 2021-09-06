import DXFParserPkg.*;
import DXFParserPkg.DXFPrimitives.*;

import static DXFParserPkg.DXFComponentParser.*;

import java.util.ArrayList;
import java.util.List;

public class Starter {

    public static void main(String[] args) throws InterruptedException {

        DXFComponentParser dxfcp = new DXFComponentParser("C:\\DXF example\\144mess.dxf");

        //примитивный бенчмарк

        /*
        long nowtime = System.currentTimeMillis();

        for (int i = 0; i < 1000000; i++) {

        }

        System.out.println("millis passed " + (System.currentTimeMillis() - nowtime));
        */

        //DXFParserGUI gui = new DXFParserGUI();
        DXFContainer c = dxfcp.getComponentContainer();
        List<Circle> byProductCList = new ArrayList<>();
        List<List<DStVLine>> contours = dxfcp.getAllContours(c, byProductCList);


        int index = getIndexOfExternal(contours);
        System.out.println("наружный контур имеет индекс " + index);

        //GraphicField gf = new GraphicField();
        /*
        for (var cir : byProductCList) {
            gf.drawCircle(cir);
        }
        */

        //здесь строка имени только для заголовка!! данные все исходят из dxfcp!
        System.out.println(dxfcp.getDStVRep("C:\\DXF example\\144mess.dxf"));

        //DStVContainer contn = DStVContainer.getContainer("C:\\DXF example\\144.dxf");
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
        if(contours.size() != 0){
            ext = new DStVContour(contours.get(index));
        } else ext = null;

        ArrayList<DStVContour> internals = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            if (i != index) {
                internals.add(new DStVContour(contours.get(i)));
            }
        }

        ArrayList<DStVContour> extContours = new ArrayList<>();
        extContours.add(ext);
        return new DStVContainer(byProductCList, extContours, internals);
    }
}