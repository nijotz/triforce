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
        (vector-add coords velocity) ))

(defn vector-add [& vectors]
    (apply map + vectors))

(defn point-left [coords]
    (let [x (nth coords 0)
          y (nth coords 1)]
    [y, (- x)] ))

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

(defn render-actor [ctx actor interp]
    (let [interp-actor (move-actor actor interp)
          coords (interp-actor :coords)]
        ; Draw circle
        (.beginPath ctx)
        (.arc ctx (nth coords 0) (nth coords 1) (/ (actor :mass) 2) 0 tau false)
        (set! ctx -fillStyle (actor :color))
        (.fill ctx)

        ; Draw heading
        ;(.beginPath ctx)
        ;(.moveTo ctx (nth coords 0) (nth coords 1))
        ;(let [move-coords (move-vector coords (map (partial * 5) (actor :velocity)))]
        ;    (.lineTo ctx (nth move-coords 0) (nth move-coords 1)))
        ;(.stroke ctx)
        ))

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
    (render-middle state)
    (doseq [actor (state :actors)] (render-actor (state :context) actor interp))
    (display-fps state) )

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
        reflect-actors
        attract-actors-to-middle
        attract-actors-together))

(defn update-scene [state]
    (-> state
        handle-mouse-click
        update-actors))

;;;
; Gravity
;;;
(defn attraction-force-scalar [coords1 mass1 coords2 mass2]
    (let [dist-sqr (distance-squared coords1 coords2)
          mass-multp (* mass1 mass2) ]
    ; This is to make things less "flingy". If things are close relative to
    ; their masses, don't attract them together. They'll just get flung off the
    ; canvas
    (if (< dist-sqr mass-multp)
        (* 6 (/ dist-sqr mass-multp))
    ; else
        (* 6 (/ mass-multp dist-sqr)) )))

(defn attraction-force [coords1 mass1 coords2 mass2]
    (let [scalar (attraction-force-scalar coords1 mass1 coords2 mass2)]
    (map (partial * scalar) (point-at coords1 coords2)) ))

(defn attract-actor-to-middle [actor state]
    (let [midx (/ (state :width) 2)
          midy (/ (state :height) 2)
          coords (actor :coords)
          mass (actor :mass)
          attr (attraction-force coords mass [midx midy] 150)]
    (update-in actor [:velocity]
        (fn [velocity] (move-vector velocity attr)) )))

(defn attract-actors-to-middle [state]
    (apply-actors state attract-actor-to-middle [state]))

(defn attraction-force-pair [actor1 actor2]
    (let [result (attraction-force (actor1 :coords) (actor1 :mass) (actor2 :coords) (actor2 :mass))]
        [result (map - result)] ))

(defn get-actor-attract-forces [state]
    (all-pairs (state :actors) attraction-force-pair vector-add))

(defn attract-actors-together [state]
    (let [forces (get-actor-attract-forces state)]
    (update-in state [:actors]
        (fn [actors]
            (map
                (fn [actor f]
                    (update-in actor [:velocity] (fn [v] (vector-add v f))) )
                actors forces )))))

(defn all-pairs [coll pair-fn sum-fn]
    (loop [calc []
           partial-calc (take (count coll) (repeat [0 0]))
           uncalc coll
           pair-fn pair-fn
           sum-fn sum-fn]
        (if (= (count uncalc) 0)
            calc
        ;else
        (let [nxt (first uncalc)
              nxt-partial-calc (first partial-calc)
              other-partial-calc (next partial-calc)
              new-calc-pairs (for [x (next uncalc)] (pair-fn nxt x))
              new-calc-vals (map first new-calc-pairs)
              other-calc-vals (map second new-calc-pairs)]
            (recur
                (conj calc (apply sum-fn (cons (or nxt-partial-calc [0 0]) new-calc-vals)))
                (map sum-fn other-partial-calc (map second new-calc-pairs))
                (next uncalc)
                pair-fn sum-fn )))))

;;;
; Actor things
;;;
(defn apply-actors [state func args]
    (assoc state :actors
        (for [actor (state :actors)]
            (apply func actor args) )))

(defn move-actor
    ([actor]
        (update-in actor [:coords]
            (fn [coords] (move-vector coords (actor :velocity))) ))
    ([actor interp]
        (update-in actor [:coords]
            (fn [coords] (move-vector coords (map (partial * interp) (actor :velocity)))) )))

(defn move-actors [state]
    (apply-actors state move-actor nil))

(defn reflect-actor-edge [actor coord-idx edge]
    (-> actor
        ; Coords is a LazySeq, but needs to be a Vector for update-in [:x 1] to
        ; work
        ((fn [actor] (update-in actor [:velocity] vec)))
        ((fn [actor] (update-in actor [:coords] vec)))

        ((fn [actor] (update-in actor [:velocity coord-idx] #(- %1))))
        ((fn [actor] (update-in actor [:coords coord-idx] #(- edge (- %1 edge)))) )))

; It'd be nice to figure out how to do this without the copied and pasted code
(defn reflect-actor [actor state]
    (let [coords (actor :coords)
          width  (state :width)
          height (state :height)]
    (-> actor
        (#(if (< (nth coords 0) 0)
            (reflect-actor-edge %1 0 0)
            %1))
        (#(if (> (nth coords 0) width)
            (reflect-actor-edge %1 0 width)
            %1))
        (#(if (< (nth coords 1) 0)
            (reflect-actor-edge %1 1 0)
            %1))
        (#(if (> (nth coords 1) height)
            (reflect-actor-edge %1 1 height)
            %1)) )))

(defn reflect-actors [state]
    (apply-actors state reflect-actor [state]))

(defn create-actor [state coords]
    (let [mid-coords [(/ (state :width) 2) (/ (state :height) 2)]
          x (nth coords 0)
          y (nth coords 1)]
    (assoc state
        :actors
        (conj (get state :actors) {
            :coords coords
            :velocity (map
                (partial * (rand 3 10))
                (point-left (point-at coords mid-coords)))
            :color "#F00"
            :mass (rand 2 20) }))))

;;;
; FPS tracking and display
;;;
(defn update-fps [state]
    (update-in state [:frametimes]
        (fn [ft] (subvec (vec (cons (get-timestamp) ft)) 0 (+ 1 (min (count ft) 9)))) ))

(defn display-fps [state]
    (let [frametimes (state :frametimes)
          mspf (/ (- (first frametimes) (last frametimes)) (count frametimes))
          fps (js/parseInt (* (/ 1 mspf) 1000))
          fps-str (str "FPS: " fps)
          ctx (state :context)]
    (set! ctx -fillStyle "rgb(255,255,255)")
    (set! ctx -strokeStyle "rgb(0,0,0)")
    (set! ctx -textBaseline "bottom")
    (set! ctx -font (str (/ 2 (state :scale)) "em Arial"))
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

(defn hook-input-events [state]
    (let [canvas (state :canvas)]
    (.addEventListener canvas "click"
        (fn [e]
            (println "Click")
            (update-mouse-state (. e -clientX) (. e -clientY))
            false ))
    (.addEventListener canvas "touchstart"
        (fn [e]
            (println "Click")
            (update-mouse-state (. e -clientX) (. e -clientY))
            false ))))

;;;
; Initial state and game loop start
;;;
(def ticks 10)
(defn create-state [canvas context width height] {
    :canvas canvas
    :context context
    :ticks ticks
    :ms_per_tick (/ 1000 ticks)
    :next_tick (.getTime (js/Date.))
    :frametimes []
    :width width
    :height height
    :scale 1
    :actors []})

(defn context []
    (let [canvas (.getElementById js/document "experiment")
          width  (. canvas -clientWidth)
          height (. canvas -clientHeight)]
    [canvas
     (.getContext canvas "2d")
     (set! (. canvas -width) width)
     (set! (. canvas -height) height) ]))

(def animation-frame
  (or (.-requestAnimationFrame js/window)
      (.-webkitRequestAnimationFrame js/window)
      (.-mozRequestAnimationFrame js/window)
      (.-oRequestAnimationFrame js/window)
      (.-msRequestAnimationFrame js/window)
      (fn [callback] (js/setTimeout callback 17)) ))

(defn game-loop [state]
    (let [
        timestamp (get-timestamp)
        interp (- 1 (/ (- (state :next_tick) timestamp) (state :ms_per_tick)))]
        (render-scene state interp)
        (animation-frame (fn []
            (game-loop (update-if-needed (update-fps state) timestamp)) ))))

(defn ^:export init []
    (let [state (apply create-state (context))]
    (hook-input-events state)
    (game-loop state) ))
