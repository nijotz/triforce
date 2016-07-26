(defproject triforces "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}

  :min-lein-version "2.3.4"

  ;; We need to add src/cljs too, because cljsbuild does not add its
  ;; source-paths to the project source-paths
  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/tools.trace "0.7.8"]
                 [com.aphyr/prism "0.1.1"]
                 [org.clojure/math.numeric-tower "0.0.4"]]

  :plugins [
      [lein-cljsbuild "1.1.0"]
      [lein-figwheel "0.5.4-7"]]


  :cljsbuild {
    :builds [{ :id "triforces"
                :source-paths ["src/cljs"]
                :figwheel true
                :compiler {
                  :main "triforces.core"
                  :asset-path "js/out"
                  :output-to "resources/public/js/triforces.js"
                  :pretty-print false
                  :source-map true }}]})
