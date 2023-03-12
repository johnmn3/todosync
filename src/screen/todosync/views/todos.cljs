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
                                      ;;  :padding-top 20 ;8 ;3
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
                    ;; :bottom "-5px"
                    :height "100%" #_ "75%"}}}
    
   #(let [visible-todos @(subscribe [:visible-todos])]
      (println :visible-todos visible-todos)
      (->> visible-todos
           reverse
           (mapv (fn [x] [todo-item {:todo x}]))
           (interpose [comp/divider])
           (into [#_[comp/divider]])
           ((fn [f] (conj f [comp/divider])))))))

(defn footer-selectors []
  (let [showing @(subscribe [:showing])]
    [comp/container
    ;;  {:style {:padding 0}}
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
    ;;   {:style {:padding 0}}
      [comp/item {:xs 3}
       active " " (case active 1 "item" "items") " left"]
      ;; [comp/item {:xs 1}]
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
  (println :todo-app-stuff stuff)
  [:div {:style styled/body-style
         :id "scroll-todo-app-div"}
  ;;  [:div ;comp/paper
  ;;   {:style {:bottom 50}} ;50
    ;;                     ;:padding-right 50
    ;;                     ;; :padding 50
    ;;          :width "110%"}}
    ;;               ;; :min-width "230px"
    ;;               ;; :max-width "550px"}}
    [comp/box
     {:id "scroll-box"
      :style {;:padding-top 80
            ;;  :padding-bottom 30
              ;; :min-width "330px"
              ;; :max-width "650px"
              ;; :width "90%"
              ;; :position "absolute" ; "fixed"
              :overflow "hidden"
              :bottom 50
              ;; :padding-left 50
              ;; :padding-right 50
            ;;  :width "120%"
            ;;  :margin-right 50
              ;; :left "25%" ;"-5px"
              ;; :width "120%"
            ;;  :height "95%" ;"90%"
              :padding-bottom 50 ;80 ;60 ;120
              ;; :padding-right 30
              ;; :top 80 
              #_#_
              :bottom 80}}
    ;; [c/todo-header-title "todos"]
     [comp/paper {:id "scroll-paper"
                  :style {;:padding-top 160
                          ;; :height "100%" ;"90%" ; "75%"
                          ;; :width "90%"
                          :overflow "hidden"
                          :position "absolute"
                          :min-width "230px"
                          :max-width "92%" ;"350px"
                          :padding 0
                          :padding-right 30
                          ;; :padding-right 30
                          :top 100
                          :padding-bottom 50 ;10
                          :bottom 100 ; 20
                         ;;  :width "100%"
                          ;; :padding-bottom 20
                         ;;  :margin-right 50
                          :margin 0
                          :box-shadow (str "0 2px 4px 0 rgba(0, 0, 0, 0.2),"
                                           "0 25px 50px 0 rgba(0, 0, 0, 0.1)")}}
      [new-todo-box]
      [:div {:style {;:height "88%" ;"100%"
                     :position "absolute"
                     :width "100%"
                     :top 60
                     :padding-bottom 5 ;30
                     ;; :margin 0
                     :bottom 25 ;0 ;25 ;"-10px"
                     :height "auto"
                     :overflow "hidden"}}
       [comp/divider {:style {:top 54 :width "100%"}}] ;:padding-bottom 2}}]
       (when (seq @(subscribe [:todos]))
         [todo-list])]
      [footer-controls]]
     [:footer styled/todo-app-footer
      [:p "Double-click to edit a todo"]]]])

(defn main [{:as main-args :keys [^js classes]}]
  (println :main-args main-args)
  [comp/grid-container {:component "main" :max-width "xs"
                        :id "main-grid-container"}
                        ;; :style {:height "90%"}} ; "100%"
                        ;;         ;; :top 50
                        ;;         :bottom 0}}
   [comp/css-baseline]
   [todo-app]])
;;    [(c/styled sign-in custom-sign-in-styles) classes]
;;    [c/box {:mt 8}
;;     [copyright]]])
