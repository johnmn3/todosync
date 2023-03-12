(ns todosync.shell
  (:require
   [reagent.core :as reagent]
   [comp.el :as c]
   [comp.el.aux :as cc]
   [re-frame.core]
   [cljs-thread.re-frame :refer [dispatch subscribe]]
   [reitit.frontend.easy :as rfe]
   [todosync.routes :as routes]
   [todosync.views.todos.comps :as comps]
   [reitit.core :as reitit]))

;; styles

(def classes
  (let [prefix "rmui-dash"]
    {:root               (str prefix "-root")
     :toolbar            (str prefix "-toolbar")
     :toolbar-icon       (str prefix "-toolbar-icon")
     :app-bar            (str prefix "-app-bar")
     :app-bar-shift      (str prefix "-app-bar-shift")
     :menu-button        (str prefix "-menu-button")
     :menu-button-hidden (str prefix "-menu-button-hidden")
     :title              (str prefix "-title")
     :drawer-paper       (str prefix "-drawer-paper")
     :drawer-paper-close (str prefix "-drawer-paper-close")
     :app-bar-spacer     (str prefix "-app-bar-spacer")
     :content            (str prefix "-content")
     :container          (str prefix "-container")
     :paper              (str prefix "-paper")
     :fixed-height       (str prefix "-fixed-height")}))

(def drawer-width 180)

(defn custom-styles [{:as theme-m :keys [theme]}]
  (let [spacing            (:spacing theme)
        create-transitions (fn [& args]
                             (apply (-> theme :transitions :create)
                                    (apply clj->js args)))]
    {(str "&." (:root classes))          {:display "flex"}
     (str "&." (:toolbar classes))       {:padding-right 24}
     (str "&." (:toolbar-icon classes))  {:display         "flex"
                                          :align-items     "center"
                                          :justify-content "flex-end"
                                          :padding         "0 8px"}
     (str "& ." (:app-bar classes))       {:z-index    (+ (-> theme :z-index :drawer) 1)
                                           :transition (let [create (-> theme :transitions :create)]
                                                         (create #js ["width" "margin"]
                                                                 #js {:easing   (-> theme :transitions :easing :sharp)
                                                                      :duration (-> theme :transitions :duration :leaving-screen)}))}
     (str "& ." (:app-bar-shift classes)) {:margin-left drawer-width
                                           :width       (str "calc(100% - " drawer-width "px)")
                                           :transition  (let [create (-> theme :transitions :create)]
                                                          (create #js ["width" "margin"]
                                                                  #js {:easing   (-> theme :transitions :easing :sharp)
                                                                       :duration (-> theme :transitions :duration :entering-screen)}))}
     (str "& ." (:menu-button classes))  {:margin-right 36}
     (str "& ." (:title classes))        {:flexGrow 1}
     (str "& ." (:drawer-paper classes)) {:position    "fixed"
                                          :white-space "nowrap"
                                          :bottom      0
                                          :overflow    "hidden"
                                          :top         (-> theme :mixins :toolbar :min-height (+ 6))
                                          :width       drawer-width
                                          :transition  (let [create (-> theme :transitions :create)]
                                                         (create "width"
                                                                 #js {:easing   (-> theme :transitions :easing :sharp)
                                                                      :duration (-> theme :transitions :duration :leaving-screen)}))}
     (str "& ." (:drawer-paper-close classes)) {:overflow-x                        "hidden"
                                                :overflow                          "hidden"
                                                :height                            "100%"
                                                :position                          "fixed"
                                                :transition                        (let [create (-> theme :transitions :create)]
                                                                                     (create "width"
                                                                                             #js {:easing   (-> theme :transitions :easing :sharp)
                                                                                                  :duration (-> theme :transitions :duration :entering-screen)}))
                                                :width                             0
                                                ((-> theme :breakpoints :up) "sm") {:width (spacing 6)}}
     (str "& ." (:app-bar-spacer classes)) (-> theme :mixins :toolbar)
     (str "& ." (:content classes)) {:flexGrow 1
                                     :overflow "hidden"}
     (str "& ." (:container classes)) {:padding-top    0
                                       :padding-bottom 0}
     (str "& ." (:paper classes)) {:display       "flex"
                                   :overflow      "hidden"
                                   :flex-direction "column"}
     (str "& ." (:fixed-height classes)) {:height 0}}))

;;; Components

(defn list-item [{:keys [selected route-name text icon]}]
  [c/list-item {:style {:padding 10}
                :button true
                :selected selected
                :on-click #(rfe/push-state route-name)}
   [c/list-item-icon [icon]]
   [c/list-item-text {:primary text}]])

(defn account-menu []
  (let [anchor-el (reagent/atom nil)]
    (fn []
      (let [a-open? @(subscribe [:account-menu/open?])
            a-close #(dispatch [:account-menu/toggle])
            logged-in? @(subscribe [:account-menu/logged-in?])]
        [:div
         {:on-click #(do (println :div-clicked)
                         (when a-open? (a-close)))}
         [c/icon-button {:style {:z-index 100}
                         :on-click #(do (dispatch [:account-menu/toggle])
                                        (when @anchor-el (reset! anchor-el nil))
                                        (reset! anchor-el (.-currentTarget %)))}
          [c/avatar]]
         [c/menu {:anchor-el @anchor-el
                  :open (boolean a-open?)
                  :on-close a-close
                  :on-click a-close
                  :keepMounted true
                  :transformOrigin {:horizontal "right" :vertical "top"}
                  :anchorOrigin {:horizontal "right" :vertical "bottom"}}
          (if logged-in?
            [c/menu-item
             {:on-click #(do (a-close)
                             (set! js/document.location "/auth/sign-in"))}
             [c/list-item-icon
              [c/account-circle]]
             "Sign In"]
            [c/menu-item
             {:on-click #(do (a-close)
                             (set! js/document.location "/auth/sign-out"))}
             [c/list-item-icon
              [c/exit-to-app {:fontSize "small"}]]
             "Logout"])]]))))

(defn todosync [{:keys [class-name]}]
  (let [open? @(subscribe [:drawer/open?])
        dark-theme? @(subscribe [:dark-theme?])
        router routes/router
        current-route @(re-frame.core/subscribe [:current-route])
        goal-chips @(subscribe [:welcome/goal-chips])]
    [c/box {:class [class-name (:root classes)]}
     [c/app-bar {:class [(:app-bar classes)]}
      (->> goal-chips
           (filter #(-> % second :outline))
           (mapv (fn [_] [cc/icon-egg]))
           (into [c/toolbar {:class (:toolbar classes)}
                  [:div {:style {:width 7}}]
                  [c/icon-button {:edge "start"
                                  :color "inherit"
                                  :aria-label "open drawer"
                                  :on-click #(if open?
                                               (dispatch [:drawer/close])
                                               (dispatch [:drawer/open]))
                                  :class [(:menu-button classes)]}
                   [c/avatar
                    [cc/add-circle-outline]]]
                  [comps/todo-header-title
                   (merge
                    {:color "inherit"
                     :class (:title classes)}
                    (when dark-theme?
                      {:style {:color "rgba(175, 47, 47, 0.25)"}}))
                   "todos"]])
           (#(into % [[account-menu]])))]


     [c/drawer {:variant "permanent"
                :classes {:paper (str (:drawer-paper classes) " "
                                      (if open? "" (:drawer-paper-close classes)))}
                :open open?}
      [c/divider]
      [c/list-items
       (for [route-name (reitit/route-names router)
             :let [route (reitit/match-by-name router route-name)
                   text (-> route :data :link-text)
                   icon (-> route :data :icon)
                   selected? (= route-name (-> current-route :data :name))]]
         ^{:key route-name} [list-item {:text text
                                        :icon icon
                                        :route-name route-name
                                        :selected selected?}])]
      [c/divider]
      [c/list-items
        {:style {:bottom 65
                 :width "100%"
                 :position "absolute"}}
        [c/list-item {:button true
                      :on-click #(dispatch [:toggle-dark-theme])}
         [c/list-item-icon
          {:style {:padding-left 5}}
          [c/switch {:size "small" :checked (or dark-theme? false)}]]
         [c/list-item-text {:primary "Toggle Theme"}]]]]
     [:main {:class (:content classes)
             :style {:height "100%"}
             :id "outer-main"}

      [:div {:class (:app-bar-spacer classes)}]
      (when current-route
        [(-> current-route :data :view) {:classes classes}])]]))

(def styled-todosync (c/styled todosync custom-styles))
