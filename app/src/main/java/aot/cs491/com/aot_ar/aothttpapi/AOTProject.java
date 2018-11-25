package aot.cs491.com.aot_ar.aothttpapi;

import com.github.filosganga.geogson.model.Geometry;

import java.util.Date;

public class AOTProject {
    String name;
    String slug;
    Date firstObservation;
    Date latestObservation;
    Geometry bbox;
    Geometry hull;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Date getFirstObservation() {
        return firstObservation;
    }

    public void setFirstObservation(Date firstObservation) {
        this.firstObservation = firstObservation;
    }

    public Date getLatestObservation() {
        return latestObservation;
    }

    public void setLatestObservation(Date latestObservation) {
        this.latestObservation = latestObservation;
    }

    public Geometry getBbox() {
        return bbox;
    }

    public void setBbox(Geometry bbox) {
        this.bbox = bbox;
    }

    public Geometry getHull() {
        return hull;
    }

    public void setHull(Geometry hull) {
        this.hull = hull;
    }
}
