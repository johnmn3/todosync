(ns todosync.regs.db
  (:require
   [re-frame.core :as rf :refer [path after]]
   [cljs-thread.core :as thread :refer [in]]
   [cljs-thread.env :as env :refer [in-screen? in-core?]]
   [cljs-thread.re-frame :refer [reg-sub dispatch]]
   [cljs-thread.db :as db :refer [db-get db-set!]]
   [editscript.core :as e]
   [todosync.specs :as dts]
   [clojure.edn :as edn]))

(defn get-csrf-token []
    (let [cookies (str (if (and (exists? js/document) (thread/in? :screen))
                         (aget js/document "cookie")
                         @(thread/in :screen (aget js/document "cookie"))))
          token (second (re-find (js/RegExp. "XSRF-TOKEN=(.*)") cookies))]
      token))

(def csrf-token (get-csrf-token))

(def patch-key "patch-store")

(declare app-state->local-store)

(def hash-check-limiter (atom nil))

(defn post-app-state [data]
  (let [token (get-csrf-token)]
    (if-not token
      (js/setTimeout #(post-app-state data) 1000)
      (-> (js/fetch "/sync/app-state"
                    #js {:method "POST"
                         :headers #js {:Content-Type "application/json"
                                       :XSRF-Token token}
                         :body (js/JSON.stringify (clj->js data))})
          (.then #(.json %))
          (.then #(let [client-cmd (aget % "client-cmd")]
                    (reset! hash-check-limiter nil)
                    (when (= client-cmd "sync-back")
                      (let [all-patches (edn/read-string (aget % "sync-back"))
                            last-index (aget % "last-index")
                            catch-up (aget % "catch-up")
                            server-app-state (->> all-patches
                                                  sort
                                                  (map second)
                                                  (map :patches)
                                                  (map e/edits->script)
                                                  (reduce e/patch {}))]
                        (reset! hash-check-limiter nil)
                        (if-not catch-up
                          (do (rf/dispatch [:reset-db all-patches server-app-state])
                              (in :screen (rf/dispatch [:set-db server-app-state])))
                          (rf/dispatch [:catch-up-db (edn/read-string catch-up)]))))
                    (when-let [since (aget % "sync-since")]
                      (let [current-patches (db-get patch-key)
                            new-patches (->> current-patches
                                             (filter (fn [[k _v]] (<= since k)))
                                             (into (sorted-map)))]
                        (post-app-state {:server-cmd :print
                                         :patches (pr-str new-patches)})))))
          (.catch #(reset! hash-check-limiter nil))))))

(def default-db
  {:showing :all
   :user-id ""
   :email ""
   :logged-in? false
   :todos (sorted-map)
   :dark-theme? false
   :drawer-open? false})

(def ls-key "local-store")

(when (env/in-core?)
  (js/setInterval
   #(when-not @hash-check-limiter
      (let [current-patches (db-get patch-key)
            index (-> current-patches keys sort last)
            last-patch (get current-patches index)
            last-patch-index (:patch-index last-patch)
            app-state-hash (:hash last-patch)]
        #_
        (println :sending-sync-check
                 :app-state-index index
                 :last-patch-index (:patch-index last-patch))
        (reset! hash-check-limiter app-state-hash)
        (post-app-state {:check-app-state-hash app-state-hash
                         :patch-index last-patch-index})))
   500))

(def tmp-db-atom (atom nil))

(defn app-state->local-store
  "Puts app-state into local forage"
  [app-state]
  (reset! tmp-db-atom app-state)
  (when (and (env/in-core?) app-state)
    (let [current-patches (db-get patch-key)
          old-app-state (or (db-get ls-key) {})
          new-app-state-hash (str (hash app-state))
          old-app-state-hash (str (hash old-app-state))]
      (db-set! ls-key app-state)
      (if current-patches
        (let [new-patch-index (-> current-patches keys sort last (or -1) inc)
              new-patches (e/diff old-app-state app-state)
              patch {:patches new-patches
                     :patch-index new-patch-index
                     :hash new-app-state-hash}]
          (when-not (= "[]" (pr-str new-patches))
            (in :screen (rf/dispatch [:set-db app-state]))
            #_
            (println :posting-new-patches :index new-patch-index :patches new-patches)
            (post-app-state {:patches (pr-str {new-patch-index patch})
                             :patch-index new-patch-index
                             :new-app-state-hash new-app-state-hash
                             :old-app-state-hash old-app-state-hash})
            (db-set! patch-key (assoc current-patches new-patch-index patch))))
        (let [initial-diffs {0 (e/diff {} app-state)}
              initial-patches {0 {:patches initial-diffs
                                  :patch-index 0
                                  :hash (str (hash app-state))}}]
          (post-app-state {:patches (pr-str initial-diffs)
                           :patch-index 0
                           :new-app-state-hash new-app-state-hash
                           :old-app-state-hash old-app-state-hash})
          (db-set! patch-key initial-patches))))
    app-state))

(def ->local-store (rf/after app-state->local-store))

(def app-state-interceptors
  [->local-store
   rf/trim-v])

(def check-spec-interceptor (after (partial dts/check-and-throw ::dts/db)))

(def todo-interceptors [check-spec-interceptor
                        ->local-store
                        (path :todos)])

(when (or (in-screen?) (in-core?))
  (rf/reg-cofx
   :local-store
   (fn [cofx second-param]
     (if (in-screen?)
       cofx
       (let [app-state (db-get ls-key)]
         (if-not app-state
           cofx
           (-> cofx
               (assoc :app-state (update app-state :todos #(into (sorted-map) %))))))))))

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
   (let [new-db (merge default-db db server-app-state)]
     (reset! hash-check-limiter nil)
     (reset! tmp-db-atom app-state)
     (db-set! patch-key all-patches)
     (db-set! ls-key new-db)
     {:db new-db})))

(rf/reg-event-fx
 :catch-up-db
 [(rf/inject-cofx :local-store)
  check-spec-interceptor]
 (fn [{:as cofx :keys [db app-state]} [_ catch-up-patches]]
   (let [current-patches (db-get patch-key)
         new-patches (merge current-patches catch-up-patches)
         new-app-state (->> new-patches
                            sort
                            (map second)
                            (map :patches)
                            (map e/edits->script)
                            (reduce e/patch {}))
         new-db (merge default-db db new-app-state)]
     #_
     (println :catch-up-db :in (:id env/data))
     (reset! tmp-db-atom new-db)
     (db-set! patch-key new-patches)
     (db-set! ls-key new-db)
     (in :screen (rf/dispatch [:set-db new-db]))
     {:db new-db})))


(rf/reg-event-fx
 :initialize-db
 [(rf/inject-cofx :local-store)
  check-spec-interceptor]
 (fn [{:as cofx :keys [db app-state]} second-param]
   (let [new-db (merge default-db db app-state)]
     (app-state->local-store new-db)
     {:db new-db})))

(rf/reg-event-db
 :toggle-dark-theme
 app-state-interceptors
 (fn [app-state [stuff s2]]
   (update app-state :dark-theme? not)))

(rf/reg-event-fx
 :set-db
 (fn [{:keys [db]} [_ new-app-state]]
   (if-not new-app-state
     {:db db}
     (let [res-db (merge db new-app-state)]
       (reset! tmp-db-atom new-app-state)
       {:db res-db}))))
