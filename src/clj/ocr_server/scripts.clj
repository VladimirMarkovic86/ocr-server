(ns ocr-server.scripts
  (:require [mongo-lib.core :as mon]
            [common-middle.collection-names :refer [db-updates-cname]]
            [common-server.scripts :as css]
            [ocr-server.scripts.language :as ossl]
            [ocr-server.scripts.role :as ossr]
            [ocr-server.scripts.user :as ossu]))

(defn initialize-db
  "Initialize database"
  []
  (css/initialize-db)
  (ossl/insert-labels)
  (ossr/insert-roles)
  (ossu/update-users)
  (mon/mongodb-insert-one
    db-updates-cname
    {:initialized true
     :date (java.util.Date.)})
 )

(defn initialize-db-if-needed
  "Check if database exists and initialize it if it doesn't"
  []
  (try
    (when-not (mon/mongodb-exists
                db-updates-cname
                {:initialized true})
      (initialize-db))
    (catch Exception e
      (println e))
   ))

