(ns url-shortener.core
  (:gen-class)
  (:require [url-shortener.client :as client]
            [url-shortener.server :as server]))

(defn parse-port [value]
  (try
    (let [port (Integer/parseInt value)]
      (when (<= 1 port 65535)
        port))
    (catch NumberFormatException _
      nil)))

(defn -main [& args]
  (if (empty? args)
    (client/start-client!)
    (if (and (= 1 (count args))
             (parse-port (first args)))
      (server/start-server! (parse-port (first args)))
      (do
        (println "Usage:")
        (println "  java -jar url-shortener.jar")
        (println "  java -jar url-shortener.jar 8080")
        (System/exit 1)))))