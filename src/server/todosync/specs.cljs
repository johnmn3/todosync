(ns todosync.specs 
  (:require [clojure.spec.alpha :as s]))

(s/def ::id int?)
(s/def ::title string?)
(s/def ::done boolean?)
(s/def ::todo (s/keys :req-un [::id ::title ::done]))
(s/def ::todos (s/and
                (s/map-of ::id ::todo)
                #(do true)))

(s/def ::showing
  #{:all :active :done})

(s/def ::dark-theme? boolean?)
(s/def ::drawer-open? boolean?)
(s/def ::logged-in? boolean?)
(s/def ::user-id string?)
(s/def ::email string?)
(s/def ::db
  (s/keys :req-un
          [::todos ::showing ::dark-theme? ::drawer-open? ::logged-in?
           ::user-id ::email]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(defn check
  "Checks if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (s/valid? a-spec db))
