(ns todosync.views.todos.comps
  (:require
;;    [re-frame.core :refer [dispatch]]
   [cljs-thread.re-frame :refer [dispatch subscribe]]
   [clojure.string :as str]
   [reagent.core :as r]
   [comp.el :as comp]
   [todosync.views.todos.affects :as a]
   [todosync.views.todos.styled :as styled]))

(defn unchecked [props]
  [:span (merge props styled/circle)])

(defn checked [props]
  [:span (styled/deep-merge props styled/checked-circle styled/check)
   [:div styled/check-leg]
   [:div styled/check-foot]])

(defn complete-check [{:keys [id done]}]
  (let [toggle #(dispatch [:toggle-done id])]
    (if done
      [checked {:on-click toggle}]
      [unchecked {:on-click toggle}])))

(def delete-todo
  (comp/div
   {:as ::delete-todo :with [styled/delete-todo a/void-todo]
    :on-click #(dispatch [:delete-todo (:is %)])}
   "×"))

(def todo-display
  (comp/label
   {:as ::todo-display :with [styled/todo-display a/void-todo]
    :props/ef (fn [{:as todo :keys [editing done]}]
                (let [dark-theme? @(subscribe [:dark-theme?])
                      theme [done dark-theme?]
                      font-color (case theme
                                   [true true] "#4d4d4d"
                                   [true false] "#e6e6e6"
                                   [false true] "#f5f5f5"
                                   [false false] "#4d4d4d")]
                  (merge {:on-double-click #(reset! editing true)}
                         (if-not (:done todo)
                           {:style {:color font-color}}
                           (assoc-in styled/todo-done [:style :color] font-color)))))}))
                         

(def todo-input
  (comp/raw-input
   {:as ::todo-input :with [styled/todo-input a/void-todo]
    :props/void :af-state
    :props/ef (fn [{:keys [on-save on-stop af-state style]}]
                (let [stop #(do (reset! af-state "")
                                (when on-stop (on-stop)))
                      save #(do (on-save (some-> af-state deref str str/trim))
                                (stop))
                      dark-theme? @(subscribe [:dark-theme?])]
                  {:auto-focus  true
                   :style       (merge style {:color (if dark-theme? "white" "black")})
                   :on-blur     save
                   :value       (some-> af-state deref)
                   :on-change   (fn [ev] (reset! af-state (-> ev .-target .-value)))
                   :on-key-down #(case (.-which %)
                                   13 (save)
                                   27 (stop)
                                   nil)}))}))

(def new-todo
  (todo-input
   {:as ::new-todo :with styled/new-todo
    :props {:placeholder "What needs to be done?"
            :af-state (r/atom nil)
            :on-save #(when (seq %)
                        (dispatch [:add-todo %]))}}))

(def existing-todo
  (todo-input
   {:as ::existing-todo :with styled/edit-todo
    :props/af (fn [{:keys [editing]
                    {:keys [id title]} :todo}]
                {:af-state (r/atom title)
                 :on-save #(if (seq %)
                             (dispatch [:save id %])
                             (dispatch [:delete-todo id]))
                 :on-stop #(reset! editing false)})}))

(def todo-header-title
  (comp/box
   {:as ::todo-header-title :with styled/todo-header-title}))

(def filter-anchor
  (comp/a
   {:as ::a :with [styled/filter-anchor a/selected?]
    :props {:on-click #(dispatch [:set-showing (:is %)])
            :on-selected #(update % :style
                                  assoc :border-color
                                  "rgba(175, 47, 47, 0.2)")}}))

(def filter-all
  (filter-anchor
   {:as :all :with a/void-todo}
   "All"))

(def filter-active
  (filter-anchor
   {:as :active :with a/void-todo}
   "Active"))

(def filter-done
  (filter-anchor
   {:as :done :with a/void-todo}
   "Completed"))
