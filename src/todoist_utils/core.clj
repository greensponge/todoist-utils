(ns todoist-utils.core
  (:gen-class)
  (:require
   [clj-http.client :as client]
   [clojure.data.json :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io])
  (:import [java.time LocalDate YearMonth]))

(defn load-config
  "You MUST have a config.edn added to project root with the required key/values for this program to work."
  []
  (let [config-file (io/file "config.edn")]
    (if config-file
      (-> config-file
          slurp
          edn/read-string)
      (throw (ex-info "Config file not found!" {:file "resources/config.edn"})))))

(def config (load-config))

(def client-params {:accept       :json
                    :content-type :json
                    :headers      {:authorization (str "Bearer " (get config :api-token))}})

(def todoist-rest-url "https://api.todoist.com/rest/v2/")
(def todoist-sync-url "https://api.todoist.com/sync/v9/sync/")

(defn start-of-week [^LocalDate date]
  ;; Move back to Monday
  (.minusDays date (dec (.getValue (.getDayOfWeek date)))))

(defn classify-date [due-date-str]
  (if (not-empty due-date-str)
    (let [due-date (LocalDate/parse due-date-str)
          today (LocalDate/now)
          ;; Week calculations
          start-this-week (start-of-week today)
          end-this-week (.plusDays start-this-week 6)
          start-next-week (.plusWeeks start-this-week 1)
          end-next-week (.plusDays start-next-week 6)
          ;; Month calculations
          start-this-month (.withDayOfMonth today 1)
          end-this-month (.atEndOfMonth (YearMonth/from today))
          start-next-month (.plusMonths start-this-month 1)
          end-next-month (.atEndOfMonth (YearMonth/from start-next-month))]

      (cond
        ;; Past or this week
        (or (.isBefore due-date today)
            (and (not (.isBefore due-date start-this-week))
                 (not (.isAfter due-date end-this-week))))
        :this-week

        (and (not (.isBefore due-date start-next-week))
             (not (.isAfter due-date end-next-week)))
        :next-week

        (and (.isAfter due-date end-next-week)
             (not (.isAfter due-date end-this-month)))
        :this-month

        (and (not (.isBefore due-date start-next-month))
             (not (.isAfter due-date end-next-month)))
        :next-month

        :else :later))
    :later))

(defn get-tasks [project-id]
  (-> (client/get (str todoist-rest-url "tasks?project_id=" project-id) client-params)
      :body
      (json/read-str :key-fn keyword)))

(defn move-item [task-id to-project]
  (let [payload {:commands [{:type "item_move"
                             :uuid (str (random-uuid))
                             :args {:id         task-id
                                    :project_id to-project}}]}]
    (json/read-str
     (:body (client/post todoist-sync-url
                         (merge client-params
                                {:body (json/write-str payload)})))
     :key-fn keyword)))

(defn move-task-according-to-date [project-id]
  (let [{:keys [inbox-project-id
                this-week-project-id
                next-week-project-id
                this-month-project-id
                next-month-project-id
                later-project-id]} config
        tasks (get-tasks project-id)]
    (when (seq tasks)
      (doseq [task tasks]
        (let [due-date (-> task :due :date)
              target-location (classify-date due-date)
              task-id (:id task)]
          (condp = target-location
            :this-week (when-not (= project-id this-week-project-id)
                         (move-item task-id this-week-project-id))

            :next-week (when-not (= project-id next-week-project-id)
                         (move-item task-id next-week-project-id))

            :this-month (when-not (= project-id this-month-project-id)
                          (move-item task-id this-month-project-id))

            :next-month (when-not (= project-id next-month-project-id)
                          (move-item task-id next-month-project-id))

            :later (when-not (= project-id later-project-id)
                     (move-item task-id later-project-id))

            :else (move-item task-id inbox-project-id)))))))

(defn -main [& _]
  (try
    (let [{:keys [inbox-project-id
                  this-week-project-id
                  next-week-project-id
                  this-month-project-id
                  next-month-project-id
                  later-project-id]} config]
      (move-task-according-to-date inbox-project-id)
      (move-task-according-to-date this-week-project-id)
      (move-task-according-to-date next-week-project-id)
      (move-task-according-to-date this-month-project-id)
      (move-task-according-to-date next-month-project-id)
      (move-task-according-to-date later-project-id))
    (println "Done.")
    (catch Exception e
      (println "Error:" e))))
