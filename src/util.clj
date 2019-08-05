(ns util
  (:require [re-frame-datatable.core :as dt]))


(comment

  ; [f-1] clojure.pprint/print-table
  ;      https://github.com/clojure/clojure/blob/master/src/clj/clojure/pprint/print_table.clj

  ;   Copyright (c) Rich Hickey. All rights reserved.
  ;   The use and distribution terms for this software are covered by the
  ;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
  ;   which can be found in the file epl-v10.html at the root of this distribution.
  ;   By using this software in any fashion, you are agreeing to be bound by
  ;   the terms of this license.
  ;   You must not remove this notice, or any other, from this software.

  (in-ns 'clojure.pprint)

  (defn print-table
        "Prints a collection of maps in a textual table. Prints table headings
       ks, and then a line of output for each row, corresponding to the keys
       in ks. If ks are not specified, use the keys of the first item in rows."
    {:added "1.3"}
    ([ks rows
      (when (seq rows)
        (let [widths (map
                       (fn [k]
                         (apply max (count (str k)) (map #(count (str (get % k))) rows)))
                       ks)
              spacers (map #(apply str (repeat % "-")) widths)
              fmts (map #(str "%" % "s") widths)
              fmt-row (fn [leader divider trailer row]
                        (str leader
                             (apply str (interpose divider
                                                   (for [[col fmt] (map vector (map #(get row %) ks) fmts)]
                                                     (format fmt (str col)))))
                             trailer))]
          (println)
          (println (fmt-row "| " " | " " |" (zipmap ks ks)))
          (println (fmt-row "|-" "-+-" "-|" (zipmap ks spacers)))
          (doseq [row rows]
            (println (fmt-row "| " " | " " |" row)))))])
    ([rows] (print-table (keys (first rows)) rows)))


    ; (interpose ...) -> (interpose " - " ["one" "two" "three"]) => ("one" " - " "two" " - " "three")

    ; (zipmap ...) -> (zipmap [:a :b :c :d :e] [1 2 3 4 5]) => {:a 1, :b 2, :c 3, :d 4, :e 5}



  (defn sneak-peek-for-readme []
    [dt/datatable
     :songs
     [::subs/songs-list] ; <- re-frame subscription

     [{::dt/column-key   [:index]
       ::dt/sorting      {::dt/enabled? true}
       ::dt/column-label "#"}
      {::dt/column-key   [:name]
       ::dt/column-label "Name"}
      {::dt/column-key   [:duration]
       ::dt/column-label "Duration"
       ::dt/sorting      {::dt/enabled? true}
       ::dt/render-fn    (fn [val]
                           [:span
                            (let [m (quot val 60)
                                  s (mod val 60)]
                              (if (zero? m)
                                s
                                (str m ":" (when (< s 10) 0) s)))])}]
     {::dt/pagination    {::dt/enabled? true
                          ::dt/per-page 5}
      ::dt/table-classes ["ui" "table" "celled"]}])

  ())