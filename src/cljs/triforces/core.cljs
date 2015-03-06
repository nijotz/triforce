(ns triforces.core)

(enable-console-print!)

(def sqrt (.-sqrt js/Math))
(def abs  (.-abs  js/Math))
(defn sqr [x] (* x x))
(defn avg [& col]
    (result (/ (reduce + col) (count col))))

(defn vec-len [& coords]
    (sqrt (apply + (map sqr coords))))

(def mouse-state (atom {:mousex -1 :mousey -1}))

(defn render-scene [[ctx width height] state]
    (println "Render scene")
    (set! (. ctx -fillStyle) "#000")
    ;(println "Actors: " (get state :actors))
    (doseq [actor (get state :actors)]
        ;(println "Actor: " actor)
        (.beginPath ctx)
        (.arc ctx (nth (actor :coords) 0) (nth (actor :coords) 1) 5 0 (* 2 (.-PI js/Math)) false)
        (set! ctx -fillStyle "green")
        (.fill ctx)))

(defn create-state [] {:actors []})

(defn update-state [state]
    (println "Update state")
    (-> state
        handle-mouse-click
        move-actors))
    (println "State: " state)

(defn handle-mouse-click [state]
    (let [x (@mouse-state :mousex)
          y (@mouse-state :mousey)]
        (if (and (> x 0) (> y 0)) (do
            (println "Handle click")
            (clear-mouse-state)
            (let [state (create-actor state [x y])]
                (println "State: " state)
                (identity state)))
        (identity state))))

(defn distance [coords1 coords2]
    (let [result (map - coords2 coords1)]
        (println "Coords1: " coords1)
        (println "Coords2: " coords2)
        (println "Result: " result)
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

(defn inc-based-on-actors [coords state]
    (let [total (count (state :actors))]
        (println "Inc Based")
        (println "State: " state)
        (println "Coords: " coords)
        (println "Total " total)
        (map (partial + total) coords)))

(defn move-actors [state]
    (assoc state :actors
        (for [actor (state :actors)]
            (update-in actor [:coords] inc-based-on-actors state))))

(defn create-actor [state coords]
    (println "Create actor")
    (println "Coords: " coords)
    (assoc state
        :actors
        (conj (get state :actors) {:coords coords :color "#F00"})))

(defn clear-screen [[ctx width height]]
    ;(println "Clear screen")
    (set! (. ctx -fillStyle) "#FFF")
    (.clearRect ctx 0 0 width height))

(defn tick [ctx state]
    (println "Tick")
    (println "State: " state)
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
            (println "Register click")
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
