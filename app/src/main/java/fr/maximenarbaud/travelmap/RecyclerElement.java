package fr.maximenarbaud.travelmap;

public class RecyclerElement {
    private final String firstLine;
    private final String secondLine;
    private final int resourceId;
    private final String meta;
    private final int backgroundTint;

    private final String pointId;

    public String getFirstLine() { return this.firstLine; }
    public String getSecondLine() { return this.secondLine; }
    public int getResourceId() { return this.resourceId; }
    public String getMeta() { return this.meta; }
    public int getBackgroundTint() { return this.backgroundTint; }
    public String getPointId() { return this.pointId; }

    public RecyclerElement(String firstLine, String secondLine, int resourceId, String meta, int backgroundTint, String pointId) {
        this.firstLine = firstLine;
        this.secondLine = secondLine;
        this.resourceId = resourceId;
        this.meta = meta;
        this.backgroundTint = backgroundTint;

        this.pointId = pointId;
    }
}
