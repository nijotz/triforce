(ns triforces.core)

(enable-console-print!)

; Make JS math easier to use
(def pi (.-PI js/Math))
(def tau (* pi 2))
(def sqrt (.-sqrt js/Math))
(def abs  (.-abs  js/Math))
(def sin  (.-sin  js/Math))
(def cos  (.-cos  js/Math))
(def rand1  (.-random  js/Math))
(defn sqr [x] (* x x))
(defn avg [& col]
    (/ (reduce + col) (count col)))

(defn create-state [context width height] {
    :context context
    :ticks 33
    :ms_per_tick (/ 1000 33)
    :next_tick (.getTime (js/Date.))
    :width width
    :height height
    :actors []})

(defn apply-actors [state func]
    (assoc state :actors
        (for [actor (state :actors)]
            (func state actor))))

(defn render-actor [state actor]
    (let [ctx (state :context)]
        ; Draw circle
        (.beginPath ctx)
        (.arc ctx (nth (actor :coords) 0) (nth (actor :coords) 1) 5 0 tau false)
        (set! ctx -fillStyle (actor :color))
        (.fill ctx)
        (identity actor)

        ; Draw heading
        (.beginPath ctx)
        (.moveTo ctx (nth (actor :coords) 0) (nth (actor :coords) 1))
        (let [move-coords (move-vector (actor :coords) (actor :heading) 20)]
            (.lineTo ctx (nth move-coords 0) (nth move-coords 1)))
        (.stroke ctx) ))

(defn update-actors [state]
    (-> state
        move-actors
        avg-actors-heading))

(defn render-scene [state]
    (apply clear-screen (map state '(:context :width :height)))
    (doseq [actor (state :actors)] (render-actor state actor)))

(defn update-scene [state]
    (-> state
        handle-mouse-click
        update-actors))

(defn vec-len [& coords]
    (sqrt (apply + (map sqr coords))))

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
                attract-attractors (map :coords (state :actors)) ))))

; Assumes 2D
(defn move-vector [coords heading length]
    (apply
        (fn [x y] (vector
            (+ x (* (cos heading) length))
            (+ y (* (sin heading) length)) ))
        coords))

(defn avg-actors-heading [state]
    (let [avg-heading (apply avg (map :heading (state :actors)))]
        (apply-actors state
            (fn [state actor] (avg-actor-heading avg-heading actor)) )))

(defn avg-actor-heading [avg-heading actor]
    (update-in actor [:heading] (fn [heading] (avg heading avg-heading))))

(defn move-actors [state actor]
    (apply-actors state move-actor))

(defn move-actor [state actor]
    (update-in actor [:coords]
        (fn [coords] (move-vector coords (actor :heading) (actor :velocity))) ))

(defn create-actor [state coords]
    (assoc state
        :actors
        (conj (get state :actors) {
            :coords coords
            :color "#F00"
            :heading (* (rand1) tau)
            :velocity 1})))

(defn clear-screen [ctx width height]
    (set! (. ctx -fillStyle) "#FFF")
    (.clearRect ctx 0 0 width height))

(defn update-if-needed [timestamp state]
    (if (> timestamp (+ (state :next_tick) (* (state :ms_per_tick) 10))) (do
        (println "Resuming from pause")
        (update-scene (assoc state :next_tick timestamp)) )
    ; else if
    (if (> timestamp (state :next_tick))
        (update-scene (update-in state [:next_tick]
            (partial + (state :ms_per_tick)) ))
    ; else
        (identity state) )))

(defn game-loop [state]
    (let [
        timestamp (.getTime (js/Date.))
        interp (/ (- (state :next_tick) timestamp) (state :ms_per_tick))]

        (render-scene state interp)
        (js/requestAnimationFrame (fn []
            (game-loop (update-if-needed timestamp state))) )))

(defn context [width height]
    (let [target (.getElementById js/document "canvas")]
        [(.getContext target "2d")
            (set! (. target -width) width)
            (set! (. target -height) height)]))

(defn handle-mouse-click [state]
    (let [x (@mouse-state :mousex)
          y (@mouse-state :mousey)]
        (if (and (> x 0) (> y 0)) (do
            (clear-mouse-state)
            (create-actor state [x y]) )
        ; else
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
    (game-loop (apply create-state (context 640 480))))
