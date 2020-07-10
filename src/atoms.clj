(ns atoms)


(def widgets [
              {:name        :spectrum-area-widget
               :basis       :chart
               :type        :area-chart
               :data-source :spectrum-traces
               :options     {:viz/title             "Channels (area)"
                             :viz/allowDecimals     false
                             :viz/x-title           "frequency"
                             :viz/y-title           "power"
                             :viz/banner-color      "blue"
                             :viz/banner-text-color "white"
                             :viz/style-name        "widget"
                             :viz/animation         false
                             :viz/tooltip           {:followPointer true}}}])

(def widget-layout
  {
   :spectrum-area-widget      {:layout-opts
                               {:position {:lg {:x 0 :y 7 :w 2 :h 3}
                                           :md {:x 0 :y 7 :w 2 :h 3}
                                           :sm {:x 0 :y 7 :w 2 :h 3 :static true}}}}})


(def widget-atom (atom (mapv #(merge % (get widget-layout (:name %))) widgets)))
(def default-layout {:layout-opts
                     {:position {:lg {:x 0 :y 0 :w 2 :h 2}
                                 :md {:x 0 :y 0 :w 2 :h 2}
                                 :sm {:x 0 :y 0 :w 2 :h 2 :static true}}}})

(def dashboard {:layout  :responsive-grid-layout
                :options {:layout-opts {:cols {:lg 6 :md 4 :sm 2 :xs 1 :xxs 1}}}
                :widgets widget-atom})

(defn add-to-widget-atom [widget]
  (prn (str "add-to-widget-atom " widget
         ", merged " (merge default-layout widget)
         ",  after conj " (conj @widget-atom (merge default-layout widget))))

  (swap! widget-atom conj (merge widget default-layout)))


(add-to-widget-atom {:name :bubble-widget,
                     :basis :chart,
                     :type :bubble-chart,
                     :data-source :bubble-service,
                     :options {:viz/title "Bubble",
                               :viz/banner-color "darkgreen",
                               :viz/banner-text-color "white",
                               :viz/dataLabels true,
                               :viz/labelFormat "{point.name}",
                               :viz/lineWidth 0,
                               :viz/animation false,
                               :viz/data-labels true}})

@widget-atom
dashboard

(comment
  {:layout-opts {:position {:lg {:x 0, :y 0, :w 2, :h 2},
                            :md {:x 0, :y 0, :w 2, :h 2},
                            :sm {:x 0, :y 0, :w 2, :h 2, :static true}}},
   :name        :bubble-widget,
   :basis       :chart,
   :type        :bubble-chart,
   :data-source :bubble-service,
   :options     {:viz/title             "Bubble",
                 :viz/banner-color      "darkgreen",
                 :viz/banner-text-color "white",
                 :viz/dataLabels        true,
                 :viz/labelFormat       "{point.name}",
                 :viz/lineWidth         0,
                 :viz/animation         false,
                 :viz/data-labels       true}}


  [
   {:name :spectrum-area-widget,
    :basis :chart,
    :type :area-chart,
    :data-source :spectrum-traces,
    :options {:viz/style-name "widget",
              :viz/y-title "power",
              :viz/x-title "frequency",
              :viz/allowDecimals false,
              :viz/banner-color "blue",
              :viz/tooltip {:followPointer true},
              :viz/title "Channels (area)",
              :viz/banner-text-color "white",
              :viz/animation false},
    :layout-opts {:position {:lg {:x 0, :y 7, :w 2, :h 3},
                             :md {:x 0, :y 7, :w 2, :h 3},
                             :sm {:x 0, :y 7, :w 2, :h 3, :static true}}}}

   {:layout-opts {:position {:lg {:x 0, :y 0, :w 2, :h 2},
                             :md {:x 0, :y 0, :w 2, :h 2},
                             :sm {:x 0, :y 0, :w 2, :h 2, :static true}}},
    :name :bubble-widget,
    :basis :chart,
    :type :bubble-chart,
    :data-source :bubble-service,
    :options {:viz/title "Bubble",
              :viz/banner-color "darkgreen",
              :viz/banner-text-color "white",
              :viz/dataLabels true,
              :viz/labelFormat "{point.name}",
              :viz/lineWidth 0,
              :viz/animation false,
              :viz/data-labels true}}]



  ())

