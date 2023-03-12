(ns comp.el.aux
  (:require
   [comp.el :as c]
   [reagent-mui.icons.egg :as mui-egg]
   [reagent-mui.icons.add-circle-outline :as mui-add-circle-outline]
   [reagent-mui.icons.format-line-spacing :as mui-format-list-spacing]
   [reagent-mui.icons.format-list-bulleted :as mui-format-list-bulleted]
   [reagent-mui.icons.arrow-upward :as mui-arrow-upward]
   [reagent-mui.icons.arrow-downward :as mui-arrow-downward]
   [reagent-mui.material.chip :as mui-chip]))

(def icon-egg (c/el {:as ::icon-egg :props {:comp mui-egg/egg}}))
(def icon-upward-arrow (c/el {:as ::icon-upward-arrow :props {:comp mui-arrow-upward/arrow-upward}}))
(def icon-downward-arrow (c/el {:as ::icon-downward-arrow :props {:comp mui-arrow-downward/arrow-downward}}))
(def chip (c/el {:as ::chip :props {:comp mui-chip/chip}}))
(def format-list-bulleted (c/el {:as ::format-list-bulleted :props {:comp mui-format-list-bulleted/format-list-bulleted}}))
(def format-list (c/el {:as ::format-list :props {:comp mui-format-list-spacing/format-line-spacing}}))
(def add-circle-outline (c/el {:as ::add-circle-outline :props {:comp mui-add-circle-outline/add-circle-outline}}))
