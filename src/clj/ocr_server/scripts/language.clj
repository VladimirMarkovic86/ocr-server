(ns ocr-server.scripts.language
  (:require [mongo-lib.core :as mon]
            [common-middle.collection-names :refer [language-cname]]))

(defn insert-labels
  "Inserts labels"
  []
  (mon/mongodb-insert-many
    language-cname
    [{ :code 1001 :english "Working area" :serbian "Радионица" }
     { :code 1002 :english "Document" :serbian "Документ" }
     { :code 1003 :english "Name" :serbian "Назив" }
     { :code 1004 :english "Document type" :serbian "Тип документа" }
     { :code 1005 :english "Image" :serbian "Слика" }
     { :code 1006 :english "Learning" :serbian "Учење" }
     { :code 1007 :english "Reading" :serbian "Читање" }
     { :code 1008 :english "- Select -" :serbian "- Изабери -" }
     { :code 1009 :english "Light" :serbian "Светлост" }
     { :code 1010 :english "Contrast" :serbian "Контраст" }
     { :code 1011 :english "Space" :serbian "Размак" }
     { :code 1012 :english "Hooks" :serbian "Квачице" }
     { :code 1013 :english "Matching" :serbian "Поклапање" }
     { :code 1014 :english "Threads" :serbian "Нити" }
     { :code 1015 :english "Rows threads" :serbian "Нити редова" }
     { :code 1016 :english "Read" :serbian "Читај" }
     { :code 1017 :english "Process" :serbian "Процесуирај" }
     { :code 1018 :english "Save parameters" :serbian "Сачувај параметре" }
     { :code 1019 :english "Save sign" :serbian "Сачувај знак" }
     { :code 1020 :english "Unknown signs list" :serbian "Листа непознатих знакова" }
     { :code 1021 :english "Test document entity" :serbian "Тестирај ентитет документ" }
     { :code 1022 :english "Book page" :serbian "Страница књиге"}
     { :code 1023 :english "Typewriter" :serbian "Куцаћа машина"}
     ]))

