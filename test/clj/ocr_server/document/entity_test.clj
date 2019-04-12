(ns ocr-server.document.entity-test
  (:require [clojure.test :refer :all]
            [ocr-server.document.entity :refer :all]
            [mongo-lib.core :as mon]))

(def db-uri
     (or (System/getenv "MONGODB_URI")
         (System/getenv "PROD_MONGODB")
         "mongodb://admin:passw0rd@127.0.0.1:27017/admin"))

(def db-name
     "test-db")

(defn create-db
  "Create database for testing"
  []
  (mon/mongodb-connect
    db-uri
    db-name)
  (mon/mongodb-insert-many
    "language"
    [{ :code 1022
       :english "Book page"
       :serbian "Страница из књиге" }
     { :code 1023
       :english "Typewriter"
       :serbian "Куцаћа машина" }
     ]))

(defn destroy-db
  "Destroy testing database"
  []
  (mon/mongodb-drop-database
    db-name)
  (mon/mongodb-disconnect))

(deftest test-format-dtype-field
  (testing "Test format dtype field"
    
    (create-db)
    
    (let [raw-dtype nil
          chosen-language nil
          result (format-dtype-field
                   raw-dtype
                   chosen-language)]
      
      (is
        (nil?
          result)
       )
      
     )
    
    (let [raw-dtype "unknown"
          chosen-language nil
          result (format-dtype-field
                   raw-dtype
                   chosen-language)]
      
      (is
        (= result
           "unknown")
       )
      
     )
    
    (let [raw-dtype "book_page"
          chosen-language nil
          result (format-dtype-field
                   raw-dtype
                   chosen-language)]
      
      (is
        (= result
           "Book page")
       )
      
     )
    
    (let [raw-dtype "typewriter"
          chosen-language "serbian"
          result (format-dtype-field
                   raw-dtype
                   chosen-language)]
      
      (is
        (= result
           "Куцаћа машина")
       )
      
     )
    
    (destroy-db)
    
   ))

