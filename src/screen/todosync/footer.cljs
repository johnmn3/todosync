(ns todosync.footer
  (:require
   [comp.el :as c]))

(defn copyright []
  [c/text {:variant "body2" :color "textSecondary" :align "center" :padding 5}
   [c/link {:color "inherit" :href "https://github.com/johnmn3/cljs-thread"}
    "Terms and Conditions"]
   " ~ nestegg ~ "
   (.getFullYear (js/Date.))])
