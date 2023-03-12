(ns todosync.views.welcome
  (:require
   [reagent.core :as reagent]
   [comp.el :as c]
   [comp.el.aux :as cc]
   [cljs-thread.re-frame :refer [dispatch subscribe]]
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

(defn custom-sign-in-styles [{:keys [theme]}]
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

(defn sign-in [{:keys [^js classes] :as props}]
  (let [form (reagent/atom {:userid "" :password "" :remember? false})
        errors (subscribe [:sign-in/errors])
        goal-chips (subscribe [:welcome/goal-chips])]
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
        "Welcome to Todo Sync"]
       [c/text {:component "h1"
                :variant "h6"
                :style {:padding-bottom 40}}
        [:a {:href "/auth/sign-in"}
         "Sign in"]
        " to get started"]
       (into [c/container]
             (->> @goal-chips
                  (mapv (fn [[k v]]
                          [c/item {:xs true
                                   :style {:padding 10 #_5}}
                           [cc/chip
                            (merge (dissoc v :outline)
                                   (when-let [o (:outline v)]
                                     {:variant "outlined"})
                                   {:on-click
                                    #(let [new-goal-chip (update v :outline not)]
                                       (dispatch [:welcome/assoc-goal-chip new-goal-chip]))})]]))))])))

(defn main [{:keys [^js classes]}]
  [c/grid-container {:component "main" :max-width "xs"}
   [c/css-baseline]
   [(c/styled sign-in custom-sign-in-styles) classes]
   [c/box {:mt 8}
    [copyright]]])
