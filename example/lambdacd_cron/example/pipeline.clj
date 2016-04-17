(ns lambdacd-cron.example.pipeline
  (:use [compojure.core])
  (:require [lambdacd.steps.shell :as shell]
            [lambdacd.steps.manualtrigger :refer [wait-for-manual-trigger]]
            [lambdacd.steps.control-flow :refer [either with-workspace in-parallel run]]
            [lambdacd.core :as lambdacd]
            [ring.server.standalone :as ring-server]
            [lambdacd.ui.ui-server :as ui]
            [lambdacd-cron.core :as lambdacd-cron]
            [lambdacd.runners :as runners]
            [lambdacd.util :as utils]))

(defn print-date [args ctx]
  (shell/bash ctx (:cwd args) "date"))

(def pipeline-structure
  `((either
      wait-for-manual-trigger
      (lambdacd-cron/cron 0 12)) ; trigger build every day at 12:00 UTC
     print-date))

(defn -main [& args]
  (let [home-dir (utils/create-temp-dir)
        config {:home-dir home-dir}
        pipeline (lambdacd/assemble-pipeline pipeline-structure config)]
    (runners/start-one-run-after-another pipeline)
    (ring-server/serve (routes
                         (ui/ui-for pipeline))
                       {:open-browser? false
                        :port          8082})))