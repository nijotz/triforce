(ns triforces.core)

(defn drawSquare []
    (let [target (.getElementById js/document "canvas")
          context (.getContext target "2d")]
        (.fillRect context 0 0 100 100)))

(defn ^:export init []
    (drawSquare))
