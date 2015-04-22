(ns trackif.core-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [trackif.core :refer :all]
            [net.cgrand.enlive-html :as html]))

(deftest query-price-test
    (is (< 36 (query-price "http://github.com/jcouyang" ".vcard-stat-count"))))

(deftest history-price-test
  (with-redefs
    [orch (fn [method key]
            [{:value {:price 123} :reftime 123123123}
             {:value {:price 212} :reftime 123123122}])]
    (is (= '({:time 123123123, :price 123} {:time 123123122, :price 212})
           (history-price "http://oyanglul.us")))))
