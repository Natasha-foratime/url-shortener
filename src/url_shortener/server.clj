(ns url-shortener.server
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [ring.adapter.jetty :as jetty]
            [url-shortener.db :as db]
            [url-shortener.service :as service]))

(defn json-response [status data]
  {:status status
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/generate-string data)})

(defn parse-json-body [request]
  (try
    (json/parse-string
     (slurp (:body request))
     true)
    (catch Exception _
      nil)))

(defn handle-create [database request]
  (let [body (parse-json-body request)
        result (service/create-short-url!
                database (:original_url body))]
    (case (:status result)
      :created
      (json-response 201
                     {:short_code (:short-code result)})

      :invalid-url
      (json-response 400 {:error "Invalid URL"})

      :generation-failed
      (json-response 503 {:error "Could not generate code"})

      (json-response 500 {:error "Internal server error"}))))

(defn handle-get [database code]
  (let [result (service/get-original-url database code)]
    (case (:status result)
      :ok
      (json-response 200
                     {:original_url (:original-url result)})

      :not-found
      (json-response 404 {:error "URL not found"})

      (json-response 500 {:error "Internal server error"}))))

(defn handle-update [database code request]
  (let [body (parse-json-body request)
        result (service/update-short-url!
                database code (:original_url body))]
    (case (:status result)
      :ok (json-response 200 {:message "OK"})
      :invalid-url (json-response 400 {:error "Invalid URL"})
      :not-found (json-response 404 {:error "URL not found"})
      (json-response 500 {:error "Internal server error"}))))

(defn handle-delete [database code]
  (let [result (service/delete-short-url! database code)]
    (case (:status result)
      :ok (json-response 200 {:message "OK"})
      :not-found (json-response 404 {:error "URL not found"})
      (json-response 500 {:error "Internal server error"}))))

(defn make-handler [database]
  (fn [request]
    (let [method (:request-method request)
          uri (:uri request)
          match (re-matches #"/api/urls/([^/]+)" uri)]
      (cond
        (and (= method :post) (= uri "/api/urls"))
        (handle-create database request)

        (and (= method :get) match)
        (handle-get database (second match))

        (and (= method :put) match)
        (handle-update database (second match) request)

        (and (= method :delete) match)
        (handle-delete database (second match))

        :else
        (json-response 404 {:error "Endpoint not found"})))))

(defn start-server! [port]
  (db/init-db!)
  (println (str "URL Shortener server started on port " port))
  (jetty/run-jetty
   (make-handler db/db-spec)
   {:port port
    :join? true}))