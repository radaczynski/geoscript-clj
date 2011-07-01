(ns geoscript.proj
  (:import
   [org.geotools.geometry.jts JTS]
   [org.geotools.factory Hints]
   [org.geotools.referencing CRS ReferencingFactoryFinder]))

(defonce *crsfactory* (ReferencingFactoryFinder/getCRSFactory nil))

(defn proj-from-wkt
  "Function to return a GeoTools projection object from a well know text"
  [wkt]
  (. *crsfactory* createFromWKT  wkt))

(defn proj->epsg
  "Returns a epsg code from a CRS object"
  [crs]
     (System/setProperty "org.geotools.referencing.forceXY" "true")
     (Hints/putSystemDefault (Hints/COMPARISON_TOLERANCE) 1e-9)
     (format "EPSG:%s"(CRS/lookupEpsgCode crs true)))

(defn epsg->proj  
  "Function to return a GeoTools projection object from a EPSG code"
  [epsg]
  (. CRS decode epsg))

(defn get-area
  "Returns the area of validity for a given projection"
  [projection]
   (first (.getGeographicElements (.getDomainOfValidity projection))))

(defn transform
  "Function to transform a geometry from one EPSG code to another"
  [geometry input-epsg output-epsg]
  (. JTS transform geometry
     (. CRS findMathTransform
        (epsg->proj input-epsg)
        (epsg->proj output-epsg))))

(defn find-feature-epsg [feature-source]
  "Function to look up an epsg code from a GeoTools FeatureSource"
  (try
    (proj->epsg (.getCoordinateReferenceSystem (.getSchema feature-source)))
    (catch Exception _)))


