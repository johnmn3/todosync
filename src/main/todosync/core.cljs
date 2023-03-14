(ns todosync.core
  (:require
   [re-frame.core :as rf]
   [todosync.regs.db]
   [todosync.regs.shell]
   [todosync.regs.welcome]
   [todosync.regs.todo]
   [cljs-thread.env :as env]
   [cljs-thread.core]))

(enable-console-print!)

(defn init! []
  (when (or (env/in-screen?) (env/in-core?))
    (rf/dispatch-sync [:initialize-db])
    (rf/clear-subscription-cache!)))
