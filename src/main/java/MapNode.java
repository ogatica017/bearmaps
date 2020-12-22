/**
 * creates vertex instances
 * these instances are stored in the graph in GraphDB
 */

public class MapNode {
    public Long id;
    public Double lat;
    public Double lon;
    public String name;

    public MapNode(Long id, Double lat, Double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.name = null;
    }

    public void AddName(String name) {
        this.name = name;
    }

    public Long GetID() {
        return this.id;
    }

    public Double getLat() {
        return this.lat;
    }

    public Double getLon() {
        return this.lon;
    }
}