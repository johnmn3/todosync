(ns todosync.views.todos
  (:require
   [comp.el :as comp]
   [todosync.views.todos.comps :as c]
   [todosync.views.todos.styled :as styled]
   [reagent.core :as reagent]
   [comp.el.aux :as cc]
   [cljs-thread.re-frame :refer [dispatch subscribe]]))


(defn drawer-icon []
  [cc/format-list-bulleted])

(defn new-todo-box []
  (let [all-complete? @(subscribe [:all-complete?])]
    [comp/container styled/new-todo-styles
     [comp/item {:xs 1
                 :on-click #(dispatch [:complete-all-toggle])}
      [comp/arrow-down {:font-size "large"
                        :style (merge {:margin-top 12
                                       :padding-left 2
                                       :color        "#e6e6e6"}
                                      (when all-complete?
                                        {:color "#737373"}))}]]
     [comp/item {:xs 11}
      [c/new-todo]]]))

(defn todo-item []
  (let [editing (reagent/atom false)
        hover-state (reagent/atom false)]
    (fn [{{:as todo :keys [id title]} :todo}]
      [comp/list-item
       (merge styled/todo-item
              {:on-mouse-over #(reset! hover-state true)
               :on-mouse-out #(reset! hover-state false)})
       [c/complete-check todo]
       (if-not @editing
         [c/todo-display (assoc todo :editing editing)
          title]
         [(c/existing-todo
           {:as ::edit-existing-todo
            :props {:todo todo
                    :editing editing}})])
       (when @hover-state
         [c/delete-todo {:is id}])])))

(def todo-list
  (comp/list-items
   {:as ::todo-list
    :props {:id "todo-list"
            :style {:overflow-y "scroll"
                    :height "100%"}}}
    
   #(let [visible-todos @(subscribe [:visible-todos])]
      (->> visible-todos
           reverse
           (mapv (fn [x] [todo-item {:todo x}]))
           (interpose [comp/divider])
           ((fn [f] (conj f [comp/divider])))))))

(defn footer-selectors []
  (let [showing @(subscribe [:showing])]
    [comp/container
     [comp/item {:xs 3}
      [c/filter-all {:selected? showing}]]
     [comp/item {:xs 5}
      [c/filter-active {:selected? showing}]]
     [comp/item {:xs 4}
      [c/filter-done {:selected? showing}]]]))

(defn footer-controls []
  (let [[active done] @(subscribe [:footer-counts])]
    [comp/paper styled/footer-controls
     [comp/container
      [comp/item {:xs 3}
       active " " (case active 1 "item" "items") " left"]
      [comp/item {:xs 5 :container true}
       [footer-selectors]]
      [comp/item {:xs 1}]
      [comp/item {:xs 3
                  :on-click #(dispatch [:clear-completed])
                  :style/hover {:text-decoration "underline"
                                :cursor "pointer"}}
       (when (pos? done)
         "Clear done")]]]))

(defn todo-app [stuff]
  [:div {:style styled/body-style
         :id "scroll-todo-app-div"}
    [comp/box
     {:id "scroll-box"
      :style {:overflow "hidden"
              :bottom 50
              :padding-bottom 50}}
     [comp/paper {:id "scroll-paper"
                  :style {:overflow "hidden"
                          :position "absolute"
                          :min-width "230px"
                          :max-width "92%"
                          :padding 0
                          :padding-right 30
                          :top 100
                          :padding-bottom 50
                          :bottom 100
                          :margin 0
                          :box-shadow (str "0 2px 4px 0 rgba(0, 0, 0, 0.2),"
                                           "0 25px 50px 0 rgba(0, 0, 0, 0.1)")}}
      [new-todo-box]
      [:div {:style {:position "absolute"
                     :width "100%"
                     :top 60
                     :padding-bottom 5
                     :bottom 25
                     :height "auto"
                     :overflow "hidden"}}
       [comp/divider {:style {:top 54 :width "100%"}}]
       (when (seq @(subscribe [:todos]))
         [todo-list])]
      [footer-controls]]
     [:footer styled/todo-app-footer
      [:p "Double-click to edit a todo"]]]])

(defn main [{:as main-args :keys [^js classes]}]
  [comp/grid-container {:component "main" :max-width "xs"
                        :id "main-grid-container"}
   [comp/css-baseline]
   [todo-app]])
