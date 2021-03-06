(ns lambdacd-cron.core
  (:require [lambdacd.steps.support :as support]
            [clj-time.core :as t]
            [clojure.core.async :as async]))

(defn- is-wildcard [target]
  (or (= "*" target)
      (nil? target)))

(defn- nil-to-star [x]
  (if (nil? x) "*" (str x)))

(defn- pattern-to-str [pattern]
  (clojure.string/join " " (map nil-to-star pattern)))

(defn matches
  "Check if a date matches the given cron pattern."
  [date & [minutes hours day-of-month month day-of-week]]
  (let [results [(or (is-wildcard minutes) (= (t/minute date) minutes))
                 (or (is-wildcard hours) (= (t/hour date) hours))
                 (or (is-wildcard day-of-month) (= (t/day date) day-of-month))
                 (or (is-wildcard month) (= (t/month date) month))
                 (or (is-wildcard day-of-week) (= (t/day-of-week date) day-of-week))]]
    (every? identity results)))

(defn- wait-for-cron-while-not-killed [ctx & [minutes hours day-of-month month day-of-week]]
  (let [start-date (t/now)]
    (loop []
      (support/if-not-killed ctx
                             (let [current-date (t/now)]
                               (if (and (>= (t/in-seconds (t/interval start-date current-date)) 60)
                                        (matches current-date minutes hours day-of-month month day-of-week))
                                 {:status :success}
                                 (do
                                   (Thread/sleep (* 5 1000))
                                   (recur))))))))

(defn cron
  "Build step that waits for a default cron pattern to match"
  [& [minutes hours day-of-month month day-of-week]]
  (fn [_ ctx]
    (let [result-ch (:result-channel ctx)
          _ (async/>!! result-ch [:status :waiting])
          _ (async/>!! result-ch [:out (str "Waiting for cron with pattern " (pattern-to-str [minutes hours day-of-month month day-of-week]) " ...")])
          wait-result (wait-for-cron-while-not-killed ctx minutes hours day-of-month month day-of-week)]
      wait-result)))
