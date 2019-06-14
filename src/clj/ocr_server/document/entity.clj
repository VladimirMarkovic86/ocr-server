(ns ocr-server.document.entity
  (:require [language-lib.core :refer [get-label]]
            [ocr-middle.document.entity :as omde]
            [common-server.preferences :as prf]))

(defn format-dtype-field
  "Formats dtype field"
  [raw-dtype
   selected-language]
  (when (and raw-dtype
             (string?
               raw-dtype))
    (let [dtype-a (atom raw-dtype)]
      (when (= raw-dtype
               omde/dtype-book-page)
        (reset!
          dtype-a
          (get-label
            1022
            selected-language))
       )
      (when (= raw-dtype
               omde/dtype-typewriter)
        (reset!
          dtype-a
          (get-label
            1023
            selected-language))
       )
      @dtype-a))
 )

(defn reports
  "Returns reports projection"
  [request
   & [chosen-language]]
  (prf/set-preferences
    request)
  {:entity-label (get-label
                   1002
                   chosen-language)
   :projection [:dname
                :dtype
                ;:image
                ]
   :qsort {:dname 1}
   :rows (int
           (omde/calculate-rows))
   :table-rows (int
                 @omde/table-rows-a)
   :card-columns (int
                   @omde/card-columns-a)
   :labels {:dname (get-label
                     1003
                     chosen-language)
            :dtype (get-label
                     1004
                     chosen-language)
            :image (get-label
                     1005
                     chosen-language)
            }
   :columns {:dname {:width "70"
                     :header-background-color "lightblue"
                     :header-text-color "white"
                     :column-alignment "C"}
             :dtype {:width "70"
                     :header-background-color "lightblue"
                     :header-text-color "white"
                     :data-format-fn format-dtype-field
                     :column-alignment "C"}
             :image {:width "15"
                     :header-background-color "lightblue"
                     :header-text-color "white"
                     :column-alignment "C"}
             }
   })

