(ns cljs-thread.re-frame
  (:require
   [reagent.ratom :as ra]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [cljs-thread.re-state :as state]
   [cljs-thread.core :refer [in]]
   [cljs-thread.env :as env]))

(def ^:export reg-sub
  (if (env/in-core?)
    (fn [& args]
      (apply re-frame/reg-sub args))
    (fn [& args]
      (apply re-frame/reg-sub args))))

(defn ^:export dispatch
  [event & [screen-only?]]
  (if (env/in-core?)
    (do (when-not screen-only? (re-frame/dispatch event))
        (in :screen (re-frame/dispatch event)))
    (do (re-frame/dispatch event)
        (when-not screen-only? (in :core (re-frame/dispatch event))))))

(defonce ^:export trackers
  (atom {}))

(defn- ^:export reg-tracker
  [ts id sub-v]
  (if ts
    (update ts :subscribers conj id)
    (let [new-sub (re-frame/subscribe sub-v)]
      {:tracker (r/track!
                 #(let [sub @new-sub]
                    @(in :screen (swap! state/subscriptions assoc sub-v sub)))
                 [])
       :subscribers #{id}})))

(defn ^:export add-sub
  [[id sub-v]]
  (swap! trackers update sub-v reg-tracker id sub-v))

(defn- ^:export unreg-tracker
  [ts id sub-v]
  (if-let [t (get ts sub-v)]
    (let [{:keys [tracker subscribers]} t
          new-subscribers (disj subscribers id)]
      (if (< 0 (count new-subscribers))
        (assoc ts sub-v {:tracker tracker :subscribers new-subscribers})
        (do
          (r/dispose! tracker)
          @(in :screen (swap! state/subscriptions dissoc sub-v))
          (dissoc ts sub-v))))
    ts))

(defn ^:export dispose-sub
  [[id sub-v]]
  (swap! trackers unreg-tracker id sub-v))

(defn ^:export subscribe
  [sub-v & [alt]]
  (if (env/in-core?)
    (re-frame/subscribe sub-v)
    (let [id (str (random-uuid))]
      (in :core (add-sub [id sub-v]))
      (ra/make-reaction
       #(if (contains? @state/subscriptions sub-v)
          (get @state/subscriptions sub-v alt)
          (when-let [sub (re-frame/subscribe sub-v)]
            @sub))
       :on-dispose
       #(in :core (dispose-sub [id sub-v]))))))
