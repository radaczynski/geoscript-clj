(ns geoscript.seq
  (:import  [com.vividsolutions.jts.geom
             MultiPoint
             MultiLineString
             MultiPolygon
             LineString
             Polygon
             LinearRing
             Point
             Coordinate]))

;; seq-on written by Konrad Hinsen
(defmulti seq-on
  "Returns a seq on the object s. Works like the built-in seq but as
   a multimethod that can have implementations for new classes and types."
  {:arglists '([s])}
  type)

(defmethod seq-on :default
  [s]
  (seq s))

(defmethod seq-on Coordinate [geometry]
  (list(.x geometry) (.y geometry)))

(defmethod seq-on Point [geometry]
  (list (.getX geometry) (.getY geometry)))

(defmethod seq-on ::lines [geometry]
  (map #(seq-on %) (.getCoordinates geometry)))

(derive LineString ::lines)
(derive LinearRing ::lines)

(defmethod seq-on Polygon [geometry]
  (let [interioir-rings (range (.getNumInteriorRing geometry))]
    (list (seq-on (.getExteriorRing geometry))
          (map #(seq-on (.getInteriorRingN geometry %)) interioir-rings ))))


(defn get-mulit [geometry]
  (map #(seq-on (.getGeometryN geometry %))
       (range (.getNumGeometries geometry))))

(defmethod seq-on MultiPoint [geometry]
  (get-mulit geometry))

(defmethod seq-on MultiPoint [geometry]
  (get-mulit geometry))

(defmethod seq-on MultiPolygon [geometry]
  (get-mulit geometry))

