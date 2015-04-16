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

(defn history-price [url]
  [4 2 3 4 3])

(defn price-drop? [url]
  (< (query-price url) (first (history-price url))))
