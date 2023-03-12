(ns todosync.regs.db
  (:require
   [re-frame.core :as rf :refer [path after]]
   [cljs-thread.core :as thread]
   [cljs-thread.env :as env :refer [in-core?]]
   [cljs-thread.re-frame :refer [reg-sub dispatch]]
   [cljs-thread.db :as db :refer [db-get db-set!]]
   [converge.api :as convergent]
   [editscript.core :as e]
   [todosync.specs :as dts]
   [clojure.edn :as edn]))

(defn get-csrf-token []
    (let [cookies (str (if (and (exists? js/document) (thread/in? :screen))
                         (aget js/document "cookie")
                         @(thread/in :screen (aget js/document "cookie"))))]
      (second (re-find (js/RegExp. "XSRF-TOKEN=(.*)") cookies))))

(def csrf-token (get-csrf-token))

(def patch-key "patch-store")

(declare app-state->local-store)

(defn post-app-state [data]
  (-> (js/fetch "/sync/app-state"
                #js {:method "POST"
                     :headers #js {:Content-Type "application/json"
                                   :XSRF-Token (get-csrf-token)}
                     :body (js/JSON.stringify (clj->js data))})
      (.then #(.json %))
      (.then #(let [client-cmd (aget % "client-cmd")] ;(println :result %)
                (when (= client-cmd "sync-back")
                  (let [all-patches (edn/read-string (aget % "sync-back"))
                        server-app-state (->> all-patches
                                              sort
                                              (map second)
                                              (map e/edits->script)
                                              (reduce e/patch {}))]
                    (dispatch [:reset-db all-patches server-app-state])))
                (when-let [since (aget % "sync-since")]
                  (let [current-patches (db-get patch-key)
                        new-patches (->> current-patches
                                         (filter (fn [[k _v]] (<= since k)))
                                         (into (sorted-map)))]
                    (post-app-state {:server-cmd :print
                                     :patches (pr-str new-patches)})))))))

(def default-db
  {:showing :all
   :user-id ""
   :email ""
   :logged-in? false
   :todos (sorted-map)
   :dark-theme? false
   :drawer-open? true})

(def ls-key "local-store")

(def patch-atom (convergent/ref {}))

(when (env/in-core?)
  (println :in-core :launching :check-interval)
  (let [app-state (or (db-get ls-key) {})
        app-state-hash (hash app-state)]
    (post-app-state {:check-app-state-hash app-state-hash}))
  (js/setInterval
   #(let [app-state (or (db-get ls-key) {})
          app-state-hash (hash app-state)]
      (post-app-state {:check-app-state-hash app-state-hash}))
   500))

(defn app-state->local-store
  "Puts app-state into local forage"
  [app-state]
  (let [pc (reset! patch-atom app-state)
        patches (convergent/peek-patches patch-atom)]
    (when app-state
      (let [current-patches (db-get patch-key)
            old-app-state (or (db-get ls-key) {})]
        (if current-patches
          (let [new-patch-index (-> current-patches keys sort last (or -1) inc)
                new-patches (e/diff old-app-state app-state)]
            (when-not (= "[]" (pr-str new-patches))
              (post-app-state {:patches (pr-str {new-patch-index new-patches})
                               :new-app-state-hash (str (hash app-state))
                               :old-app-state-hash
                               (let [oash (str (hash old-app-state))]
                                 (println :oash oash)
                                 oash)})
              (db-set! patch-key (assoc current-patches new-patch-index new-patches))))
          (let [initial-diffs {0 (e/diff {} app-state)}]
            (post-app-state {:patches (pr-str initial-diffs)
                             :new-app-state-hash (str (hash app-state))
                             :old-app-state-hash (str (hash old-app-state))})
            (db-set! patch-key initial-diffs))))
      (db-set! ls-key app-state))
    (convergent/pop-patches! patch-atom)
    app-state))

(def ->local-store (rf/after app-state->local-store))

(def app-state-interceptors
  [->local-store
   rf/trim-v])

(def check-spec-interceptor (after (partial dts/check-and-throw ::dts/db)))

(def todo-interceptors [check-spec-interceptor
                        ->local-store
                        (path :todos)])

(when (in-core?)
  (rf/reg-cofx
   :local-store
   (fn [cofx second-param]
     (let [local-store (or (db-get ls-key) {})]
       (-> cofx
           (assoc :app-state (update local-store :todos #(into (sorted-map) %))))))))

(reg-sub
 :db
 (fn [db]
   db))

(reg-sub
 :dark-theme?
 (fn [db]
   (:dark-theme? db)))

(reg-sub
 :errors
 (fn [db]
   (:errors db)))

;;; events

(rf/reg-event-fx
 :reset-db
 [(rf/inject-cofx :local-store)
  check-spec-interceptor]
 (fn [{:as cofx :keys [db app-state]} [_ all-patches server-app-state]]
   (let [new-db (merge default-db (or server-app-state {}))]
     (db-set! patch-key all-patches)
     (app-state->local-store new-db)
     {:db new-db})))


(rf/reg-event-fx
 :initialize-db
 [(rf/inject-cofx :local-store)
  check-spec-interceptor]
 (fn [{:as cofx :keys [db app-state]} second-param]
   (let [new-db (merge default-db (or app-state {}))]
     (app-state->local-store new-db)
     {:db new-db})))

(rf/reg-event-db
 :toggle-dark-theme
 app-state-interceptors
 (fn [app-state [stuff s2]]
   (update app-state :dark-theme? not)))
