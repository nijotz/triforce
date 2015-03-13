(ns triforces.core)

; Turn console.log into println
(enable-console-print!)

;;;
; Make JS math and misc functinos easier to use
;;;
(def pi (.-PI js/Math))
(def tau (* pi 2))
(def sqrt (.-sqrt js/Math))
(def abs  (.-abs  js/Math))
(def sin  (.-sin  js/Math))
(def cos  (.-cos  js/Math))
(def tan  (.-tan  js/Math))
(def atan2 (.-atan2 js/Math))
(def rand1 (.-random js/Math))
(defn rand
    ([] (rand1))
    ([min max] (+ (* (rand1) (- max min)) min)) )
(defn sqr [x] (* x x))
(defn avg [& col]
    (/ (reduce + col) (count col)))
(def get-timestamp (.-now js/Date))

;;;
; Just vector things
;;;
(defn move-vector
    ; Assumes 2D
    ([coords heading speed]
        (apply
            (fn [x y] (vector
                (+ x (* (cos heading) speed))
                (+ y (* (sin heading) speed)) ))
            coords))
    ; TODO: take time into account (ms_per_tick / 1000)
    ([coords velocity]
        (map + coords velocity) ))

(defn point-at
    ([coords1 coords2] (apply point-at (concat coords1 coords2)))
    ([x1 y1 x2 y2]
        (let [heading (atan2 (- x2 x1) (- y2 y1)) ]
        (vector (sin heading) (cos heading)) )))

(defn distance-squared [coords1 coords2]
    (apply + (map sqr (map - coords1 coords2))) )

(defn distance [coords1 coords2]
    (sqrt (distance-squared coords1 coords2)))

;;;
; Rendering
;;;
(defn clear-screen [ctx width height]
    (set! (. ctx -fillStyle) "#FFF")
    (.clearRect ctx 0 0 width height))

(defn interpolate-coords
    ([coords heading speed interp]
        (move-vector coords heading (* speed interp)) )
    ([coords velocity interp]
        (move-vector coords (map (partial * interp) velocity)) ))

(defn render-actor [ctx actor interp]
    (let [coords (interpolate-coords (actor :coords) (actor :velocity) interp)]
        ; Draw circle
        (.beginPath ctx)
        (.arc ctx (nth coords 0) (nth coords 1) (/ (actor :mass) 2) 0 tau false)
        (set! ctx -fillStyle (actor :color))
        (.fill ctx)

        ; Draw heading
        (.beginPath ctx)
        (.moveTo ctx (nth coords 0) (nth coords 1))
        (let [move-coords (move-vector coords (map (partial * 5) (actor :velocity)))]
            (.lineTo ctx (nth move-coords 0) (nth move-coords 1)))
        (.stroke ctx) ))

(defn render-middle [state]
    (let [midx (/ (state :width) 2)
          midy (/ (state :height) 2)
          ctx (state :context)]
    (.beginPath ctx)
    (.arc ctx midx midy 5 0 tau false)
    (set! ctx -fillStyle "green")
    (.fill ctx) ))

(defn render-scene [state interp]
    (apply clear-screen (map state '(:context :width :height)))
    (doseq [actor (state :actors)] (render-actor (state :context) actor interp))
    (display-fps state)
    (render-middle state))

;;;
; Updating
;;;
(defn update-if-needed [state timestamp]
    ; If more than 10 ticks have gone by without an update, assume we were
    ; paused because of requestAnimationFrame and set next_tick to now
    (if (> timestamp (+ (state :next_tick) (* (state :ms_per_tick) 10))) (do
        (println "Resuming from pause")
        (update-scene (assoc state :next_tick timestamp)) )
    ; else if
    (if (> timestamp (state :next_tick))
        (update-scene (update-in state [:next_tick]
            (partial + (state :ms_per_tick)) ))
    ; else
        (identity state) )))

(defn update-actors [state]
    (-> state
        move-actors
        attract-actors-to-middle))

(defn update-scene [state]
    (-> state
        handle-mouse-click
        update-actors))

;;;
; Actor things
;;;
(defn apply-actors [state func]
    (assoc state :actors
        (for [actor (state :actors)]
            (func state actor))))

(defn attraction-force-scalar [coords1 mass1 coords2 mass2]
    (let [dist-sqr (distance-squared coords1 coords2)]
    (if (< dist-sqr 1) 0
    ; else
    (* 6 (/ (* mass1 mass2) dist-sqr)) )))

(defn attraction-force [coords1 mass1 coords2 mass2]
    (let [scalar (attraction-force-scalar coords1 mass1 coords2 mass2)]
    (map (partial * scalar) (point-at coords1 coords2)) ))

(defn attract-actor-to-middle [state actor]
    (let [midx (/ (state :width) 2)
          midy (/ (state :height) 2)
          coords (actor :coords)
          mass (actor :mass)
          attr (attraction-force coords mass [midx midy] 50)]
    (update-in actor [:velocity]
        (fn [velocity] (move-vector velocity attr)) )))

(defn attract-actors-to-middle [state]
    (apply-actors state attract-actor-to-middle))

(defn move-actor [state actor]
    (update-in actor [:coords]
        (fn [coords] (move-vector coords (actor :velocity))) ))

(defn move-actors [state actor]
    (apply-actors state move-actor))

(defn create-actor [state coords]
    (assoc state
        :actors
        (conj (get state :actors) {
            :coords coords
            :velocity [(rand -5 5) (rand -5 5)]
            :color "#F00"
            :mass (rand 2 20) })))

;;;
; FPS tracking and display
;;;
(defn update-fps [state]
    (update-in state [:frametimes]
        (fn [ft] (subvec (vec (cons (get-timestamp) ft)) 0 (+ 1 (min (count ft) 9)))) ))

(defn display-fps [state]
    (let [
        frametimes (state :frametimes)
        mspf (/ (- (first frametimes) (last frametimes)) (count frametimes))
        fps (js/parseInt (* (/ 1 mspf) 1000))
        fps-str (str "FPS: " fps)
        ctx (state :context)]

        (set! ctx -fillStyle "rgb(255,255,255)")
        (set! ctx -strokeStyle "rgb(0,0,0)")
        (set! ctx -textBaseline "bottom")
        (set! ctx -font (str (/ 2 (state :scale)) + "em Arial"))
        (.fillText ctx fps-str 5 (state :height))
        (.strokeText ctx fps-str 5 (state :height)) ))

;;;
; Just mouse things
;;;
(def mouse-state (atom {:mousex -1 :mousey -1}))

(defn update-mouse-state [x y]
    (swap! mouse-state assoc :mousex x)
    (swap! mouse-state assoc :mousey y))

(defn clear-mouse-state []
    (swap! mouse-state assoc :mousex -1)
    (swap! mouse-state assoc :mousey -1))

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

;;;
; Things to eventually be used for triforces (Recycling Bin)
;;;
(defn vec-len [& coords]
    (sqrt (apply + (map sqr coords))))

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

;;;
; Initial state and game loop start
;;;
(defn create-state [context width height] {
    :context context
    :ticks 10
    :ms_per_tick (/ 1000 10)
    :next_tick (.getTime (js/Date.))
    :frametimes []
    :width width
    :height height
    :scale 1
    :actors []})

(defn context [width height]
    (let [target (.getElementById js/document "canvas")]
        [(.getContext target "2d")
            (set! (. target -width) width)
            (set! (. target -height) height)]))

(defn game-loop [state]
    (let [
        timestamp (get-timestamp)
        interp (- 1 (/ (- (state :next_tick) timestamp) (state :ms_per_tick)))]
        (render-scene state interp)
        (js/requestAnimationFrame (fn []
            (game-loop (update-if-needed (update-fps state) timestamp)) ))))

(defn ^:export init []
    (hook-input-events)
    (game-loop (apply create-state (context 640 480))))
