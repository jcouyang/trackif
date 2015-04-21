(ns trackif.core-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [trackif.core :refer :all]))


(deftest query-price-test
  (testing "query price not a number"
    (is (= 0 (query-price "https://github.com/jcouyang" ".vcard-fullname"))))
  (testing "query price is 189.00 with css selector"
    (is (= 189.0 (query-price (str "file://" (-> (java.io.File. "test/trackif") .getAbsolutePath) "/fixture.html") ".priceLarge.kitsunePrice")))))

;; (deftest history-price-test
;;   (with-fake-http ["https://api.orchestrate.io/v0/item/http%3A%2F%2Foyanglul.us/refs/?values=true"
;;                    {:value
;;                     [{:price 123 :reftime 123123123}
;;                      {:price 212 :reftime 123123122}]}]

;;   (testing "history of price"
;;     (is (= '(123 212) (history-price "http://oyanglul.us"))))))


;; (deftest notify-test
;;   (testing "notify user if price drop"
;;     (with-redefs [price-drop (fn [url price] true)
;;                   query-price (fn [url selector] 2)]
;;       (is (= '("emailing ouyang2") (notify-when-price-drop {:url "" :selector []}))))))
