package DXFParserPkg;

import DXFParserPkg.DXFPrimitives.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.DoubleStream;

import static java.lang.String.format;

public enum DXFPatterns {
    // работа с мультипримитивами реализована криво. В частности, для полилинии, признак " 70" -
    // групповой, а не персональный для каждого участка. В такой реализации, как есть, у каждого
    // участка - свой признак 70, но захват актуального произойдет в нулевой участок, откуда он
    // и может быть считан. Но это сработает только если группа 70 встретится раньше начала групп
    // координат фрагментов. Если она встретится после координат, то будет захвачена в последний
    // участок. Пока так.
    CIRCLE("CIRCLE", Arrays.asList(" 10", " 20", " 40"), Circle.class),
    ARC("ARC", Arrays.asList(" 10", " 20", " 40", " 50", " 51"), Arc.class),
    LINE("LINE", Arrays.asList(" 10", " 20", " 11", " 21"), Line.class),
    POLYLINE("LWPOLYLINE", Arrays.asList(" 10", " 20", " 42", " 70"), true, Polyline.class,
            PolylineBlock.class, 2, 3),
    ERROR("ERR", Collections.singletonList("error"), null);

    String sectionMark;
    List<String> valuesMarkList;
    List<Integer> optionalArgsIndexes = new ArrayList<>();
    boolean multiObj;
    //этот элемент нужен для того, чтобы с использованием Reflection раскидывать примитивы по
    // контейнерам. Для мультипримитивов создаются агрегаты из субпримитивов, и в этом случае тип
    // должен быть именно типом агрегата (а не субпримитива). В противном
    // случае, в контейнере DXFContainer не будет найдено ни одного метода добавления,
    // принимающего representationDataClass как аргумент, и произойдет NullPointerException
    Class<? extends DXFComponent> representationDataClass;
    Class<? extends DXFComponent> reprMultiClass;
    Constructor<?> typeConstructor;

    DXFPatterns(String sMark, List<String> vMarkList, Class<? extends DXFComponent> clazz, Integer... oai) {
        this.sectionMark = sMark;
        this.valuesMarkList = vMarkList;
        optionalArgsIndexes.addAll(Arrays.asList(oai));
        representationDataClass = clazz;

        if (!sMark.equals("ERR")) {
            Constructor<?>[] constructors = this.representationDataClass.getConstructors();
            for (Constructor<?> c : constructors) {
                Annotation[] annots = c.getAnnotations();
                for (Annotation anno : annots) {
                    if (anno.annotationType() == FactoryConstructor.class) typeConstructor = c;
                }
            }
            if (typeConstructor == null) {
                System.out.println(format("Ошибка создания фабрики - нет фабричного конструктора для " +
                        "примитива %s", this.name()));
            }
        }
    }

    DXFPatterns(String sMark, List<String> vMarkList, Boolean multiObj,
                Class<? extends DXFComponent> clazz, Class<
            ? extends DXFComponent> multiClazz, Integer... oai) {
        this(sMark, vMarkList, clazz, oai);
        this.multiObj = multiObj;
        reprMultiClass = multiClazz;
    }

    static DXFPatterns getType(String name) {
        for (DXFPatterns pat : values()) {
            if (pat.sectionMark.equals(name)) return pat;
        }
        return ERROR;
    }

    static List<String> getAllTypeNames() {
        List<String> sectionsTypes = new ArrayList<>();
        for (DXFPatterns dxfpat : values()) {
            sectionsTypes.add(dxfpat.sectionMark);
        }
        return sectionsTypes;
    }

    public DXFComponent createComponent(List<Double> args) {
        DXFComponent component = null;
        double[] argums = new double[args.size()];
        int index = 0;
        for (double d : args) {
            argums[index] = d;
            index++;
        }
        try {
            if (typeConstructor == null) {
                System.out.println("Нет специализированного конструктора фабрики! будет получен " +
                        "пустой примитив");
                return null;
            } else {
                component = this.representationDataClass.
                        cast(typeConstructor.newInstance(DoubleStream.of(argums).boxed().toArray()));
            }
        } catch (SecurityException | InstantiationException |
                IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return component;
    }

    // небольшая "архитектурная особенность": для мультипримитивов создается еще одна фабрика,
    // генерирующая сборки из субпримитивов. Здесь пока без Reflection
    public DXFComponent createComponent(DXFComponent[] args) {
        DXFComponent component = null;
        switch (this) {
            case POLYLINE: component = new PolylineBlock(args);
        }
        return component;
    }
}
