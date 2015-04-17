(ns trackif.core
  (:require [net.cgrand.enlive-html :as html])
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
  [4 2 3 4 3])

(defn price-drop [url selector]
  (< (query-price url selector) (first (history-price url))))

(defn notify
  "notify user about price drop"
  [who]
  (str "emailing " (:name who)))

(defn users-of
  "search users via url"
  [url]
  [{:email "oyanglulu@gmail.com"
    :name "ouyang"
    :subscription [:email]
    :urls "amazon.com"}])

(defn notify-when-price-drop [url selector]
  (if (price-drop url selector)
    (map notify (users-of url))))
