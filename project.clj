(defproject url-shortener "0.1.0-SNAPSHOT"
  :description "URL Shortener REST API and CLI client"
  :license {:name "EPL-2.0"}
  :dependencies [[org.clojure/clojure "1.12.2"]
                 [ring/ring-core "1.13.0"]
                 [ring/ring-jetty-adapter "1.13.0"]
                 [cheshire "5.13.0"]
                 [com.github.seancorfield/next.jdbc "1.3.955"]
                 [org.xerial/sqlite-jdbc "3.46.1.3"]]
  :main ^:skip-aot url-shortener.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})