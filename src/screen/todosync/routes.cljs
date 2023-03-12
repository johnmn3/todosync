(ns todosync.routes
  (:require
   [re-frame.core :as rf]
   [reitit.frontend]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [todosync.views.todos :as todos]
   [todosync.views.welcome :as welcome]))


(defn log-fn [& args]
  (fn [& _] 
    #_(apply js/console.log args)))


;;; Subs

(rf/reg-sub
 :current-route
 (fn [db]
   (:current-route db)))

;;; Events

;; (rf/reg-event-fx
;;  :navigate
;;  (fn [_cofx [_ & route]]
;;    {:navigate! route}))

;; Triggering navigation from events.
(rf/reg-fx
 :navigate!
 (fn [route]
   (apply rfe/push-state route)))

(rf/reg-event-db
 :navigated
 (fn [db [_ new-match]]
   (let [old-match   (:current-route db)
         controllers (rfc/apply-controllers (:controllers old-match) new-match)]
     (assoc db :current-route (assoc new-match :controllers controllers)))))

;;; Routes

(def routes
  ["/"
   [""
    {:name :routes/home
     :view welcome/main 
     :link-text "Home"
     :icon welcome/drawer-icon
     :controllers
     [{:start (log-fn "Entering home page")
       :stop (log-fn "Leaving home page")}]}]
   ["todos"
    {:name :routes/todos
     :view todos/main
     :link-text "Todos"
     :icon todos/drawer-icon
     :controllers
     [{:start (log-fn "Entering todos")
       :stop  (log-fn "Leaving todos")}]}]])

(def router
  (reitit.frontend/router
   routes
   {}))

(defn on-navigate [new-match]
  (when new-match
    (rf/dispatch [:navigated new-match])))

(defn init-routes! []
  (rfe/start!
   router
   on-navigate
   {:use-fragment true}))
