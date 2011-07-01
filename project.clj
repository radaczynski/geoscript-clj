(defproject geoscript "1.0.0-SNAPSHOT"
  :description "Clojure version of the geoscript project"
  :repositories { "OpenGeo Maven Repository" "http://repo.opengeo.org"}
  :url "http://geoscript.org/"
  :main geoscript.core
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.geotools/gt-main "8-SNAPSHOT"]
                 [org.geotools/gt-swing "8-SNAPSHOT"]                 
                 [org.geotools/gt-epsg-hsql "8-SNAPSHOT"]
                 [org.geotools/gt-referencing "8-SNAPSHOT"]
                 [org.geotools.jdbc/gt-jdbc-postgis "8-SNAPSHOT"] 
                 [org.geotools.jdbc/gt-jdbc-h2 "8-SNAPSHOT"]                 
                 [org.geotools/gt-geojson "8-SNAPSHOT"]
                 [org.geotools/gt-charts "8-SNAPSHOT"]
                 [org.geotools/gt-render "8-SNAPSHOT"]
                 [org.geotools/gt-shapefile "8-SNAPSHOT"]])
