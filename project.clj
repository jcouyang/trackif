(defproject trackif "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [enlive "1.1.5"]
                 [environ "1.0.0"]
                 [liberator "0.12.2"]
                 [clj-http "1.1.0"]
                 [compojure "1.1.8"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [org.clojure/tools.logging "0.3.1"]]
  :main ^:skip-aot trackif.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
