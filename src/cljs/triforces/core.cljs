(ns triforces.core)

(enable-console-print!)

(defn render-scene [[ctx width height] state]
    (println "Render scene")
    (set! (. ctx -fillStyle) "#000")
    (.fillRect ctx 0 0 100 100))

(defn update-state [state]
    (println "Update state"))

(defn create-state [] {
    :actors ()})

(defn clear-screen [[ctx width height]]
    (println "Clear screen")
    (set! (. ctx -fillStyle) "#FFF")
    (.clearRect ctx 0 0 width height))

(defn tick [ctx state]
    (println "Tick")
    (clear-screen ctx)
    (render-scene ctx state)
    (js/setTimeout (fn []
        (tick ctx (update-state state))) 33))

(defn context [width height]
    (let [target (.getElementById js/document "canvas")]
        (println "Canvas: " target)
        [(.getContext target "2d")
            (set! (. target -width) width)
            (set! (. target -height) height)]))

(defn ^:export init []
    (let [ctx (context 640 480)] [
        (println "Context: " ctx)
        (tick ctx (create-state))]))
