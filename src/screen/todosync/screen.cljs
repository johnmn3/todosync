(ns todosync.screen
  (:require
   [reagent.dom :as rdom]
   [comp.el :as c]
   [cljs-thread.core :as thread]
   [cljs-thread.re-frame :refer [subscribe]]
   [todosync.routes :as routes]
   [todosync.shell :as shell]))

;;; Config
(enable-console-print!)
; for docs release
#_
(thread/init!
 {:sw-connect-string "/cljs-thread/sw.js"
  :repl-connect-string "/cljs-thread/repl.js"
  :core-connect-string "/cljs-thread/core.js"})

;; #_
(thread/init!
 {:sw-connect-string "/sw.js"
  :repl-connect-string "/repl.js"
  :core-connect-string "/core.js"
  :future-count 2
  :injest-count 0})

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
  (dev-setup)
  (mount-root))
