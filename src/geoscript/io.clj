(ns geoscript.io
  (:require [clojure.contrib.string :as string])
  (:import
   [java.io File]
   [com.vividsolutions.jts.geom Geometry]
   [org.geotools.data.shapefile ShapefileDataStoreFactory]
   [org.geotools.swing ProgressWindow]
   [org.geotools.data DefaultTransaction DataUtilities]
   [org.geotools.feature FeatureCollection]
   [org.geotools.feature.simple SimpleFeatureBuilder]
   [org.geotools.data.memory MemoryFeatureCollection]
   [org.geotools.data DataStoreFinder]))

(defn data-store
  "Function to return a geotools datastore given an input string uri
   \"shp://path\"
   \"pg://user:pass@localhost:port/db\"
   \"h2://dbname\"
   "
  [uri]
  (when-let
      [params
       (condp #(.startsWith %2 %) uri
         "shp://" {"url"
                   (-> (.substring uri (count "shp://"))
                       File. .toURL)}
         "pg://" (when-let
                     [[_ user pass host port db]
                      (re-find
                       (re-pattern "pg://([^:]*):([^@]*)@([^:]*):([^/]*)/(.*)")
                       uri)]
                   {"dbtype" "postgis" "host" host "port" port
                    "user" user "passwd" pass "database" db})
         "h2://" {"dbtype" "h2"
                  "database" (.substring uri (count "h2://"))})]
    (DataStoreFinder/getDataStore params)))


(defn create-shapefile [file]
  "Function to create and return a new datastore shapefile"
  (let [factory (ShapefileDataStoreFactory.)]
    (.createNewDataStore factory
                         {"url" (-> file  (.toURI)(.toURL))
                          "create spatial index" true })))

(defn create-schema [schema]
  "Function to create a GeoTools SimpleFeatureType"
  (let [name (:name schema)
        string (StringBuilder.)]
    (doseq [propertry (:properties schema)]
      (.append string (str (string/join ":" propertry) ",")))
    (DataUtilities/createType name (.toString string))))

(defn get-schema [gt-feature-source]
  "Takes a GeoTools Feature Source and returns "
  (let [schema (.getSchema gt-feature-source)]
    {:name (.getLocalPart (.getName schema))
     :fields  (map #(vector ( .. % getName getLocalPart)
                           (.getBinding %)) (.getTypes schema))}))

(defn read-properties
  "Given a geotools feature, return a hash of properties that aren't geometries"
  [gt-feature]
  (let [geom-prop? #(-> % .getValue class (isa? Geometry))
        nongeom-properties (remove geom-prop?
                                   (.getProperties gt-feature))]
    (reduce (fn [hashmap field]
              (assoc hashmap
                (-> field .getDescriptor .getLocalName keyword)
                (.getValue field)))
            {}
            nongeom-properties)))

(defn make-geo-feature [gt-feature]
  "Given a geotools feature, create a hash of useful info"
  {:id (.getID gt-feature)
   :properties (read-properties gt-feature)
   :geometry (.getDefaultGeometry gt-feature)})

(defn make-lazy-feature-iterator
  "Create an object implementing iterator and featureiterator"
  [feature-builder feature-sequence]
  (let [features-state (atom (seq feature-sequence))]
    (reify
     java.util.Iterator
     org.geotools.feature.FeatureIterator
     (close [this])
     (hasNext [this] (boolean @features-state))
     (next [this] (let [current-state @features-state]
                    (swap! features-state next)
                    (feature-builder (first current-state)))))))

(defn make-feature-builder
  "Create a function that can take a clojure feature
   and generates a geotools feature"
  [feature-type]
  (let [feature-builder (SimpleFeatureBuilder. feature-type)]
    (fn [geo-feature]
      (doseq [prop-keyval (:properties geo-feature)]
        (.set feature-builder (name (key prop-keyval)) (val prop-keyval)))
      (.set feature-builder
            (.getLocalName (.getGeometryDescriptor feature-type))
            (:geometry geo-feature))
      (.buildFeature feature-builder (:id geo-feature)))))

(defn make-lazy-feature-collection
  "Creates a lazy implementation of a feature collection"
  [feature-type features-sequence]
  (let [feature-builder (make-feature-builder feature-type)]
    (reify
      FeatureCollection
      (getSchema [this] feature-type)
      (features [this]
                (make-lazy-feature-iterator feature-builder features-sequence))
      (iterator [this]
               (make-lazy-feature-iterator feature-builder features-sequence))
      (^void close [this ^java.util.Iterator iterator] nil))))

(defn make-eager-feature-collection
  "Uses the geotools memoryfeaturecollection to store all features in memory"
  [feature-type geo-sequence]
  (let [feature-builder (make-feature-builder feature-type)
        memory-feature-collection (MemoryFeatureCollection. feature-type)]
    (doseq [geo-feature geo-sequence]
      (.add memory-feature-collection (feature-builder geo-feature)))
    memory-feature-collection))

(defn read-features
  "Read features from data store and return a collection"
  [datastore & options]
  (let [{:keys [layer query]} (apply hash-map options)
        feature-source (if layer
                         (.getFeatureSource datastore layer)
                         (.getFeatureSource datastore))]
    (if query
      (.getFeatures feature-source query)
      (.getFeatures feature-source))))

(defn write-features
  "Writes a collection to an existing layer in a datastore"
  [datastore layer gt-collection]
  (let [transaction (DefaultTransaction. "add")
        feature-source (.getFeatureSource datastore layer)]
    (.setTransaction feature-source transaction)
    (try
      (.addFeatures feature-source gt-collection)
      (.commit transaction)
      (catch Exception ex
        (.rollback transaction)
        (throw ex))
      (finally (.close transaction)))))



(defmacro with-feature-source 
  "Macro that allows a user to iterate through FeatureSource. The
   feature collection iterator is closed after the body is evaluated.
   This does not convert the feature to a clojure map. see the
   with-features macro if you want to convert each feature into a
   clojure map.

   Arguments
    source-binding
    body 
   "
  [source-binding & body]
  `(let [gt-coll# ~(source-binding 1)
         source-iterator# (.iterator gt-coll#)
         ~(source-binding 0) (iterator-seq source-iterator#)]
     (try
       (doall ~@body)
       (finally (.close gt-coll# source-iterator#)))))
       
(defmacro with-features
  "Obtain a feature iterator, and close it out of scope"
  [features-binding & body]
  (if (or (not (vector? features-binding))
          (not (= 2 (count features-binding)))
          (not (symbol? (features-binding 0))))
    (throw (IllegalArgumentException. "bad features binding"))
    `(let [gt-coll# ~(features-binding 1)
           feature-iterator# (.iterator gt-coll#)
           feature-seq# (iterator-seq feature-iterator#)
           ~(features-binding 0) (map make-geo-feature feature-seq#)]
       (try
         ~@body
         (finally (.close gt-coll# feature-iterator#))))))

(defmacro process-features
  "gt-collection in -> transform features -> gt-collection out
   body will get a geo-feature, return type gets added to coll"
  [feature-binding & body]
  (if (or (not (vector? feature-binding))
          (not (= 2 (count feature-binding)))
          (not (symbol? (feature-binding 0))))
    (throw (IllegalArgumentException. "bad features binding"))
    `(let [gt-coll# ~(feature-binding 1)]
       (with-features [features# gt-coll#]
         (make-eager-feature-collection
          (.getSchema gt-coll#)
          (remove
           nil?
           (map (fn [~(feature-binding 0)] ~@body)
                features#)))))))


(defn make-filter [cql]
  "convenience functions for creating filters"
  (org.geotools.filter.text.cql2.CQL/toFilter cql))
