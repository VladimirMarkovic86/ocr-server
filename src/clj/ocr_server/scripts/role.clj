(ns ocr-server.scripts.role
  (:require [mongo-lib.core :as mon]
            [common-middle.collection-names :refer [role-cname]]
            [common-middle.role-names :refer [test-privileges-rname]]
            [ocr-middle.functionalities :as omfns]
            [ocr-middle.role-names :refer [document-admin-rname
                                           document-mod-rname
                                           working-area-user-rname]]))

(defn insert-roles
  "Inserts roles"
  []
  (mon/mongodb-insert-many
    role-cname
    [{:role-name document-admin-rname
      :functionalities [omfns/document-create
                        omfns/document-read
                        omfns/document-update
                        omfns/document-delete]}
     {:role-name document-mod-rname
      :functionalities [omfns/document-read
                        omfns/document-update]}
     {:role-name working-area-user-rname
      :functionalities [omfns/process-images
                        omfns/read-image
                        omfns/save-sign
                        omfns/save-parameters]}
     {:role-name test-privileges-rname
      :functionalities [omfns/test-document-entity]}]))

