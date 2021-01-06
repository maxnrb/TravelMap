package fr.maximenarbaud.travelmap;

public class CityObj {
    final private String cityName;
    private Boolean selected;

    public CityObj(String cityName, Boolean selected) {
        this.cityName = cityName;
        this.selected = selected;
    }

    public String getCityName() { return cityName; }
    public Boolean getSelected() { return selected; }

    public void setSelected(Boolean selected) { this.selected = selected; }
}
