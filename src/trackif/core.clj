(ns trackif.core
  (:require [net.cgrand.enlive-html :as html]
            [clojure.core.async
             :as a
             :refer [>! >!! <! <!! go chan go-loop timeout]]
            [environ.core :refer [env]]
            [clj-http.client :as http])
  (:gen-class))

(import [java.net URLEncoder])

(def options {:content-type :json
              :headers {:User-Agent "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:37.0) Gecko/20100101 Firefox/37.0"}
              :accept :json
              :as :json
              :basic-auth [(env :ti-api-key) (env :ti-password)]
              })
(defn orch
  ([method key data]
   (let [{:keys [status headers body error] :as resp} (method (str "https://api.orchestrate.io/v0/" key) (merge options {:form-params data}))]
     (if error
       (println "orch request fail with execption" error)
       (:results body))))
  ([method key]
   (let [{:keys [status headers body error] :as resp} (method (str "https://api.orchestrate.io/v0/" key) options)]
     (if error
       (println "orch request fail with execption" error)
       (:results body)))))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn query-price
  "query price from website via selector"
  [url selector]
  ((fnil read-string "0")
   (first (re-find  #"\d+(\.\d+)?"
                    (first (map html/text (html/select (fetch-url url) (map keyword (.split #" " selector)))))))))

(defn history-price
  "fetching hitory price"
  [url]
  (map #(hash-map :price (get-in % [:value :price])
                  :time (:reftime %))
       (orch http/get (str "item/" (URLEncoder/encode url) "/refs/?values=true"))))

(defn all-urls []
  (map #(:value %) (orch http/get "item")))

(defn save-price [url price]
  (println "saving " url " with price " price)
  (orch http/patch (str "item/" (URLEncoder/encode url)) [{:op "replace" :path "price" :value price}]))

(defn users-of
  "search users via url"
  [url]
  (map #(get-in % [:value :email]) (orch http/get (str "item/" (URLEncoder/encode "https://github.com/jcouyang") "/relations/subs"))))

(defn price-drop [url current-price]
  (when-let [{old-price :price} (first (history-price url))]
    (if-not (= current-price old-price)
      (save-price url current-price))
    (< current-price old-price)))

(defn notify
  "notify user about price drop"
  [who what]
  (println (str "emailing" (:name who) what))
  (str "emailing " (:name who) what))


(defn notify-when-price-drop [{url :url selector :selector}]
  (let [current-price (query-price url selector)]
    (println "find price of " url ": " current-price)
    (if (price-drop url current-price)
      (doseq [url (users-of url)]
        (notify url current-price)))))

(def c (chan))

(defn -main [& args]
  (go-loop []
    (let [url (<! c)]
      (try
        (notify-when-price-drop url)
        (catch Exception e (str "exception:" (.getMessage e)))))
    (recur))
  (loop []
    (doseq [url (all-urls)]
      (>!! c url))
    (<!! (timeout 10000))
    (recur)))
