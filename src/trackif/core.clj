(ns trackif.core
  (:require [net.cgrand.enlive-html :as html]
            [clojure.core.async
             :as a
             :refer [>! >!! <! <!! go chan go-loop timeout]]
            [environ.core :refer [env]]
            [sendgrid-clj.core :refer [send-email]]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [api]]
            [compojure.route :as route]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [clj-http.client :as http])
  (:use [clojure.tools.logging :only (info error)]
        [liberator.core :only [defresource]])
  (:import org.mindrot.jbcrypt.BCrypt)
  (:gen-class))

(import [java.net URLEncoder])

(def options {:content-type :json
              :accept :json
              :as :json
              :basic-auth [(env :ti-api-key) (env :ti-password)]
              })
(def sendgrid-auth {
                    :api_key (env :sendgrid-key)
                    :api_user (env :sendgrid-user)})

(def orch-api "https://api.orchestrate.io/v0/")

(defn orch
  ([method key data]
   (let [{:keys [status headers body err] :as resp} (method (str orch-api key) (merge options {:form-params data}))]
     (if err
       (error "orch request fail with execption" err)
       body)))
  ([method key]
   (let [{:keys [status headers body err] :as resp} (method (str orch-api key) options)]
     (if err
       (error "orch request fail with execption" err)
       body))))

(defn fetch-url [url]
  (html/html-resource (:body (http/get url {:as :stream
                                            :client-params {"http.useragent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:37.0) Gecko/20100101 Firefox/37.0"}}))))

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
       (:results (orch http/get (str "item/" (URLEncoder/encode url) "/refs/?values=true")))))

(defn all-urls []
  (map #(:value %) (:results (orch http/get "item"))))

(defn save-price [url price]
  (info "saving " url " with price " price)
  (orch http/patch (str "item/" (URLEncoder/encode url)) [{:op "replace" :path "price" :value price}]))

(defn users-of
  "search users via url"
  [url]
  (map #(get-in % [:value :email])
       (:results (orch http/get (str "item/" (URLEncoder/encode url) "/relations/subsby")))))

(defn subs-of [user]
  (map #(get % :value)
       (:results (orch http/get (str "user/" (URLEncoder/encode user) "/relations/subs")))))

(defn price-drop [url current-price]
  (when-let [{old-price :price} (first (history-price url))]
    (if-not (= current-price old-price)
      (save-price url current-price))
    (< current-price old-price)))

(defn notify
  "notify user about price drop"
  [who what]
  (info (str "emailing" (:name who) what))
  (send-email sendgrid-auth
              {
               :text what
               :to who
               :from "jichao@oyanglul.us"
               :subject what
               }))


(defn notify-when-price-drop [{url :url selector :selector}]
  (let [current-price (query-price url selector)]
    (info "find price of " url ": " current-price)
    (if (price-drop url current-price)
      (doseq [url (users-of url)]
        (notify url current-price)))))

(def c (chan))

(defn authenticated? [ctx]
  (when-let [user (orch http/get (str "user/" (get-in ctx [:request :params :user])))]
    (BCrypt/checkpw (get-in ctx [:request :headers "authorization"]) (:token user))))

(def auth-res
  {:available-media-types ["application/json"]
   :handle-not-found (fn [_] "Ops. May the lambda be with you!")
   :authorized? authenticated?})

(defresource hello-resource
  :allowed-methods [:get]
  :handle-ok (fn [_] "May the lambda be with you!"))

(defresource subscribe-res auth-res
  :allowed-methods [:get]
  :exists? (fn [ctx]
             (if-let [urls (subs-of (get-in ctx [:request :params :user]))]
               {:urls urls}))
  :handle-ok (fn [ctx]
               (get ctx :urls)))

(defresource item-res [url] auth-res
  :allowed-methods [:get :post :put]
  :exists? (fn [ctx]
             (if-let [item (orch http/get (str "item/" url))]
               (:item item)))
  :handle-ok (fn [ctx] (get ctx :item))
  :put! (fn [ctx] (orch http/put (str "item/" url) (get-in ctx [:request :body]))))

(defroutes app
  (GET "/" hello-resource)
  (GET "/subscribe" subscribe-res)
  (GET "/item/:url" item-res))

(defn -main [& args]
  (go-loop []
    (let [url (<! c)]
      (try
        (notify-when-price-drop url)
        (catch Exception e ( error e "exception"))))
    (recur))
  (go-loop []
    (doseq [url (all-urls)]
      (>! c url))
    (<! (timeout (or (env :period) 10000)))
    (recur))
  (let [port (Integer. (or (env :port) 5000))]
    (jetty/run-jetty (-> app api) {:port port :join? false})))
