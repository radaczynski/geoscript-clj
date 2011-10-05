(ns geoscript.geometry
  (:require
   [clojure.data.json :as json])
  (:use
   [geoscript seq])
  (:import [org.geotools.geometry.jts JTS JTSFactoryFinder]
           [org.geotools.referencing CRS]
           [org.geotools.renderer.chart GeometryDataset]                      
           [com.vividsolutions.jts.io.gml2 GMLWriter GMLReader]
           [com.vividsolutions.jts.io WKBWriter WKBReader]
           [com.vividsolutions.jts.simplify DouglasPeuckerSimplifier]
           [com.vividsolutions.jts.geom
            Geometry
            MultiPoint
            MultiLineString
            MultiPolygon
            LineString
            Polygon
            LinearRing
            Point
            Coordinate]))


(defonce *factory* (JTSFactoryFinder/getGeometryFactory nil))
(defonce *reader* (com.vividsolutions.jts.io.WKTReader. *factory*))

(defn write-geometry [geometry out]
  (.print out (json/json-str {:type (.getGeometryType geometry)
                               :coordinates [(seq geometry)]})))

(extend Point json/Write-JSON
        {:write-json write-geometry })

(extend LineString json/Write-JSON
        {:write-json write-geometry })

(extend Polygon json/Write-JSON
        {:write-json write-geometry })

(extend MultiPoint json/Write-JSON
        {:write-json write-geometry })

(extend MultiLineString json/Write-JSON
        {:write-json write-geometry })

(extend MultiPolygon json/Write-JSON
        {:write-json write-geometry})

(defn geometry->wkb
  "Converts a JTS geometry to a well know binary"
  [geom]
  (.write (WKBWriter.) geom))

(defn wkb->geometry
  "Loads a well know binary to a JTS geometry"
  [wkb]
  (.read (WKBReader.) wkb))

(defn wkt->geometry
  "Creates a geometry from well known text" 
  [string]
  (.read *reader* (str string)))

(defn geometry->wkt
  [geometry]
  (.toText geometry))

(defn geometry->gml [geometry]
  (.write (GMLWriter.) geometry))

(defn gml->geometry [gml]
  (.read (GMLReader.) gml *factory*))

(defn create-coord
  "Creates a JTS Coordinate Seq"
  ([coords] (apply create-coord coords))
  ([x y] (Coordinate. x y))
  ([x y z] (Coordinate. x y z)))

(defn create-point
  "Creates a JTS Point from a X Y"
  ([p] (apply create-point p))
  ([x y] (.createPoint *factory* (create-coord x y)))
  ([x y z] (.createPoint *factory* (create-coord x y z))))

(defn create-line-string
  "Creates a JTS Linear ring"
  [line]
  (.createLineString *factory* (into-array (map create-coord line))))

(defn create-linear-ring
  "Creates a JTS Linear ring"
  [ring]
  (.createLinearRing *factory* (into-array (map create-coord ring))))

(defn create-polygon
  "Creates JTS Polygon"
  ([shell & holes]
     (.createPolygon
      *factory*
      (create-linear-ring shell)
      (and holes (into-array (map create-linear-ring holes))))))

(defn create-multi-point
  [points]
  (.createMultiPoint *factory*
                     (into-array
                      (map #(create-point (first %) (second %)) points))))

(defn create-multi-line-string
  [lines]
  (.createMultiLineString *factory*
                          (into-array (map #(create-line-string %) lines ))))

(defn create-multi-polygon
  [polygons]
  (.createMultiPolygon *factory*
                       (into-array (map #(create-polygon %) polygons))))


(defn simplify [geometry tolerance]
  "Convenience function to simplifing an geometry"
  (DouglasPeuckerSimplifier/simplify geometry tolerance))

(defn make-geometry-dataset [geometies]  
  (GeometryDataset. (into-array Geometry geometies)))

