(ns geoscript.style
  (:use [geoscript io])
  (:import
   [java.awt Color]
   [org.geotools.styling SLDParser
    GraphicImpl
    StyleBuilder Rule SLD RuleImpl]
   [org.geotools.factory CommonFactoryFinder]))

(def *style-factory*       (CommonFactoryFinder/getStyleFactory nil))
(def *filter-factory*      (CommonFactoryFinder/getFilterFactory nil))
(def *style-builder*       (StyleBuilder.))

(defn read-sld
  [path]
  (first
   (.readXML (SLDParser. *style-factory* (-> path java.io.File. .toURL)))))

(defn hex->color [hex]
  (Color/decode hex))

(defn make-literal [literal]
  (.literal *filter-factory* literal))

(defn make-fill [{:keys [color opacity]
                  :or {color "#ffffff" opacity 1.0}}]
  (.createFill *style-factory*
                 (make-literal (hex->color color))
                 (make-literal opacity)))

(defn make-stroke [{:keys [color width opacity]
                    :or {color "#808080" width 1.0 opacity 1.0}}]
  (.createStroke *style-factory*
                 (make-literal (hex->color color))
                 (make-literal width)
                 (make-literal opacity)))


(defn style-point [{:keys [well-know-name
                          line-color fill-color
                           opacity size label font]
                    :or {well-know-name "Square"
                         line-color "#808080"
                         fill-color "#ffffff"
                         opacity 1.0
                         size 1.0 }}]
  (SLD/createPointStyle well-know-name (hex->color line-color)
                        (hex->color fill-color)
                        opacity
                        size
                        label
                        font))

(defn style-line [{:keys [line-color width label font]
                  :or { line-color "#808080" width 1.0}}]
  (SLD/createLineStyle (hex->color line-color) width label font))

(defn style-polygon [{:keys [storke fill opacity label font]
                     :or   {storke "#808080" fill "#ffffff" opacity 1.0}}]
  (SLD/createPolygonStyle (hex->color storke) (hex->color fill)
                          opacity label font))

(defn symbolizer-point [& options]
  (.createPointSymbolizer *style-builder*))

(defn symbolizer-line [& options]
  (.createLineSymbolizer *style-builder*))

(defn symbolizer-polygon [style]
  (let [fill (make-fill (:fill style))
        stroke (make-stroke (:stroke style))]
    (.createPolygonSymbolizer *style-factory* stroke fill nil)))

(defn make-legend []
  (GraphicImpl. *filter-factory*))

(defn make-rule [symbolizers &
                 {:keys [legend name filter else-filter max min]
                  :or   {description {:title "Title"
                                 :description "Description"}
                    name "Default Style"}}]
  (doto (.createRule *style-builder* (into-array symbolizers))
    (.setFilter filter)
    (.setName name)))

(defn make-style [rules]
  (let [feature-type-style (.createFeatureTypeStyle *style-factory*
                                                    (into-array Rule rules))
        style (.createStyle *style-factory*)]
    (.add (.featureTypeStyles style) feature-type-style) style))
