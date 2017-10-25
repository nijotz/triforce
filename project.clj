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

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/tools.trace "0.7.8"]
                 [com.aphyr/prism "0.1.1"]
                 [org.clojure/math.numeric-tower "0.0.4"]]

  :plugins [
      [lein-cljsbuild "1.1.7"]
      [lein-figwheel "0.5.13"]]

  :hooks [leiningen.cljsbuild]

  :cljsbuild {
    :builds [{:id "triforces"
              :source-paths ["src/cljs"]
              :figwheel true
              :compiler {
                :main "triforces.core"
                :source-map true
                :asset-path "js"
                :output-to "resources/public/js/triforces.js"
                :output-dir "resources/public/js/"
                :optimizations :none
                :pretty-print true }}]})

  :figwheel {
     :http-server-root "public" ;; this will be in resources/
     :server-port 3449          ;; default is 3449
     :server-ip   "0.0.0.0"     ;; default is "localhost"

     ;; CSS reloading (optional)
     ;; :css-dirs has no default value
     ;; if :css-dirs is set figwheel will detect css file changes and
     ;; send them to the browser
     :css-dirs ["resources/public/css"]

     ;; Server Ring Handler (optional)
     ;; if you want to embed a ring handler into the figwheel http-kit
     ;; server
     ; :ring-handler example.server/handler

     ;; Clojure Macro reloading
     ;; disable clj file reloading
     ; :reload-clj-files false
     ;; or specify which suffixes will cause the reloading
     ; :reload-clj-files {:clj true :cljc false}

     ;; To be able to open files in your editor from the heads up display
     ;; you will need to put a script on your path.
     ;; that script will have to take a file path, a line number and a column
     ;; ie. in  ~/bin/myfile-opener
     ;; #! /bin/sh
     ;; emacsclient -n +$2:$3 $1
     ;;
     ; :open-file-command "myfile-opener"

     ;; if you want to disable the REPL
     ;; :repl false

     ;; to configure a different figwheel logfile path
     ;; :server-logfile "tmp/logs/figwheel-logfile.log"

     ;; Start an nREPL server into the running figwheel process
     ;; :nrepl-port 7888

     ;; Load CIDER, refactor-nrepl and piggieback middleware
     ;;  :nrepl-middleware ["cider.nrepl/cider-middleware"
     ;;                     "refactor-nrepl.middleware/wrap-refactor"
     ;;                     "cemerick.piggieback/wrap-cljs-repl"]

     ;; if you need to watch files with polling instead of FS events
     ;; :hawk-options {:watcher :polling}
     ;; ^ this can be useful in Docker environments

     ;; if your project.clj contains conflicting builds,
     ;; you can choose to only load the builds specified
     ;; on the command line
     ;; :load-all-builds false ; default is true
  }
