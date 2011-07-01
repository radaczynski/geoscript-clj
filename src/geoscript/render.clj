(ns geoscript.render
  (:use
   [clojure.contrib.seq-utils :only (seq-on)]
   [geoscript.io])
  (:import [org.geotools.data
            FeatureSource Query
            DefaultFeatureResults]
           [org.geotools.data.store ContentFeatureCollection]
           [org.geotools.data.memory MemoryFeatureCollection]
           [javax.imageio ImageIO]
           [java.io File
            ByteArrayOutputStream
            ByteArrayInputStream
            FileOutputStream]
           [org.geotools.map DefaultMapContext
            MapContext
            GraphicEnhancedMapContext]
           [org.geotools.renderer.chart
            GeometryRenderer
            GeometryDataset]
           [javax.swing JFrame]
           [org.jfree.chart JFreeChart ChartPanel]
           [org.jfree.chart.plot XYPlot]
           [java.awt Color RenderingHints]
           [org.geotools.renderer.lite StreamingRenderer]
           [java.awt Rectangle]
           [java.awt.image BufferedImage] 
           [org.geotools.swing JMapFrame]))

(defn make-mapcontext
  [& options]
  (let [[title bgcolor transparent] options]
    (doto (GraphicEnhancedMapContext.)
      (.setTitle (or title "Default Map"))
      (.setBgColor (or bgcolor Color/white))
      (.setTransparent (or transparent false)))))

(defn make-map [map-config]
  "Makes a map from an hashmap"
  (let [mapcontext (apply make-mapcontext
                          (vals (dissoc map-config :layers)))]
    (doseq [layer (:layers map-config)]
       (.addLayer mapcontext (first layer) (second layer)))
    mapcontext))

(defn make-jmapframe [map-context]
  (doto (JMapFrame. map-context)
    (.setDefaultCloseOperation (JFrame/DISPOSE_ON_CLOSE))
       (.setSize 800 600)
       (.enableStatusBar true)
       (.enableToolBar true)
       (.setVisible true)))

(defmulti viewer (fn [x & more] (class x)))

(defmethod viewer
  GeometryDataset
  [dataset & {:keys [height width]
              :or {height 500 width 500}}]
  (let [render (GeometryRenderer.)
        plot (XYPlot. dataset
                      (.getDomain dataset)
                      (.getRange  dataset)
                      render)
        chart (doto (JFreeChart. plot)
                (.removeLegend))
        panel (ChartPanel. chart)]
    (doto (JFrame.) 
      (.setContentPane panel)
      (.setVisible true)
      (.setSize 500 500))))

(defmethod viewer
  GraphicEnhancedMapContext
  [map-context]
  (make-jmapframe map-context))

(derive MemoryFeatureCollection ::collections)
(derive DefaultFeatureResults   ::collections)
(derive ContentFeatureCollection ::collections)

(defmethod viewer
  ::collections
    [gt-collection style]
    (let [map-context (make-mapcontext)]
      (.addLayer map-context gt-collection style)
      (make-jmapframe map-context)))

(defn make-render [map-context graphics screen-area extent]
    (doto (StreamingRenderer.)
      (.setJava2DHints
       (RenderingHints.
        RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON))
      (.setContext  map-context)
      (.paint graphics screen-area extent)))

(defn render
  [feature-collection output extent
   & {:keys [height width style]
      :or {height 100 width 100 style nil}}]
  (let [image (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)
        graphics (.createGraphics image)
        screen-area (Rectangle. 0 0 width height)
        mapcontext (make-mapcontext)]
    (.addLayer mapcontext feature-collection style)
    (make-render mapcontext graphics screen-area extent)
    (.dispose mapcontext)
    (ImageIO/write image "png" output)))


