(ns todosync.views.welcome
  (:require
   [reagent.core :as reagent]
   [comp.el :as c]
   [todosync.footer :refer [copyright]]))

(defn drawer-icon []
  [c/home])

;;; Styles
(def classes
  (let [prefix "rmui-dash-welcome"]
    {:avatar (str prefix "-avatar")
     :paper  (str prefix "-paper")
     :form   (str prefix "-form")
     :submit (str prefix "-submit")}))

(defn custom-welcome-styles [{:keys [theme]}]
  (let [spacing            (:spacing theme)
        create-transitions (fn [& args]
                             (apply (-> theme :transitions :create)
                                    (apply clj->js args)))]
    {(str "& ." (:paper classes))   {:marginTop     (spacing 8)
                                     :display       "flex"
                                     :flexDirection "column"
                                     :alignItems    "center"}
     (str "& ." (:avatar classes)) {:margin          (spacing 1)
                                    :backgroundColor (-> theme :palette :secondary :main)}
     (str "& ." (:form classes))    {:width     "100%"  ; Fix IE 11 issue
                                     :marginTop (spacing 1)}
     (str "& ." (:submit classes)) {:margin (spacing 3 0 2)}}))

;; Components

(defn welcome [{:keys [^js classes] :as props}]
  (let [form (reagent/atom {:userid "" :password "" :remember? false})]
    (fn []
      [c/css-baseline]
      [:div {:class (:paper classes)
             :style {:marginTop     (str (* 8 8) "px")
                     :display       "flex"
                     :flexDirection "column"
                     :alignItems    "center"}}
       [c/text {:component "h1"
                :variant "h4"
                :style {:padding-bottom 40}}
        "Welcome to Todosync"]
       [c/text
        "This app syncronizes it's state between all your devices logged in"]])))

(defn main [{:keys [^js classes]}]
  [c/grid-container {:component "main" :max-width "xs"}
   [c/css-baseline]
   [(c/styled welcome custom-welcome-styles) classes]
   [c/box {:mt 8}
    [copyright]]])
