(ns todosync.views.todos.affects
  (:require
   [comp.props :refer [void props]]))

(def void-todo
  (void
   {:as ::void-todo
    :props/void [:id :editing :on-save :on-stop :title :done :selected? :new?]}))

(def selected?
  (props
   {:as ::selected?
    :props/void [:selected? :on-selected]
    :props/ef (fn [{:as props :keys [on-selected selected?]}
                   {:keys [is]}]
                (-> (merge ;{:href (str "#/todos/" (some-> is name))}
                           (when (= is selected?)
                             ((or on-selected identity) props)))))}))
