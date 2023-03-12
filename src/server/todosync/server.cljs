(ns todosync.server
  (:require
   ["fs" :as fs]
   [dispacio.alpha.core :refer [defp]]
   [applied-science.js-interop :as j]
   [editscript.core :as e]
   [clojure.edn :as edn]
   [perc.core]
   [promesa.core :as p]
   [todosync.specs :as s]
   [sitefox.db :as db]
   [sitefox.auth :as auth]
   [sitefox.web :as web]
   [clojure.string :as str]))

(defonce server (atom nil))

;; (def template (fs/readFileSync "public/index.html"))
(def sitefox-template (fs/readFileSync "public/main.html"))

(def default-db
  {:showing :all
   :user-id ""
   :email ""
   :logged-in? false
   :todos (sorted-map)
   :dark-theme? false
   :drawer-open? true})

(def session-mem-db
  (atom {}))

(defp db-effect identity
  [ctx]
  #_(println :db-effect :default :ctx ctx))

(defp server-cmd identity
  [ctx]
  (println :server-cmd :default :ctx ctx))

(defp db-effect #%(= %:app-state :server-cmd)
  [ctx]
  (println :db-effect :default :ctx ctx)
  (server-cmd ctx))

(defp server-cmd #%(= %:server-cmd :print)
  [ctx]
  (println :server-cmd :default :ctx ctx))

(defn a-get-user-by-id
  "Internal function used by passport to retrieve the user's full data from the session."
  [user-id]
  (p/let [users-table (db/kv "users")
          user (.get users-table user-id)]
    user))

(def hash-cache (atom {}))

(defn update-app [user update-fn & args]
  (let [old-patches (or (some-> user (j/get :app-state) edn/read-string)
                        {0 [[[] :r default-db]]})
        app-state (->> old-patches sort (map second) (map e/edits->script) (reduce e/patch {}))
        old-app-state-hash (str (hash app-state))
        new-app-state (apply update app-state update-fn args)
        new-app-state-hash (str (hash new-app-state))
        add-patches (e/diff app-state new-app-state)
        new-patch-index (-> old-patches keys sort last (or -1) inc)
        all-patches (merge old-patches {new-patch-index add-patches})]
    (j/assoc! user :app-state (pr-str all-patches))
    (j/assoc! user :index new-patch-index)
    (auth/save-user user)
    (swap! hash-cache assoc (j/get user :user-id) new-app-state-hash)
    (swap! session-mem-db assoc (j/get user :id) new-app-state)))

(defp db-effect #%(-> %:app-state :user-id not)
  [{:keys [user-id app-state]}]
  (p/let [user (a-get-user-by-id user-id)
          email (j/get-in user [:auth :email])]
    (update-app user merge {:email email :user-id user-id})))

(defn add-user-session-dispatcher [user-id]
  (when-not (get @session-mem-db user-id)
    (swap! session-mem-db assoc user-id {})
    (add-watch
     session-mem-db
     user-id
     (fn [key atom old-state new-state]
       (let [old-app-state (get old-state user-id)
             new-app-state (get new-state user-id)]
         (when-not (= [] (e/diff old-app-state new-app-state))
           (db-effect {:user-id user-id :app-state new-app-state})))))))

(defp handle-client #% %:res
  [{:keys [res user-index client-index new-index]}]
  (.json res (clj->js {:status 400
                       :client-cmd :sync-since
                       :sync-since (inc user-index)})))


(defp handle-client #%(or (= 0 %:user-index)
                          (= %:user-index (dec %:client-index)))
  [{:keys [res user user-id new-app-state all-patches new-index
           old-app-state-hash new-app-state-hash]}]
  (j/assoc! user :app-state (pr-str all-patches))
  (j/assoc! user :index new-index)
  (auth/save-user user)
  (swap! session-mem-db assoc user-id new-app-state)
  (swap! hash-cache assoc user-id new-app-state-hash)
  (.json res (clj->js {:status 400
                       :client-cmd :success
                       :success :awesome})))

(defp handle-client #%(< %:client-index %:user-index)
  [{:keys [res user-index client-index old-patches]}]
  (.json res (clj->js {:status 400
                       :client-cmd :sync-back
                       :client-index client-index
                       :user-index user-index
                       :sync-back
                       (->> old-patches
                            (filter (fn [[k _v]] (<= client-index k)))
                            (into (sorted-map)))})))

(defp handle-client #%(not %:user)
  [{:keys [res]}]
  (.json res (clj->js {:status 403
                       :client-cmd :login
                       :login :now})))

(defn update-app-state-when-new
  [{:as ctx :keys [req res user user-id client-patches]}]
  (let [client-new-app-state-hash (j/get-in req [:body :new-app-state-hash])
        client-old-app-state-hash (j/get-in req [:body :old-app-state-hash])
        user-index (or (j/get user :index) 0)
        old-patches (or (some-> user (j/get :app-state) edn/read-string)
                        {0 [[[] :r default-db]]})
        app-state (->> old-patches sort (map second) (map e/edits->script) (reduce e/patch {}))
        old-app-state-hash (str (hash app-state))
        old-hashes-match? (= old-app-state-hash client-old-app-state-hash)
        all-patches (merge old-patches client-patches)
        new-patches (->> all-patches
                         sort
                         (filter #(-> % first (> user-index)))
                         sort
                         (map second)
                         (filter (complement #{[]})))]
    (if-not old-hashes-match?
      (.json res (clj->js {:status 400
                           :client-cmd :sync-back
                           :user-index user-index
                           :client-index (->> client-patches keys sort first)
                           :sync-back (pr-str old-patches)}))
      (when (seq new-patches)
        (let [new-app-state (->> new-patches
                                 (map e/edits->script)
                                 (reduce e/patch app-state))
              new-app-state-hash (str (hash new-app-state))]
          (if (not (s/check :todosync.specs/db new-app-state))
            (.json res (clj->js {:status 400
                                 :client-cmd :sync-since
                                 :sync-since 0}))
            (handle-client
             (merge ctx {:app-state app-state
                         :new-app-state new-app-state
                         :old-app-state-hash old-app-state-hash
                         :new-app-state-hash new-app-state-hash
                         :all-patches all-patches
                         :old-patches old-patches
                         :new-patches new-patches
                         :client-index (-> client-patches keys sort first)
                         :user-index user-index
                         :new-index (-> all-patches keys sort last)}))))))))

(defn check-app-state-hash
  [{:as ctx :keys [res user client-app-state-hash]}]
  (let [current-patches (or (some-> user (j/get :app-state) edn/read-string)
                            {0 [[[] :r default-db]]})
        app-state (->> current-patches sort (map second) (map e/edits->script) (reduce e/patch {}))
        current-app-state-hash (hash app-state)
        old-hashes-match? (= current-app-state-hash client-app-state-hash)]
    (if-not old-hashes-match?
      (.json res (clj->js {:client-cmd :sync-back
                           :sync-back (pr-str current-patches)}))
      (.json res (clj->js {:client-cmd :success})))))

(defn handle-sync [req res & [next force?]]
  (if-let [app-state-check? (some-> req (aget "body") (aget "check-app-state-hash") str)]
    (if (-> req (j/get-in [:user :id]) (@hash-cache) (= app-state-check?))
      (.json res #js {:client-cmd "success"})
      (check-app-state-hash
       {:res res
        :user (j/get req :user)
        :client-app-state-hash app-state-check?}))
    (when-let [client-patches (some-> req (aget "body") (aget "patches") edn/read-string)]
      (let [user (j/get req :user)
            user-id (j/get user :id)]
        (add-user-session-dispatcher user-id)
        (update-app-state-when-new
         {:req req
          :res res
          :user user
          :user-id user-id
          :old-app-state-hash (some-> req (aget "body") (aget "old-app-state-hash") edn/read-string) 
          :client-patches client-patches})))))

(defn setup-routes [app]
  (web/reset-routes app)
  (web/static-folder app "/css" "node_modules/minimal-stylesheet")
  (auth/setup-auth app)
  (auth/setup-email-based-auth app sitefox-template "main")
  (auth/setup-reset-password app sitefox-template "main")
  (j/call app :post "/sync/app-state" handle-sync)
  (-> app
      (.use (fn [req res next]
              (if-not (aget req "user")
                (.redirect res (web/build-absolute-uri req "/auth/sign-up"))
                (let [body-keys (-> (js-keys (aget req "body")))
                      cmds (->> body-keys
                                (filter #{"patches" "check-app-state-hash" "app-state"})
                                seq)
                      url (aget req "url")]
                  (when-not (or (str/starts-with? url "/cljs-runtime")
                                (str/starts-with? url "//cljs-runtime"))
                    (handle-sync req res next true)
                    (.set res "Access-Control-Allow-Origin" "*")
                    (.set res "Access-Control-Allow-Headers" "Content-Type,X-Requested-With")
                    (.set res "Cross-Origin-Opener-Policy" "same-origin")
                    (.set res "Cross-Origin-Embedder-Policy" "require-corp")
                    (next))))))
      (web/static-folder "/" "public")))

(defn main! []
  (p/let [[app host port] (web/start)]
    (reset! server app)
    (setup-routes app)
    (println "Serving on" (str "http://" host ":" port))))

(defn ^:dev/after-load reload []
  (js/console.log "Reloading.")
  (setup-routes @server))
