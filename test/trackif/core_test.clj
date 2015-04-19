(ns trackif.core-test
  (:require [clojure.test :refer :all]
            [trackif.core :refer :all]))

(deftest query-price-test
  (testing "query price not a number"
    (is (= 0 (query-price "https://github.com/jcouyang" [:.vcard-fullname]))))
  (testing "query price is 189.00"
    (is (= 189.0 (query-price (str "file://" (-> (java.io.File. "test/trackif") .getAbsolutePath) "/fixture.html") [:.priceLarge.kitsunePrice])))))

(deftest price-drop-test
  (testing "price drop"
      (is (price-drop "" 0))))


(deftest notify-test
  (testing "notify user if price drop"
    (with-redefs [price-drop (fn [url price] true)
                  query-price (fn [url selector] 2)]
      (is (= '("emailing ouyang2") (notify-when-price-drop {:url "" :selector []}))))))
