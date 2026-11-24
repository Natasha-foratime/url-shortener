(ns url-shortener.client
  (:require [cheshire.core :as json])
  (:import [java.net URI]
           [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers
            HttpResponse$BodyHandlers]))

(def base-url "http://localhost:8080")

(defn send-request
  ([method path]
   (send-request method path nil))
  ([method path data]
   (let [client (HttpClient/newHttpClient)
         builder (HttpRequest/newBuilder
                  (URI/create (str base-url path)))
         builder (.header builder "Accept" "application/json")
         builder (if data
                   (.header builder "Content-Type" "application/json")
                   builder)
         body (if data
                (HttpRequest$BodyPublishers/ofString
                 (json/generate-string data))
                (HttpRequest$BodyPublishers/noBody))
         request (.build (.method builder method body))
         response (.send client request
                         (HttpResponse$BodyHandlers/ofString))]
     {:status (.statusCode response)
      :body (json/parse-string (.body response) true)})))

(defn create-url []
  (println "Enter original URL:")
  (let [url (read-line)
        response (send-request "POST" "/api/urls"
                               {:original_url url})]
    (if (= 201 (:status response))
      (println "Short code:" (get-in response [:body :short_code]))
      (println "Error:" (get-in response [:body :error])))))

(defn get-url []
  (println "Enter short code:")
  (let [code (read-line)
        response (send-request "GET" (str "/api/urls/" code))]
    (if (= 200 (:status response))
      (println "Original URL:"
               (get-in response [:body :original_url]))
      (println "Error:" (get-in response [:body :error])))))

(defn update-url []
  (println "Enter short code:")
  (let [code (read-line)]
    (println "Enter new original URL:")
    (let [url (read-line)
          response (send-request "PUT" (str "/api/urls/" code)
                                 {:original_url url})]
      (if (= 200 (:status response))
        (println "URL updated successfully.")
        (println "Error:" (get-in response [:body :error]))))))

(defn delete-url []
  (println "Enter short code:")
  (let [code (read-line)
        response (send-request "DELETE" (str "/api/urls/" code))]
    (if (= 200 (:status response))
      (println "URL deleted successfully.")
      (println "Error:" (get-in response [:body :error])))))

(defn show-menu []
  (println)
  (println "=================================")
  (println "       URL SHORTENER CLIENT")
  (println "=================================")
  (println "1. Create short URL")
  (println "2. Get original URL")
  (println "3. Update URL")
  (println "4. Delete URL")
  (println "5. Exit")
  (println)
  (print "Choose an option: ")
  (flush))

(defn start-client! []
  (loop []
    (show-menu)
    (let [choice (read-line)]
      (try
        (case choice
          "1" (create-url)
          "2" (get-url)
          "3" (update-url)
          "4" (delete-url)
          "5" (println "Goodbye!")
          (println "Invalid option. Try again."))
        (catch Exception e
          (println "Request failed:" (.getMessage e))))
      (when (not= choice "5")
        (recur)))))