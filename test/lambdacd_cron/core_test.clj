(ns lambdacd-cron.core-test
  (:use [lambdacd-cron.core]
        [lambdacd-cron.test-util]
        [lambdacd.steps.control-flow])
  (:require [clojure.test :refer :all]
            [clj-time.core :as t])
  (:refer-clojure :exclude [alias]))


(defn- super-now [dates]
  (let [state (atom dates)]
    (fn []
      (let [date (first @state)]
        (if (> (count @state) 1)
          (swap! state rest))
        date))))

(deftest matches-test
  (testing "all wildcards matches everything"
    (is (matches (t/date-time 2016 5 2 12 6 0) "*" "*" "*" "*" "*"))
    (is (matches (t/date-time 2016 5 2 12 6 0) nil nil nil nil nil))
    (is (matches (t/now))))
  (testing "minutes"
    (is (matches (t/date-time 2016 5 2 12 6 15) 6))
    (is (false? (matches (t/date-time 2016 5 2 12 6 15) 7))))
  (testing "hours"
    (is (matches (t/date-time 2016 5 2 12 6 0) nil 12))
    (is (matches (t/date-time 2016 5 2 12 6 0) 6 12))
    (is (false? (matches (t/date-time 2016 5 2 12 6 0) nil 13)))
    (is (false? (matches (t/date-time 2016 5 2 12 6 0) 7 12))))
  (testing "day-of-months"
    (is (matches (t/date-time 2016 5 2 12 6 0) nil nil 2))
    (is (matches (t/date-time 2016 5 2 12 6 0) 6 12 2))
    (is (false? (matches (t/date-time 2016 5 2 12 6 0) nil nil 3)))
    (is (false? (matches (t/date-time 2016 5 2 12 6 0) 7 12 2))))
  (testing "months"
    (is (matches (t/date-time 2016 5 2 12 6 0) nil nil nil 5))
    (is (matches (t/date-time 2016 5 2 12 6 0) 6 12 2 5))
    (is (false? (matches (t/date-time 2016 5 2 12 6 0) nil nil nil 6)))
    (is (false? (matches (t/date-time 2016 5 2 12 6 0) 7 12 2 5))))
  (testing "day-of-weeks"
    (is (matches (t/date-time 2016 5 2 12 6 0) nil nil nil nil 1))
    (is (matches (t/date-time 2016 5 2 12 6 0) 6 12 2 5 1))
    (is (false? (matches (t/date-time 2016 5 2 12 6 0) nil nil nil nil 2)))
    (is (false? (matches (t/date-time 2016 5 2 12 6 0) 7 12 2 5 1)))))

(deftest cron-test
  (testing "that cron returns a step"
    (fn? (cron)))
  (testing "that cron get's killed if the step get's killed"
    (let [ctx (some-ctx)
          waiting-ch (start-waiting-for ((cron 99) {} ctx))]
      (reset! (:is-killed ctx) true)
      (is (= {:status :killed} (get-or-timeout waiting-ch :timeout 1500)))))
  (testing "that cron does not match the very same date it is started to prevent multiple pipelines starting"
    (with-redefs [t/now (super-now [(t/date-time 2016 5 2 12 6 0)])]
      (let [ctx (some-ctx)
            waiting-ch (start-waiting-for ((cron 6) {} ctx))]
        (is (= {:status :timeout} (get-or-timeout waiting-ch :timeout 1500))))))
  (testing "that cron succeeds upon matching pattern"
    (with-redefs [t/now (super-now [(t/date-time 2016 5 2 12 6 0)
                                    (t/date-time 2016 5 2 12 7 0)])]
      (let [ctx (some-ctx)
            waiting-ch (start-waiting-for ((cron 7) {} ctx))]
        (is (= {:status :success} (get-or-timeout waiting-ch :timeout 5500)))))))