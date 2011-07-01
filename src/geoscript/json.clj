(ns geoscript.json
  (:import
   [java.io StringWriter]
   [org.geotools.geojson.feature FeatureJSON]))

(def *fjson* (FeatureJSON.))

(defn feature->geojson [feature]
  "Writes a GeoTools Simple Feature to a "
  (let [writer (StringWriter.)]
    (.writeFeature *fjson* feature writer)
    writer))

(defn geojson->feature [json]
  "Reads a String and returns a GeoTools Simple Feature"
  (.readFeature *fjson* json))

(defn collection->geojson [coll]
  "Writes a GeoTools Feature Collection to a GeoJSON String"
  (let [writer (StringWriter.)]
    (.writeFeatureCollection *fjson* coll writer)
    writer))

(defn geojson->collection [json]
  "Takes a geojson feature collection and returns a GeoTools FeatureCollection"
  (.readFeatureCollection *fjson* json))
