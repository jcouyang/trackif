(ns trackif.core
  (:require [net.cgrand.enlive-html :as html]
            [clojure.core.async
             :as a
             :refer [>! >!! <! <!! go chan go-loop timeout]])
  (:gen-class))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn query-price
  "query price from website via selector"
  [url selector]
  ((fnil read-string "0")
   (first (re-find  #"\d+(\.\d+)?"
                    (first (map html/text (html/select (fetch-url url) selector)))))))

(defn history-price
  "fetching hitory price"
  [url]
  [1004 2 3 4 3])

(defn all-urls []
  [{:url "http://www.amazon.cn/dp/1593272812" :selector [:.offer-price]}
   {:url "https://github.com/jcouyang" :selector [:.vcard-stat-count]}
   ])

(defn save-price [url price]
  "saved")

(defn price-drop [url current-price]
  (let [history-price (first (history-price url))]
    (if-not (= current-price history-price)
      (save-price url current-price))
    (< current-price history-price)))

(defn notify
  "notify user about price drop"
  [who what]
  (println (str "emailing" (:name who) what))
  (str "emailing " (:name who) what))

(defn users-of
  "search users via url"
  [url]
  [{:email "oyanglulu@gmail.com"
    :name "ouyang"
    :subscription [:email]
    :urls "amazon.com"}])

(defn notify-when-price-drop [{url :url selector :selector}]
  (let [current-price (query-price url selector)]
    (if (price-drop url current-price)
      (doseq [url (users-of url)]
        (notify url current-price)))))

(defn tracking-prices-at-interval [interval]
  (let [check-ch (chan)]
    (go-loop []
      (<! (timeout interval))
      (doseq [url (all-urls)]
        (>! check-ch url))
      (recur))
    (go-loop []
      (when-let [urls (<! check-ch)]
        (notify-when-price-drop urls))
      (recur))))

(def c (chan))

(defn -main [& args]
  (go-loop []
    (let [url (<! c)]
      (notify-when-price-drop url))
    (recur))
  (loop []
    (doseq [url (all-urls)]
      (println url)
      (>!! c url))
    (<!! (timeout 3000))
    (recur)))
