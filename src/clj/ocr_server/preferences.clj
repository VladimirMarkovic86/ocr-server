(ns ocr-server.preferences
  (:require [ocr-middle.document.entity :as omde]))

(defn set-specific-preferences-fn
  "Sets preferences on server side"
  [specific-map]
  (let [{{{table-rows-d :table-rows
           card-columns-d :card-columns} :document-entity} :display} specific-map]
    (reset!
      omde/table-rows-a
      (or table-rows-d
          10))
    (reset!
      omde/card-columns-a
      (or card-columns-d
          0))
   ))

