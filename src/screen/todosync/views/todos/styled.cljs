(ns todosync.views.todos.styled
  (:require
   [comp.el :as comp]))

(defn deep-merge [& maps]
  (apply merge-with
         (fn [& args]
           (if (every? map? args)
             (apply deep-merge args)
             (last args)))
         maps))

(def new-todo-common
  {:font-size      "24px"
   :font-weight    300
   :margin         0
   :color  "#e6e6e6"
   :background     "rgba(0, 0, 0, 0.003)"
   :line-height    "1.4em"
   :outline        "none"
   :box-sizing     "border-box"
   :font-smoothing "antialiased"})

(def placehoder-styles
  {:font-style  "italic"
   :font-weight 300
   #_#_
   :color       "#e6e6e6"})

;; (def todo-input
;;   {:style {:border         0
;;            :width          "100%"
;;            :padding-left   20
;;            :padding-top    8
;;            :padding-bottom 10}})

(def todo-input
  (comp/el
   {:as ::todo-input
    :props {:style {:border         0
                    :width          "100%"
                    :padding-left   20
                    :padding-top    12
                    :padding-bottom 13}}}))

(def new-todo-styles
  {:style/-webkit-input-placeholder placehoder-styles
   :style/-moz-placeholder          placehoder-styles
   :style/input-placeholder         placehoder-styles
   :style                           (merge new-todo-common
                                           {:width          "100%"
                                            :font           "14px 'Helvetica Neue', Helvetica, Arial, sans-serif"
                                            :padding-top    0
                                            :padding-left   8
                                            :padding-bottom 4
                                            :box-shadow     "inset 3 -2px 1px rgba(0,0,0,0.03)"})})

(def new-todo
  (comp/el
   {:as ::new-todo
    :props new-todo-styles}))

(def edit-todo
  (comp/el
   {:as ::edit-todo
    :props {:style/-webkit-input-placeholder placehoder-styles
            :style/-moz-placeholder          placehoder-styles
            :style/input-placeholder         placehoder-styles
            :style
            (merge new-todo-common
                ;;    #_
                   {:width          "100%"
                    :font-weight    300
                    :border         0
                    ;; :color          "#0f0f0f"
                    :font           "16px 'Helvetica Neue', Helvetica, Arial, sans-serif"
                    :padding-top    14
                    :padding-left   20
                    :padding-bottom 14
                    :box-shadow     "inset 3 -2px 1px rgba(0,0,0,0.03)"})}}))

(def circle
  {:style {:width              "1.7em"
           :height             "1.6em"
        ;;    :background-color   "white"
           :border-radius      "50%"
           :vertical-align     "middle"
           :border             "1px solid #eee"
           :appearance         "none"
           :-webkit-appearance "none"
           :outline            "none"
           :cursor             "pointer"
           :font-size          "large"}})

(def checked-circle
  {:style {:width              "1.7em"
           :height             "1.6em"
        ;;    :background-color   "white"
           :border-radius      "50%"
           :vertical-align     "middle"
           :border             "1px solid #ddd"
           :appearance         "none"
           :-webkit-appearance "none"
           :outline            "none"
           :cursor             "pointer"
           :font-size          "large"}})

(def check
  {:style {:-ms-transform     "rotate(45deg)"; /* IE 9 */
           :-webkit-transform "rotate(45deg)"; /* Chrome, Safari, Opera */
           :transform         "rotate(45deg)"}})

(def check-leg
  {:style {:position         "absolute"
           :width            "2px"
           :height           "16px"
           :background-color "#5dc2af"
           :left             "16px"
           :top              "4px"}})

(def check-foot
  {:style {:position         "absolute"
           :width            "9px"
           :height           "2px"
           :background-color "#5dc2af"
           :left             "9px"
           :top              "20px"}})

(def delete-todo
  (comp/el
   {:as ::delete-todo
    :props {:style/hover {:color "#af5b5e"}
            :style       {:position   "absolute"
                          :right      0
                          :bottom     0
                          :width      "40px"
                          :height     "40px"
                          :font-size  "30px"
                          :color      "#cc9a9a"
                          :transition "color 0.2s ease-out"}}}))

(def todo-display
  (comp/el
   {:as ::todo-display
    :props (deep-merge
            new-todo-styles
            {:style (merge
                     new-todo-common
                     {
                    ;;   :color          "#4d4d4d"
                      :font           "14px 'Helvetica Neue', Helvetica, Arial, sans-serif"
                      :font-weight    300
                      :width          "100%"
                      :padding-top    12
                      :padding-bottom 13
                      :padding-left   20
                      :transition     "color 0.4s"})})}))

(def todo-item
  {:style {:padding-left  12 ;10
           :padding-right 50}})

(def todo-done
  {:style {:text-decoration "line-through"
           #_#_
           :color           "#d9d9d9"}})


(def todo-header-title-style
  {:style {:text-align     "center"
           :font-size      40
          ;;  :padding-right  100
           :color          "rgba(175, 47, 47, 0.15)"}})

(def todo-header-title
  (comp/el
   {:as ::todo-display
    :props todo-header-title-style}))

(def filter-anchor
  (comp/el
   {:as ::filter-anchor
    :props {:style/hover {:border-color "rgba(175, 47, 47, 0.1)"}
            :style       {:color           "inherit"
                          :cursor          "pointer"
                          :margin          "3px"
                          :padding         "3px 7px"
                          :text-decoration "none"
                          :border          "1px solid transparent"
                          :border-color    "rgba(175, 47, 47, 0.0)"
                          :border-radius   "3px"}}}))

(def footer-controls
  {:style {:color         "#777"
           :padding       "10px 15px"
           :padding-left  0
           :padding-right 0
           :font-size     12
           :z-index 10
           :position "absolute"
           :bottom 0 ;"-10px" ;15 ;80 ;0; 10 ;18 ; 26 ;"30"
           :width "100%"
           :height        "35px"
           :text-align    "center"
           :border-top    "1px solid #e6e6e6"
           :box-shadow    (str
                           "0 1px 1px rgba(0, 0, 0, 0.05), "
                           "0 8px 0 -3px rgba(0, 0, 0, 0.05), "
                           "0 9px 1px -3px rgba(0, 0, 0, 0.05), "
                           "0 16px 0 -6px rgba(0, 0, 0, 0.05), "
                           "0 17px 2px -6px rgba(0, 0, 0, 0.05)")}})
                        ;;    "0 1px 1px, "
                        ;;    "0 8px 0 -3px, "
                        ;;    "0 9px 1px -3px, "
                        ;;    "0 16px 0 -6px, "
                        ;;    "0 17px 2px -6px")}})
                        ;;    "0 1px 1px rgba(0, 0, 0, 0.2), "
                        ;;    "0 8px 0 -3px, "
                        ;;    "0 9px 1px -3px rgba(0, 0, 0, 0.2), "
                        ;;    "0 16px 0 -6px, "
                        ;;    "0 17px 2px -6px rgba(0, 0, 0, 0.2)")}})
                            ;;    "0 8px 0 -3px #f6f6f6, "
                            ;;    "0 9px 1px -3px rgba(0, 0, 0, 0.2), "
                            ;;    "0 16px 0 -6px #f6f6f6, "
                            ;;    "0 17px 2px -6px rgba(0, 0, 0, 0.2)")}})

(def todo-app-footer
  {:style {;:margin      "20px auto 0"
          ;;  :margin-top 35 ; 55
           :bottom 40 ;"-15px"
          ;;  :padding-bottom 1
           :padding-top 80
           :padding-left 130
          ;;  :bottom 0 ;"-30px"
          ;;  :left "40%"
          ;;  :float "center"
           :position "absolute"
           :color       "#bfbfbf"
           :font-size   "10px"
           :text-shadow "0 1px 0 rgba(255, 255, 255, 0.5)"
           :text-align  "center"
           :line-height "1"}})

(def body-style
  {
  ;;  :padding 0 ;50 ;"0 auto"
  ;;  :margin-right 30
  ;;  :padding-right 30
  ;;  :padding-bottom 20
  ;;  :padding-top 10
   :font "14px 'Helvetica Neue', Helvetica, Arial, sans-serif"
   :line-height "1.4em"
  ;;  :position "fixed"
;;    :background "#f5f5f5"
;;    :color "#4d4d4d"
  ;;  :min-width "330px"
  ;;  :max-width "650px"
  ;;  :width 700
  ;;  :height "100%"
  ;;  :margin 0 ; "0 auto"
  ;;  :bottom 50
   :overflow "hidden"
   :-webkit-font-smoothing "antialiased"
   :-moz-font-smoothing "antialiased"
   :font-smoothing "antialiased"
   :font-weight 300})
