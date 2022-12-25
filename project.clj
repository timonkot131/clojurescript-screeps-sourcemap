(defproject org.clojars.timonkot13/clojurescript-screeps-sourcemap "0.1.0"
  :description "applies clojure source-maps to stack-trace"
  :url "https://github.com/timonkot131/clojurescript-screeps-sourcemap"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojurescript "1.11.60"]]
  :main ^:skip-aot screeps.sourcemap
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
