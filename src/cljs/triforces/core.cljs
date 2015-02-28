(ns triforces.core)

(enable-console-print!)

(def mouse-state (atom {:mousex -1 :mousey -1}))

(defn render-scene [[ctx width height] state]
    ;(println "Render scene")
    (set! (. ctx -fillStyle) "#000")
    ;(println "Actors: " (get state :actors))
    (doseq [actor (get state :actors)]
        ;(println "Actor: " actor)
        (.beginPath ctx)
        (.arc ctx (get actor :x) (get actor :y) 5, 0, (* 2 (.-PI js/Math)), false)
        (set! ctx -fillStyle "green")
        (.fill ctx)))

(defn create-state [] {:actors []})

(defn update-state [state]
    ;(println "Update state")
    (->
        (let [x (@mouse-state :mousex)
              y (@mouse-state :mousey)]
            (if (and (> x 0) (> y 0)) (do
                ;(println "x: " x)
                ;(println "y: " y)
                (clear-mouse-state)
                (create-actor state x y))
            (identity state)))
        move-actors))

(defn move-actors [state]
    (assoc state :actors
        (map (fn [actor] (update-in actor [:x] inc))
            (get state :actors))))

(defn create-actor [state x y]
    ;(println "Create actor")
    (assoc state
        :actors
        (conj (get state :actors) {:x x :y y :color "#F00"})))

(defn clear-screen [[ctx width height]]
    ;(println "Clear screen")
    (set! (. ctx -fillStyle) "#FFF")
    (.clearRect ctx 0 0 width height))

(defn tick [ctx state]
    ;(println "Tick")
    ;(println "State: " state)
    (clear-screen ctx)
    (render-scene ctx state)
    (js/setTimeout (fn []
        (tick ctx (update-state state))) 33))

(defn context [width height]
    (let [target (.getElementById js/document "canvas")]
        ;(println "Canvas: " target)
        [(.getContext target "2d")
            (set! (. target -width) width)
            (set! (. target -height) height)]))

(defn hook-input-events []
    (.addEventListener js/document "click"
        (fn [e]
            ;(println "Click")
            (update-mouse-state (. e -clientX) (. e -clientY))
            false)))

(defn update-mouse-state [x y]
    (swap! mouse-state assoc :mousex x)
    (swap! mouse-state assoc :mousey y))

(defn clear-mouse-state []
    (swap! mouse-state assoc :mousex -1)
    (swap! mouse-state assoc :mousey -1))

(defn ^:export init []
    (let [ctx (context 640 480)] [
        ;(println "Context: " ctx)
        (hook-input-events)
        (tick ctx (create-state))]))
