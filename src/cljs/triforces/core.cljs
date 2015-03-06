(ns triforces.core)

(enable-console-print!)

(def pi (.-PI js/Math))
(def tau (* pi 2))
(def sqrt (.-sqrt js/Math))
(def abs  (.-abs  js/Math))
(defn sqr [x] (* x x))
(defn avg [& col]
    (result (/ (reduce + col) (count col))))

(defn vec-len [& coords]
    (sqrt (apply + (map sqr coords))))

(defn create-state [context width height] {
    :context context
    :width width
    :height height
    :actors []})

(defn apply-actors [state func]
    (assoc state :actors
        (for [actor (state :actors)]
            (func state actor))))

(defn render-actor [state actor]
        (.beginPath (state :context))
        (.arc ctx (nth (actor :coords) 0) (nth (actor :coords) 1) 5 0 tau false)
        (set! ctx -fillStyle (actor :color))
        (.fill ctx)
        (identity actor))

(defn update-actors [state]
    (apply-actors state move-actor))

(defn render-scene [state]
    (let [ctx (state :context)]
        (doseq [actor (state :actors)]
            (println "Rendor actor: " actor)
            (.beginPath ctx)
            (.arc ctx (nth (actor :coords) 0) (nth (actor :coords) 1) 5 0 tau false)
            (set! ctx -fillStyle (actor :color))
            (.fill ctx))))

(defn update-scene [state]
    (-> state
        handle-mouse-click
        update-actors))

(defn distance [coords1 coords2]
    (let [result (map - coords2 coords1)]
        (identity result)))

(defn attract [x y]
    (let [distance (- x y)]
        (if (= distance 0)
            (identity 0)
            (/ 10 (sqr distance)))))

(defn attract-one [base attractor]
    ; Returns a vector representing the affect of the attractor on the base
    (let [result (map attract base attractor)]
        (println "Base: " base)
        (println "Attractor: " attractor)
        (println "Result: " result)
        (identity result)))

(defn attract-many [base & attractors]
    (println "Second Base: " base)
    (println "Second Attractors: " attractors)
    (let [
        attractors-result (map (partial attract-one base) attractors)]
        (println "Attractors-Result: " attractors-result)
        ; average each component of all the coordinates
        (map (partial apply avg)
            ; turn a list of grouped coordinates into a list of a single
            ; component of every coordinate
            (apply map vector
                ; apply attract to a list of attractors
                attractors-result))))

(defn attract-attractors [base attractors]
    (map + base (apply attract-many base attractors)))

(defn attract-actors [state]
    (assoc state :actors
        (for [actor (state :actors)]
            (update-in actor [:coords]
                attract-attractors (map :coords (state :actors))))))

(defn move-actor [state actor]
    (let [total (count (state :actors))]
        (update-in actor [:coords]
            (partial map (partial + total)))))

(defn create-actor [state coords]
    (assoc state
        :actors
        (conj (get state :actors) {
            :coords coords
            :color "#F00"
            :heading (* (/ 7 8) tau)
            :velocity 1})))

(defn clear-screen [ctx width height]
    (set! (. ctx -fillStyle) "#FFF")
    (.clearRect ctx 0 0 width height))

(defn tick [state]
    (apply clear-screen (map state '(:context :width :height)))
    (render-scene state)
    (js/setTimeout (fn []
        (tick (update-scene state))) 33))

(defn context [width height]
    (let [target (.getElementById js/document "canvas")]
        [(.getContext target "2d")
            (set! (. target -width) width)
            (set! (. target -height) height)]))

; Just mouse things
(defn handle-mouse-click [state]
    (let [x (@mouse-state :mousex)
          y (@mouse-state :mousey)]
        (if (and (> x 0) (> y 0)) (do
            (clear-mouse-state)
            (let [state (create-actor state [x y])]
                (identity state)))
        (identity state))))

(defn hook-input-events []
    (.addEventListener js/document "click"
        (fn [e]
            (println "Click")
            (update-mouse-state (. e -clientX) (. e -clientY))
            false)))

(def mouse-state (atom {:mousex -1 :mousey -1}))

(defn update-mouse-state [x y]
    (swap! mouse-state assoc :mousex x)
    (swap! mouse-state assoc :mousey y))

(defn clear-mouse-state []
    (swap! mouse-state assoc :mousex -1)
    (swap! mouse-state assoc :mousey -1))

(defn ^:export init []
    (hook-input-events)
    (tick (apply create-state (context 640 480))))
