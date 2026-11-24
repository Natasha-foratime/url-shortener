(ns url-shortener.validation
  (:require [clojure.string :as str]))

(defn valid-url? [value]
  (boolean
   (and (string? value)
        (not (str/blank? value))
        (try
          (let [uri (java.net.URI. value)
                scheme (.getScheme uri)
                host (.getHost uri)]
            (and (#{"http" "https"} scheme)
                 (some? host)
                 (not (str/blank? host))
                 (nil? (.getUserInfo uri))))
          (catch java.net.URISyntaxException _
            false)))))

(defn valid-code? [value]
  (and (string? value)
       (boolean
        (re-matches #"[A-Za-z0-9]{4,12}" value))))