package DXFParserPkg.DXFPrimitives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PolylineBlock implements DXFComponent {

    List<Polyline> polylines = new ArrayList<>();

    public PolylineBlock(List<Polyline> polylines) {
        this.polylines = polylines;
    }

    public PolylineBlock(DXFComponent[] polylines) {
        for (DXFComponent comp : polylines
        ) {
            this.polylines.add((Polyline) comp);
        }
    }

    public List<Polyline> getPolylines() {
        return polylines;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Polyline pl : polylines) {
            sb.append(pl.toString()).append("\n");
        }
        return sb.toString();
    }
}
