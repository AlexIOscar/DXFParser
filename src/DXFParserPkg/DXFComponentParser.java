package DXFParserPkg;

import CutCounterPkg.WrongLineExeption;
import DXFParserPkg.DXFPrimitives.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static DXFParserPkg.DXFParserExtension.getContourRecord;
import static java.lang.Math.*;

public class DXFComponentParser {
    File file;
    static double accuracy = 0.1;
    static double angleAccuracy = 1;
    static String rdPattern = "%.2f";
    Double diamThreshold = 100d;
    public List<String> warningLog = new ArrayList<>();

    public DXFComponentParser(String address) {
        this.file = new File(address);
    }

    public DXFComponentParser(File file) {
        this.file = file;
    }

    public DXFContainer getComponentContainer() {
        DXFContainer dxfc = new DXFContainer();
        List<Circle> circleList = new ArrayList<>();
        List<Arc> arcList = new ArrayList<>();
        List<Line> lineList = new ArrayList<>();
        //List<List<Polyline>> polylineList = new ArrayList<>();
        List<DXFComponent> abstractContainer = new ArrayList<>();

        dxfc.circleList = circleList;
        dxfc.arcList = arcList;
        dxfc.lineList = lineList;
        //dxfc.polylineList = polylineList;
        warningLog.clear();

        try (FileReader fr = new FileReader(file)) {
            BufferedReader reader = new BufferedReader(fr);

            //лист меток начала секции примитивов
            List<String> sectionsTypes = DXFPatterns.getAllTypeNames();
            //текущая секция
            DXFPatterns currentSection = null;
            //контейнер аргументов
            List<Double> componentArgs = new ArrayList<>();
            //контейнер флагов записи аргументов
            List<Boolean> argChanged = new ArrayList<>();

            // статус нахождения в работе - между тэгом ENTITIES и ENDSEC
            boolean inWork = false;
            //защелка для блока подготовки контейнеров
            boolean containersLock = false;

            String line = reader.readLine();
            while (line != null) {
                //изначально установлен "холостой режим" - !inWork. В нем проверяется только
                // началась ли секция ENTITIES или BLOCKS. Если да, то устанавливаем рабочий
                // режим, и больше в это условие никогда не попадаем, если все еще нет, считываем
                // новую линюю и  продолжаем цикл

                if (line.equals("ENTITIES") || line.equals("BLOCKS")) {
                    inWork = true;
                }

                if (!inWork) {
                    line = reader.readLine();
                    continue;
                }

                //проверяем каждую строчку на совпадение с каким-нибудь типом из тех, которые
                // зарегистрированы в списке меток секций
                for (String type : sectionsTypes) {
                    //если совпадение нашлось, то устанавливаем текущую секцию в выявленное
                    // состояние
                    if (line.equals(type)) {
                        currentSection = DXFPatterns.getType(line);
                        //сразу читаем новую линию (эта уже распознана и "использована")
                        line = reader.readLine();
                        break;
                    }
                }

                //если метка секции все еще не обнаружена, читаем новую линию и продолжаем цикл
                // досрочно
                if (currentSection == null) {
                    line = reader.readLine();
                    continue;
                }

                //сюда доходим только если обнаружена секция, в буфере сейчас её нулевая линия

                if (!containersLock) {
                    //добавляем нужное количество ячеек в контейнер аргументов и контейнер флагов
                    // их записи
                    for (int i = 0; i < currentSection.valuesMarkList.size(); i++) {
                        componentArgs.add(0d);
                        argChanged.add(false);
                    }
                    containersLock = true;
                }

                boolean aheadOfTime = false;
                for (int i = 0; i < currentSection.valuesMarkList.size(); i++) {
                    if (line.equals(currentSection.valuesMarkList.get(i))) {

                        //если ячейка по этому адресу уже изменена ранее
                        if (argChanged.get(i)) {
                            //делаем вывод что уже начался новый примитив, поэтому проверяем,
                            // изменены ли были ячейки по опциональным аргументам, и если нет,
                            // устанавливаем для них состояние измененности. Ограничение
                            // реализации: так можно делать только в случае, если состояние
                            // опциональных аргументов по-умолчанию - нулевое.
                            for (int val : currentSection.optionalArgsIndexes) {
                                if (!argChanged.get(val)) {
                                    argChanged.set(val, true);
                                }
                            }
                            //устанавливаем флаг "досрочной сборки"
                            aheadOfTime = true;
                            // обрываем цикл, чтобы не потерять метку типа аргумента, которая
                            // сейчас в буфере (иначе она перезапишется вызовом readLine ниже)
                            break;
                        }

                        String value = String.format(Locale.ROOT, rdPattern,
                                Double.parseDouble(reader.readLine()));
                        componentArgs.set(i, Double.parseDouble(value));
                        argChanged.set(i, true);
                        // дальнейшую проверку обрываем, так как только одна метка аргумента может
                        // совпасть с линией в буфере
                        break;
                    }
                }

                // если находим паттерн конца блока, устанавливаем тип секции в null. Возможно, в
                // это время "завис" недособранный примитив. Проверяем все опциональные аргументы
                boolean zeroChecked = false;
                boolean sectionDone = true;
                if (line.equals("  0")) {
                    for (boolean b : argChanged) {
                        if (!b) continue;
                        sectionDone = false;
                    }

                    if (!sectionDone) {
                        for (int val : currentSection.optionalArgsIndexes) {
                            if (!argChanged.get(val)) {
                                argChanged.set(val, true);
                            }
                        }
                    }
                    zeroChecked = true;
                }

                boolean timeForMount = true;
                //проверяем, что каждая ячейка обновлена актуальными данными
                for (Boolean b : argChanged) {
                    if (!b) {
                        timeForMount = false;
                        break;
                    }
                }

                if (timeForMount) {
                    //все компоненты собраны, создаем примитив
                    DXFComponent comp = currentSection.createComponent(componentArgs);
                    //очищаем аргументы для следующего прохода
                    componentArgs.clear();
                    //очищаем лист флагов записи
                    argChanged.clear();
                    //разблокируем защелку генератора контейнеров
                    containersLock = false;
                    //добавляем примитив как компонент в общий контейнер
                    abstractContainer.add(comp);

                    //если сборка была досрочной, продолжаем с сохранением линии в буфере - в
                    // этом случае она содержит код аргумента, который повторился
                    if (aheadOfTime) continue;

                    //если сборка не доукомплектована, но флаг того, что пора ее выполнить
                    // установлен, то в данных ошибка
                } else if (aheadOfTime || zeroChecked) {
                    if (!sectionDone) {
                        warningLog.add("Часть параметров примитива не найдена!\n");
                    }
                    //очищаем аргументы для следующего примитива
                    componentArgs.clear();
                    //очищаем лист флагов записи
                    argChanged.clear();
                    //разблокируем защелку генератора контейнеров
                    containersLock = false;
                    if (!zeroChecked) {
                        continue;
                    }
                }

                if (zeroChecked) {
                    if (currentSection.multiObj) {
                        DXFComponent[] subcomps = new DXFComponent[abstractContainer.size()];
                        int index = 0;
                        for (DXFComponent singleComp : abstractContainer) {
                            subcomps[index] = singleComp;
                            index++;
                        }
                        DXFComponent multicomp = currentSection.createComponent(subcomps);
                        pushToContainer(multicomp, dxfc, currentSection);
                    } else {
                        for (DXFComponent comp : abstractContainer) {
                            pushToContainer(comp, dxfc, currentSection);
                        }
                    }
                    currentSection = null;
                    abstractContainer.clear();
                }

                //читаем новую строку и перезаходим с ней в цикл
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dxfc;
    }

    private void pushToContainer(DXFComponent forAdding, DXFContainer consumer,
                                 DXFPatterns pat) {
        Method[] methods = DXFContainer.class.getDeclaredMethods();
        Method target = null;
        for (Method meth : methods) {
            Class<?>[] args = meth.getParameterTypes();
            for (Class<?> arg : args) {
                if (!pat.multiObj) {
                    if (arg == pat.representationDataClass) {
                        target = meth;
                        break;
                    }
                } else {
                    if (arg == pat.reprMultiClass) {
                        target = meth;
                        break;
                    }
                }
            }
        }
        try {
            assert target != null : "wrong representation type in DXFPatterns enum";
            target.invoke(consumer, forAdding);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void pushToFile(File outFile) {
        DXFContainer dxfContainer = getComponentContainer();
        List<Circle> circleList = dxfContainer.circleList;
        List<Arc> arcList = dxfContainer.arcList;
        List<Line> lineList = dxfContainer.lineList;
        List<PolylineBlock> plblist = dxfContainer.polylineBlocks;
        try (FileWriter writer = new FileWriter(outFile, false)) {
            writer.write("Перечень отверстий:\n");
            for (Circle c : circleList) {
                writer.write(c.toString() + "\n");
            }
            writer.write("Перечень дуг:\n");
            for (Arc a : arcList) {
                writer.write(a.toString() + "\n");
            }
            writer.write("Перечень линий:\n");
            for (Line l : lineList) {
                writer.write(l.toString() + "\n");
            }
            writer.write("Перечень полилиний(поблочно):\n");
            for (PolylineBlock plb : plblist) {
                writer.write(plb.toString() + "\n");
            }
            writer.flush();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    //не актуально с момента внедрения полилиний
    private List<DStVLine> getDStVLineSet() {
        //получаем контейнер DXF-примитивов
        DXFContainer dxfc = getComponentContainer();
        //создаем возвращаемый лист, инициализируем его всеми дугами, преобразованными в линии
        // DStV средствами специального класса-расширения
        List<DStVLine> outList = dxfc.arcList.stream()
                .map(DXFParserExtension::fromDXFArcToDStvLine)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        //добавляем в лист все линии, предварительно сконвертировав их к нужному типу
        outList.addAll(dxfc.lineList.stream()
                .map(DXFParserExtension::fromDXFLineToDStVLine)
                .collect(Collectors.toList()));
        return outList;
    }

    //метод, группирующий однородный список линий, полученных парсингом, в группы линий по
    //признаку совпадения точки у линии с хотя бы одной точкой, уже существующей в группе
    private static List<List<DStVLine>> combineContours(List<Line> incomeLineSet,
                                                        List<Arc> incomeArcSet) {
        //получаем список всех линий
        List<DStVLine> startSet = incomeLineSet.stream().map(DXFParserExtension::fromDXFLineToDStVLine)
                .collect(Collectors.toList());
        startSet.addAll(incomeArcSet.stream()
                .map(DXFParserExtension::fromDXFArcToDStvLine)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));
        //создаем контейнер для контуров
        List<List<DStVLine>> groups = new ArrayList<>();
        //если набор напарсенных линий пуст, возвращаем пустой список групп(контуров)
        if (startSet.size() == 0) return groups;
        //извлекаем из списка последнюю линию
        DStVLine firstLine = startSet.get(startSet.size() - 1);
        //удаляем из списка последнюю линию, чтоб она более не участвовала в сопоставлении
        //работаем с последней линией, чтоб избежать перемещения индексов
        startSet.remove(startSet.size() - 1);
        //создаем первый контур
        List<DStVLine> firstGroup = new ArrayList<>();
        //добавляем первую линию в первый контур
        firstGroup.add(firstLine);
        //добавляем первый контур в контейнер
        groups.add(firstGroup);

        //создаем массив для индексов тех контуров, которые имеют хотя бы одну общую точку с
        // проверяемой линией
        List<Integer> hitIndexes = new ArrayList<>();
        //из начального набора последовательно берем каждую линию на проверку
        for (DStVLine lineForCheck : startSet) {
            //создаем индекс, изначально он нулевой (поскольку первым проверяется нулевой контур
            // в контейнере)
            int index = 0;
            //для каждого контура в контейнере...
            for (List<DStVLine> contour : groups) {
                //берется последовательно каждая линия
                for (DStVLine contourLine : contour) {
                    //если у проверяемой линии хотя бы одна точка совпадает с любой точкой линии,
                    // которая взята из ячейки контейнера...
                    if (lineForCheck.getStPoint().equals(contourLine.getStPoint(), accuracy) ||
                            lineForCheck.getStPoint().equals(contourLine.getEndPoint(), accuracy) ||
                            lineForCheck.getEndPoint().equals(contourLine.getStPoint(), accuracy) ||
                            lineForCheck.getEndPoint().equals(contourLine.getEndPoint(), accuracy)

                    ) {
                        //в массив индексов добавляем индекс того контура, в котором случилось это
                        // событие, и обрываем поиск по этому контуру, поскольку безразлично, есть
                        // ли у проверяемой точки еще "контакты" в этой конкретной ячейке
                        hitIndexes.add(index);
                        break;
                    }
                }
                //инкрементируем индекс для каждого следующего контура
                index++;
            }

            //если контейнер, фиксирующий совпадения, оказался пустым, значит проверяемая линия
            // не относится ни к одному контуру из уже созданных, и нужно создать для нее свой и
            // добавить к массиву контуров
            if (hitIndexes.size() == 0) {
                List<DStVLine> l = new ArrayList<>();
                l.add(lineForCheck);
                groups.add(l);
            } else {
                // иначе, добавляем проверяемую линию к тому контуру, который стоит на нулевой
                // позиции в обнаруженных совпадениях
                groups.get(hitIndexes.get(0)).add(lineForCheck);
                // и, дополнительно, если линия касается и других контуров (что засвидетельствовано
                // в массиве попаданий), начиная с конца переносим все линии в массив, помеченный
                // нулевым индексом, а потом удаляем
                for (int i = hitIndexes.size() - 1; i > 0; i--) {
                    groups.get(hitIndexes.get(0)).addAll(groups.get(hitIndexes.get(i)));
                    groups.remove((int) hitIndexes.get(i));
                }
            }
            hitIndexes.clear();
        }
        return groups;
    }

    //dummy
    private static boolean isCircle(List<Arc> contour) {
        return true;
    }

    //флаг 0 - удалять дуги в исходных полилиниях
    private static List<Arc> extractArcsFromPolylines(List<PolylineBlock> pllblist, int delFlag) {
        List<Arc> outArcs = new ArrayList<>();
        if (pllblist.size() == 0) return outArcs;

        for (PolylineBlock pllb : pllblist) {
            List<Polyline> pllSections = pllb.getPolylines();
            if (pllSections.size() == 0) continue;

            boolean isClosed = false;
            if (pllSections.get(0).getClosed() == 1) isClosed = true;

            boolean[] delIndexes = new boolean[pllSections.size()];
            for (int i = 0; i < pllSections.size() - 1; i++) {
                Polyline currentPl = pllSections.get(i);
                //если выпуклость текущего фрагмента нулевая, то пропускаем (это не дуга)
                if (currentPl.getBulge() == 0) continue;
                outArcs.add(transformPolylineToArc(currentPl, pllSections.get(i + 1)));
                delIndexes[i] = true;
            }
            if (pllSections.get(pllSections.size() - 1).getBulge() != 0 && isClosed) {
                outArcs.add(transformPolylineToArc(pllSections.get(pllSections.size() - 1),
                        pllSections.get(0)));
                delIndexes[pllSections.size() - 1] = true;
            }
            if (delFlag == 0) {
                for (int i = pllSections.size() - 1; i >= 0; i--) {
                    if (delIndexes[i]) pllSections.remove(i);
                }
            }
        }
        return outArcs;
    }

    //флаг 0 - удалять дуги в исходных полилиниях
    private static List<Line> extractLinesFromPolylines(List<PolylineBlock> pllblist, int delFlag) {
        List<Line> outLines = new ArrayList<>();

        if (pllblist.size() == 0) return outLines;

        for (PolylineBlock pllb : pllblist) {
            List<Polyline> pllSections = pllb.getPolylines();
            if (pllSections.size() == 0) continue;

            boolean isClosed = false;
            if (pllSections.get(0).getClosed() == 1) isClosed = true;

            boolean[] delIndexes = new boolean[pllSections.size()];
            for (int i = 0; i < pllSections.size() - 1; i++) {
                Polyline currentPl = pllSections.get(i);
                Polyline nextPl = pllSections.get(i + 1);
                //если выпуклость текущего фрагмента не нулевая, то пропускаем (это не отрезок)
                if (currentPl.getBulge() != 0) continue;
                outLines.add(new Line(currentPl.getxCoord(), currentPl.getyCoord(),
                        nextPl.getxCoord(), nextPl.getyCoord()));
                delIndexes[i] = true;
            }
            if (pllSections.get(pllSections.size() - 1).getBulge() == 0 && isClosed) {
                Polyline currentPl = pllSections.get(pllSections.size() - 1);
                Polyline nextPl = pllSections.get(0);
                outLines.add(new Line(currentPl.getxCoord(), currentPl.getyCoord(),
                        nextPl.getxCoord(), nextPl.getyCoord()));
                delIndexes[pllSections.size() - 1] = true;
            }
            if (delFlag == 0) {
                for (int i = pllSections.size() - 1; i >= 0; i--) {
                    if (delIndexes[i]) pllSections.remove(i);
                }
            }
        }
        return outLines;
    }

    //Внимание! этот метод удаляет дуги, преобразованные в отверстия, из исходного списка.
    // дополнительно: нет способа задать в этом методе точность совпадения параметров для
    // группировки, это должно быть обеспечено во входных данных
    private List<Circle> extractCirclesFromArcs(List<Arc> contours) {
        Map<Double, Map<Double, Map<Double, List<Arc>>>> map =
                contours.stream().collect(Collectors.groupingBy(Arc::getxCoordCentre,
                        Collectors.groupingBy(Arc::getyCoordCentre,
                                Collectors.groupingBy(Arc::getRadius))));

        List<Circle> outCircles = new ArrayList<>();

        for (Double x : map.keySet()) {
            for (Double y : map.get(x).keySet()) {
                for (Double r : map.get(x).get(y).keySet()) {
                    double checkFullround = 0;
                    for (Arc arc : map.get(x).get(y).get(r)) {
                        //сложить сумму углов, проверить на равенство 360
                        checkFullround += arc.getSweepAngle();
                    }
                    if (Math.abs(checkFullround - 360) < angleAccuracy) {
                        outCircles.add(new Circle(x, y, r));
                        //удаляем дуги
                        contours.removeIf(arc -> arc.getxCoordCentre() == x &&
                                arc.getyCoordCentre() == y &&
                                arc.getRadius() == r);
                        break;
                    }
                }
            }
        }

        //debug fragment
        /*
        System.out.println("Всего отверстий из дуг:" + outCircles.size());
        for (Circle oc : outCircles) {
            oc.printCircle();
        }
        */
        return outCircles;
    }

    //слабое условие, что контур мб окружностью: проверяет совпадение радиусов всех участков
    private static boolean isSubCircle(List<DStVLine> contour) {
        double rad = contour.get(0).getRadius();
        for (int i = 1; i < contour.size(); i++) {
            if (contour.get(i).getRadius() != rad) {
                return false;
            }
        }
        return true;
    }

    public static boolean isClosed(List<DStVLine> incomeContour) {
        //создаем клон аргумента, чтоб не оказывать сайд-эффект на сам аргумент
        List<DStVLine> contour = new ArrayList<>(incomeContour);
        //создаем контейнер для точек, встречающихся в контуре
        List<DStVLine.Point> points = new ArrayList<>();
        //берем последнюю линию контура
        DStVLine lastLine = contour.get(contour.size() - 1);
        //добавляем обе ее точки в контейнер точек (они гарантированно не совпадают)
        points.add(lastLine.getStPoint());
        points.add(lastLine.getEndPoint());
        //удаляем из контура линию, точки которой уже зафиксированы, чтоб она повторно не
        // рассматривалась
        contour.remove(contour.size() - 1);
        //создаем массив под хранение количеств обнаружения каждой из точек
        List<Integer> amounts = new ArrayList<>();
        //поскольку две первые точки созданы, добавляем под них ячейки в amounts, фиксируя что
        // каждая точка однократно уже "найдена"
        amounts.add(1);
        amounts.add(1);
        //перебираем все линии контура-аргумента
        //for (DStVLine line : contour) {
        for (DStVLine dStVLine : contour) {
            //создаем пару флагов, сигнализирующих что точка линии добавлена
            boolean firstAdded = false;
            boolean secondAdded = false;

            // для каждой точки в массиве, проверяем совпадение с точками
            // рассматриваемой в этом цикле линии
            for (int i = 0; i < points.size(); i++) {
                //если совпадение есть, получаем значение количества вхождений этой точки, и
                // возвращаем его обратно с прединкрементом, устанавливаем флаг что точка
                // добавлена
                if (!firstAdded && dStVLine.getStPoint().equals(points.get(i), accuracy)) {
                    Integer am = amounts.get(i);
                    amounts.set(i, ++am);
                    firstAdded = true;
                }
                //см выше
                if (!secondAdded && dStVLine.getEndPoint().equals(points.get(i),
                        accuracy)) {
                    Integer am = amounts.get(i);
                    amounts.set(i, ++am);
                    secondAdded = true;
                }
                //если обе точки уже найдены, то дальше можно не искать.
                if (firstAdded && secondAdded) {
                    break;
                }

                // если эта итерация последняя, а все точки все еще не распределены, добавляем их
                // как новые, и устанавливаем их флаги
                if (i + 1 == points.size()) {
                    if (!firstAdded) {
                        points.add(dStVLine.getStPoint());
                        amounts.add(1);
                        //нужно сдвинуть указатель вперед, иначе произойдет еще одна итерация!
                        i++;
                    }
                    if (!secondAdded) {
                        points.add(dStVLine.getEndPoint());
                        amounts.add(1);
                        //нужно сдвинуть указатель вперед, иначе произойдет еще одна итерация!
                        i++;
                    }
                }
            }
        }
        /*
        //для дебага
        for (Integer i : amounts
        ) {
            System.out.println(i);
        }
         */

        for (int am : amounts) {
            if (am != 2) {
                return false;
            }
        }
        return true;
    }

    private static double getFullAngle(DStVLine line1, DStVLine line2) {
        double xSt1 = line1.getStPoint().xCoord;
        double ySt1 = line1.getStPoint().yCoord;
        double xEn1 = line1.getEndPoint().xCoord;
        double yEn1 = line1.getEndPoint().yCoord;
        double xSt2 = line2.getStPoint().xCoord;
        double ySt2 = line2.getStPoint().yCoord;
        double xEn2 = line2.getEndPoint().xCoord;
        double yEn2 = line2.getEndPoint().yCoord;
        CutCounterPkg.Line.Point pSt1 = new CutCounterPkg.Line.Point((float) xSt1, (float) ySt1);
        CutCounterPkg.Line.Point pEn1 = new CutCounterPkg.Line.Point((float) xEn1, (float) yEn1);
        CutCounterPkg.Line.Point pSt2 = new CutCounterPkg.Line.Point((float) xSt2, (float) ySt2);
        CutCounterPkg.Line.Point pEn2 = new CutCounterPkg.Line.Point((float) xEn2, (float) yEn2);

        try {
            CutCounterPkg.Line ln1 = new CutCounterPkg.Line(pSt1, pEn1);
            CutCounterPkg.Line ln2 = new CutCounterPkg.Line(pSt2, pEn2);
            return CutCounterPkg.Line.getFullAngle(ln1, ln2);
        } catch (WrongLineExeption wrongLineExeption) {
            wrongLineExeption.printStackTrace();
            System.out.println("вырожденная линия. этап проверки направления контура");
            return 0;
        }
    }

    public static boolean isCCW(List<DStVLine> incomeContour) {
        double angleSum = 0;
        for (int i = 0; i < incomeContour.size() - 1; i++) {
            angleSum += getFullAngle(incomeContour.get(i), incomeContour.get(i + 1));
        }
        angleSum += getFullAngle(incomeContour.get(incomeContour.size() - 1), incomeContour.get(0));
        return angleSum > 0;
    }

    //вариант метода isClosed через StreamAPI. Работает примерно в 4 раза медленнее, но зато
    //кратко, красиво и ясно.
    private boolean isClosedByStreamAPI(List<DStVLine> countour) {
        List<DStVLine.Point> pointList = new ArrayList<>();
        for (DStVLine line : countour) {
            pointList.add(line.getStPoint());
            pointList.add(line.getEndPoint());
        }

        Map<Double, Map<Double, List<DStVLine.Point>>> pointmap =
                pointList.stream().collect(Collectors.groupingBy(DStVLine.Point::getxCoord,
                        Collectors.groupingBy(DStVLine.Point::getyCoord)));

        for (Double x : pointmap.keySet()) {
            for (Double y : pointmap.get(x).keySet()) {
                if (pointmap.get(x).get(y).size() != 2) return false;
            }
        }
        return true;
    }

    // метод-фильтратор. принимает лист окружностей, возвращает лист отверстий. список переданных
    // отверстий не изменяется: на выходе новый объект
    private List<Circle> filterHoles(List<Circle> candidateList, double diamThreshold) {
        // поскольку в ходе анализа мы можем обнаруживать ошибку графических данных - пересечение
        // окружностей, которого практически не может быть на нормальном чертеже по крайней мере
        // с отображением только видимых линий - будем логировать такие события. Сам логгер
        // находится во вспомогательном методе класса DXFParserExtension.
        StringBuffer log = new StringBuffer();
        //создаем контейнер, куда будем складывать выявленные отверстия
        List<Circle> localCandList = new ArrayList<>();
        //создаем контейнер, в котором будем хранить для каждой окружности ее вычисленный статус,
        // индекс статуса и индекс его окружности в листе окружностей совпадают.
        List<Boolean> statusList = new ArrayList<>();
        //берем каждое отверстие...
        for (int i = 0; i < candidateList.size(); i++) {
            //и сравниваем с каждым... (каждое отверстие в итоге будет сравнено с каждым другим
            // дважды, встав на место сначала первого а потом второго аргумента, это необходимо
            // по причине некоммутативности операции)
            for (int j = 0; j < candidateList.size(); j++) {
                //...кроме самого себя
                if (i == j) {
                    // если это отверстие последнее, то шанс обнаружить ложность его статуса уже
                    // нулевой (больше не с чем сравнить), поэтому записываем его в статус-лист
                    // со статусом отверстия
                    if (i == candidateList.size() - 1) {
                        statusList.add(true);
                    }
                    //ну а если отверстие от 0 до предпоследнего, то просто пропускаем сравнение
                    // с самим собой
                    continue;
                }
                //определяем статус в паре вызовом внешней функции, используем "штатный" лог
                // ошибок этого класса для записи в том числе и ошибок с пересечением отверстий
                boolean state = DXFParserExtension.checkHolesPare(candidateList.get(i),
                        candidateList.get(j), log);
                //если статус отверстия однажды потерян, то никакие дальнейшие сравнения его не
                // восстановят, поэтому прекращаем для него поиск незамедлительно, и записываем
                // статус ложности в статус-лист
                if (!state) {
                    statusList.add(false);
                    break;
                }
                //если дошли до последнего отверстия, но ложность так и не была выявлена, то уже
                // и не будет, поэтому вписываем в статус-лист статус истинности отверстия
                if (j == candidateList.size() - 1) {
                    statusList.add(true);
                }
            }
        }

        // строим список отверстий на вывод. пробегаем весь лист "кандидатов", проверяем что
        // диаметр кандидата не превышает установленный пороговый, а также проверяем, что его
        // статус - истина. Если оба условия соблюдены, то добавляем его в output, взяв по
        // индексу. Нельзя сразу отфильтровать все маленькие от больших, хотя это и ускорило бы
        // вычисления, сократив цикл (выше), потому что пересечения нам важны безотносительно
        // диаметра
        for (int i = 0; i < candidateList.size(); i++) {
            if (candidateList.get(i).getRadius() * 2 <= diamThreshold &&
                    statusList.get(i))
                localCandList.add(candidateList.get(i));
        }
        //дополняем варнинг-лог собранными сообщениями
        warningLog.add(new String(log));
        //и выкидываем результат
        return localCandList;
    }

    public List<Circle> getAllHoles(DXFContainer cont) {
        //создаем лист всех окружностей, на базе их списка из контейнера
        List<Circle> allCircles = new ArrayList<>(cont.circleList);
        List<Arc> arcs = cont.arcList;
        //добавляем к списку окружности, экстрагированные из полилиний
        arcs.addAll(extractArcsFromPolylines(cont.polylineBlocks, 0));
        //...и из дуг
        allCircles.addAll(extractCirclesFromArcs(arcs));
        // фильтруем список окружностей, отсеивая все, не являющиеся отверстиями
        List<Circle> filtredCircles = filterHoles(allCircles, diamThreshold);
        //из списка окружностей удаляем все те, что прошли через фильтр и стали отверстиями
        for (Circle c : filtredCircles) {
            allCircles.remove(c);
        }
        // Теперь необходимо возмратить "зависшие" в allCircles оставшиеся окружности в переданный
        // контейнер. Для этого преобразуем их в пары дуг, и добавляем в подконтейнер с дугами.
        for (Circle c : allCircles) {
            cont.arcList.addAll(DXFParserExtension.fromCircleToArcs(c));
        }
        //рафинированные отверстия возвращаем
        return filtredCircles;
    }

    // Метод с основной задачей - возврат всех контуров. "Побочная задача" - возврат листа
    // отверстий (он все равно неизбежно образуется в результате работы, и такой дизайн позволяет
    // экономить время там, где предполагается получение обоих списков, выполняя один вызов)
    public List<List<DStVLine>> getAllContours(DXFContainer cont, List<Circle> holes) {
        List<Line> allLines = new ArrayList<>(cont.lineList);
        allLines.addAll(extractLinesFromPolylines(cont.polylineBlocks, 0));
        // тяжеловесное решение, но нет проще способа очистить лист дуг от участков окружностей,
        // иначе как вызвав экстракцию окружностей. По этой причине в сигнатуре добавлен
        // "побочный" аутпут листа окружностей
        List<Circle> outHoles = getAllHoles(cont);
        holes.addAll(outHoles);
        List<Arc> filtredArcs = cont.arcList;
        return combineContours(allLines, filtredArcs);
    }

    private static Arc transformPolylineToArc(Polyline pl1, Polyline pl2) {
        double xs = pl1.getxCoord();
        double ys = pl1.getyCoord();
        double b = pl1.getBulge();
        double xe = pl2.getxCoord();
        double ye = pl2.getyCoord();
        //если точки совпадают то нельзя ничего построить между ними
        if (xs == xe && ys == ye) return null;
        //угол между y=0 и линией st-centre
        double gamma = atan((ys - ye) / (xs - xe)) + PI / 2 - 2 * atan(b);
        //длина хорды st-end
        double l = sqrt(pow(abs(ys - ye), 2) + pow(abs(xs - xe), 2));
        //радиус дуги
        double rad = (l * (b * b + 1)) / (4 * b);
        //смещения центра дуги относительно точки st
        double xtr = rad * cos(gamma);
        double ytr = rad * sin(gamma);
        if (xs > xe) {
            xtr = -xtr;
            ytr = -ytr;
        }
        //координаты центра
        double xcenter = xs + xtr;
        double ycenter = ys + ytr;

        //охват угла дугой
        double hugedAng = 4 * atan(b);
        double stAngle;
        double endAngle;

        //раздельно для положительной и отрицательной выпуклости
        if (b >= 0) {
            // В связи с разрывностью функции арктангенса, для положительной выпуклости база
            // отсчета угла резко меняется на PI, когда начальная точка левее конечной (по оси X).
            // Компенсируем.
            if (xs < xe) {
                gamma += PI;
            }
            stAngle = gamma;
            endAngle = stAngle + hugedAng;
        } else {
            //...для отрицательной выпуклости - когда начальная точка правее конечной (по оси X).
            // Компенсируем.
            if (xs > xe) {
                gamma += PI;
            }
            endAngle = gamma;
            stAngle = endAngle + hugedAng;
        }

        //убираем целое число оборотов
        stAngle = stAngle % (2 * PI);
        endAngle = endAngle % (2 * PI);

        //убираем отрицательное значение (проворотом на +360 градусов)
        if (stAngle < 0) stAngle += 2 * PI;
        if (endAngle < 0) endAngle += 2 * PI;

        //надо все поокруглять согласно паттерну
        xcenter = Double.parseDouble(String.format(Locale.ROOT, rdPattern, xcenter));
        ycenter = Double.parseDouble(String.format(Locale.ROOT, rdPattern, ycenter));
        rad = Double.parseDouble(String.format(Locale.ROOT, rdPattern, rad));
        /* дебаг блок
        System.out.println("gamma: " + toDegrees(gamma));
        System.out.println("start ang: " + toDegrees(stAngle));
        System.out.println("end ang: " + toDegrees(endAngle));
        System.out.println("x centre: " + xcenter);
        System.out.println("y centre: " + ycenter);
        System.out.println("radius: " + rad);
        */
        return new Arc(xcenter, ycenter, abs(rad), toDegrees(stAngle), toDegrees(endAngle));
    }

    //возвращает ноль, даже если на входе пустой список.
    public static int getIndexOfExternal(List<List<DStVLine>> contours) {

        double eReach = Double.MIN_VALUE;
        double wReach = Double.MAX_VALUE;
        double nReach = Double.MIN_VALUE;
        double sReach = Double.MAX_VALUE;

        int[] leaderIndexes = new int[4];
        int contourIndex = 0;
        for (List<DStVLine> contour : contours) {
            for (DStVLine segment : contour) {
                double locEReach;
                double locWReach;
                double locNReach;
                double locSReach;
                if (segment.getRadius() != 0) {
                    Arc arc = DXFParserExtension.fromDStVLineToArc(segment);
                    assert arc != null;
                    locEReach = arc.getxCoordCentre() + DXFParserExtension.getMaxReach(arc,
                            0);
                    locWReach = arc.getxCoordCentre() - DXFParserExtension.getMaxReach(arc,
                            180);
                    locNReach = arc.getyCoordCentre() + DXFParserExtension.getMaxReach(arc,
                            90);
                    locSReach = arc.getyCoordCentre() - DXFParserExtension.getMaxReach(arc,
                            270);
                } else {
                    locEReach = Math.max(segment.getStPoint().xCoord, segment.getEndPoint().xCoord);
                    locWReach = Math.min(segment.getStPoint().xCoord, segment.getEndPoint().xCoord);
                    locNReach = Math.max(segment.getStPoint().yCoord, segment.getEndPoint().yCoord);
                    locSReach = Math.min(segment.getStPoint().yCoord, segment.getEndPoint().yCoord);
                }
                if (locEReach > eReach) {
                    leaderIndexes[0] = contourIndex;
                    eReach = locEReach;
                }
                if (locWReach < wReach) {
                    leaderIndexes[1] = contourIndex;
                    wReach = locWReach;
                }
                if (locNReach > nReach) {
                    leaderIndexes[2] = contourIndex;
                    nReach = locNReach;
                }
                if (locSReach < sReach) {
                    leaderIndexes[3] = contourIndex;
                    sReach = locSReach;
                }
            }
            contourIndex++;
        }

        for (int i = 0; i < 3; i++) {
            if (leaderIndexes[i] != leaderIndexes[i + 1]) return -1;
        }
        return leaderIndexes[0];
    }

    //TODO Спорная логика в случае, если контуров нет вообще
    public String getDStVRep(String header) {
        StringBuilder sb = new StringBuilder(header);
        DXFContainer c = getComponentContainer();
        ArrayList<Circle> byProductCList = new ArrayList<>();
        List<List<DStVLine>> contours = getAllContours(c, byProductCList);
        int index = getIndexOfExternal(contours);
        DStVContour ext;
        //дублирует код в Starter, который был сделан специально для коннекта с Delphi. правильное размещение этого
        // кода - здесь
        if (contours.size() != 0 && index != -1) {
            ext = new DStVContour(contours.get(index));
        } else ext = null;
        ArrayList<DStVContour> internals = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            if (i != index) {
                internals.add(new DStVContour(contours.get(i)));
            }
        }

        //заносим данные
        if (byProductCList.size() != 0) {
            sb.append("\nBO\n");
            for (Circle cir : byProductCList) {
                sb.append(DXFParserExtension.getHoleRecord(cir, "v")).append("\n");
            }
        }

        if (ext != null && ext.lineSet != null) {
            sb.append(getContourRecord(ext, DXFParserExtension.ContourType.AK, "v")).append("\n");
            if (isCCW(ext.lineSet)) sb.append("**clockwise\n");
            else sb.append("**counterclockwise\n");
        }

        if (internals.size() > 0) {
            for (DStVContour con : internals) {
                if (con != null && con.lineSet != null && con.lineSet.size() > 0) {
                    sb.append(getContourRecord(con, DXFParserExtension.ContourType.IK, "v")).append("\n");
                    if (isCCW(con.lineSet)) sb.append("**clockwise\n");
                    else sb.append("**counterclockwise\n");
                }
            }
        }
        return sb.toString();
    }
}