(ns trackif.core-test
  (:require [clojure.test :refer :all]
            [trackif.core :refer :all]))

(deftest query-price-test
  (testing "query price not a number"
    (is (= 0 (query-price "https://github.com/jcouyang" [:.vcard-fullname]))))
  (testing "query price is 189.00"
    (is (= 189.0 (query-price "file:///Users/twer/Development/trackif/test/trackif/fixture.html" [:.priceLarge.kitsunePrice])))))

(deftest price-drop-test
  (testing "price drop"
    (with-redefs [query-price (fn [url selector] 3)]
      (is (price-drop "" [])))))


(deftest notify-test
  (testing "notify user if price drop"
    (with-redefs [price-drop (fn [url selector] true)]
      (is (= '("emailing ouyang") (notify-when-price-drop "" []))))))
