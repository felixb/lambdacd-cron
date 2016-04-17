(ns lambdacd-cron.test-util
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async]
            [lambdacd.event-bus :as event-bus]
            [lambdacd.util :as utils]
            [lambdacd.internal.default-pipeline-state :as default-pipeline-state])
  (:refer-clojure :exclude [alias]))

(defn- some-ctx-template []
  (let [config {:home-dir                (utils/create-temp-dir)
                :ms-to-wait-for-shutdown 10000}]
    (-> {:initial-pipeline-state   {}                       ;; only used to assemble pipeline-state, not in real life
         :step-id                  [42]
         :result-channel           (async/chan (async/dropping-buffer 100))
         :pipeline-state-component nil                      ;; set later
         :config                   config
         :is-killed                (atom false)
         :_out-acc                 (atom "")
         :started-steps            (atom #{})}
        (event-bus/initialize-event-bus))
    ))


(defn- add-pipeline-state-component [template]
  (if (nil? (:pipeline-state-component template))
    (assoc template :pipeline-state-component
                    (default-pipeline-state/new-default-pipeline-state template :initial-state-for-testing (:initial-pipeline-state template)))
    template))

(defn some-ctx []
  (add-pipeline-state-component
    (some-ctx-template)))

(defmacro start-waiting-for [body]
  `(async/go
     ~body))

(defn get-or-timeout [c & {:keys [timeout]
                           :or   {timeout 10000}}]
  (async/alt!!
    c ([result] result)
    (async/timeout timeout) {:status :timeout}))
