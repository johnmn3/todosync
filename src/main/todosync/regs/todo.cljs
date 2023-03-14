(ns todosync.regs.todo
  ;; copied mostly from https://github.com/day8/re-frame/tree/master/examples/todomvc
  (:require
   [todosync.regs.db :refer [todo-interceptors check-spec-interceptor ->local-store]]
   [re-frame.core :refer [reg-event-db]]
   [cljs-thread.re-frame :refer [reg-sub subscribe]]))

(defn allocate-next-id
  "Returns the next todo id.
  Assumes todos are sorted.
  Returns one more than the current largest id."
  [todos]
  ((fnil inc 0) (last (keys todos))))

(reg-event-db
 :set-showing

 [check-spec-interceptor
  ->local-store]
 
 (fn [db [_ new-filter-kw]]
   (println :setting-showing new-filter-kw)
   (assoc db :showing new-filter-kw)))

(reg-event-db
 :add-todo
 todo-interceptors
 (fn [todos [_ text]]
   (let [id (allocate-next-id todos)]
     (assoc todos id {:id id :title text :done false}))))

(reg-event-db
 :toggle-done
 todo-interceptors
 (fn [todos [_ id]]
   (update-in todos [id :done] not)))

(reg-event-db
 :save
 todo-interceptors
 (fn [todos [_ id title]]
   (assoc-in todos [id :title] title)))

(reg-event-db
 :delete-todo
 todo-interceptors
 (fn [todos [_ id]]
   (dissoc todos id)))

(reg-event-db
 :clear-completed
 todo-interceptors
 (fn [todos _]
   (let [done-ids (->> (vals todos)
                       (filter :done)
                       (map :id))]
     (reduce dissoc todos done-ids))))

(reg-event-db
 :complete-all-toggle
 todo-interceptors
 (fn [todos _]
   (let [new-done (not-every? :done (vals todos))]
     (reduce #(assoc-in %1 [%2 :done] new-done)
             todos
             (keys todos)))))
(reg-sub
 :showing
 (fn [db _]
   (:showing db)))

(defn sorted-todos
  [db _]
  (:todos db))
(reg-sub :sorted-todos sorted-todos)

(reg-sub
 :todos
 (fn [_query-v _]
   (subscribe [:sorted-todos]))
 (fn [sorted-todos _query-v _]
   (vals sorted-todos)))

(reg-sub
 :visible-todos
 (fn [_query-v _]
   [(subscribe [:todos])
    (subscribe [:showing])])
 (fn [[todos showing] _]
   
   (let [filter-fn (case showing
                     :active (complement :done)
                     :done   :done
                     :all    identity
                     identity)]
     (filter filter-fn todos))))

(reg-sub
 :all-complete?
 :<- [:todos]
 (fn [todos _]
   (every? :done todos)))

(reg-sub
 :completed-count
 :<- [:todos]
 (fn [todos _]
   (count (filter :done todos))))

(reg-sub
 :footer-counts
 :<- [:todos]
 :<- [:completed-count]
 (fn [[todos completed] _]
   [(- (count todos) completed) completed]))
