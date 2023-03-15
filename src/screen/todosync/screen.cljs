(ns todosync.screen
  (:require
   [reagent.dom :as rdom]
   [comp.el :as c]
   [cljs-thread.core :as thread]
   [cljs-thread.re-frame :refer [dispatch subscribe]]
   [cljs-thread.idb :as idb :refer [idb-get]]
   [todosync.routes :as routes]
   [todosync.shell :as shell]
   [todosync.regs.db :as db]
   [todosync.regs.todo]
   [todosync.regs.shell]
   [re-frame.core :as rf]
   [cljs-thread.env :as e]))

;;; Config
(enable-console-print!)
; for docs release
(when (e/in-screen?)
  #_(thread/init!
     {:sw-connect-string "/cljs-thread/sw.js"
      :repl-connect-string "/cljs-thread/repl.js"
      :core-connect-string "/cljs-thread/core.js"})

  ;; #_
  (thread/init!
   {:sw-connect-string "/sw.js"
    :repl-connect-string "/repl.js"
    :core-connect-string "/core.js"
    :future-count 2
    :injest-count 0}))

(def debug?
  ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (println "dev mode")))

;; Styles
(defn custom-theme [dark-theme?]
  {:palette {:mode (if dark-theme? "dark" "light")
             :primary {:main "#f5f5f5" #_ "#ef5350"}
             :secondary {:main "#3f51b5"}}
   :status {:danger "red"}})

;; Views

(defn main-shell [{:keys [router]}]
  (let [dark-theme? @(subscribe [:dark-theme?])]
    [:<>
     [c/css-baseline]
     [c/theme-provider (c/create-theme (custom-theme dark-theme?))
      [shell/styled-todosync]]]))

;;; Setup on screen

(defn ^{:after-load true, :dev/after-load true} mount-root []
  (routes/init-routes!)
  (rdom/render [main-shell]
               (.getElementById js/document "app")))

(defn init! []
  (when (e/in-screen?)
    (idb-get db/ls-key
             (fn [res]
               (rf/dispatch [:set-db (:res res)])))
    (rf/dispatch-sync [:initialize-db])
    (rf/clear-subscription-cache!)

    (dev-setup)
    (mount-root)))
