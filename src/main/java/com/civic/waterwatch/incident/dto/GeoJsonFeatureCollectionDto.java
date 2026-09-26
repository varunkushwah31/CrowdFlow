package com.civic.waterwatch.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonFeatureCollectionDto {
    private String type = "FeatureCollection";
    private List<Feature> features = new ArrayList<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Geometry {
        private String type;
        private Object coordinates;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Feature {
        private String type = "Feature";
        private Geometry geometry;
        private Map<String, Object> properties;

        public Feature(Geometry geometry, Map<String, Object> properties) {
            this.type = "Feature";
            this.geometry = geometry;
            this.properties = properties;
        }
    }

    public void addFeature(Geometry geometry, Map<String, Object> properties) {
        features.add(new Feature(geometry, properties));
    }
}
