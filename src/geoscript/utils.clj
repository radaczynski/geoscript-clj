(ns geoscript.utils)

(defmacro java-apply [instance method args]
  `(clojure.lang.Reflector/invokeInstanceMethod
    ~instance (name (quote ~method)) (to-array ~args)))
