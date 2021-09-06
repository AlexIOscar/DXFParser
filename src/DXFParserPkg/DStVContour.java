package DXFParserPkg;

import java.util.ArrayList;
import java.util.List;

public class DStVContour {
    public List<DStVLine> lineSet;

    public DStVContour(List<DStVLine> inLineSet) {
        //если контур не закрыт, то возвращаем пустой набор линий
        if(!DXFComponentParser.isClosed(inLineSet)){
            lineSet = new ArrayList<>();
            return;
        }

        List<DStVLine> lineSet = new ArrayList<>(inLineSet);

        int lineSetSize = lineSet.size();
        List<DStVLine> sortedSet = new ArrayList<>();
        sortedSet.add(lineSet.get(0));
        //выбираем "опорную" линию - та, относительно которой будем искать следующую в очереди
        DStVLine current = lineSet.get(0);
        lineSet.set(0, null);
        while (lineSetSize > 1) {
            int index = 0;
            for (DStVLine ln : lineSet) {
                if (ln == null) {
                    index++;
                    continue;
                }
                // если конечная точка опорной линии совпадает с конечной точкой рассматриваемой,
                // то рассматриваемую нужно развернуть, после чего можно добавить в
                // последовательность
                if (current.getEndPoint().equals(ln.getEndPoint(), 0.01)) {
                    DXFParserExtension.flipLine(ln);
                    sortedSet.add(ln);
                    current = ln;
                    lineSet.set(index, null);
                    lineSetSize--;
                    break;
                }
                // если конечная точка "опорной" линии совпадает с начальной точкой
                // рассматриваемой, то рассматриваемая является следующей в последовательности
                if (current.getEndPoint().equals(ln.getStPoint(), 0.01)) {
                    sortedSet.add(ln);
                    current = ln;
                    lineSet.set(index, null);
                    lineSetSize--;
                    break;
                }
                index++;
            }
        }
        this.lineSet = sortedSet;
    }
}