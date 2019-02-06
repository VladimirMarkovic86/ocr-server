(ns ocr-server.scripts.user
  (:require [mongo-lib.core :as mon]
            [common-middle.collection-names :refer [user-cname
                                                    role-cname]]
            [common-middle.role-names :refer [test-privileges-rname]]
            [ocr-middle.role-names :refer [document-admin-rname
                                           working-area-user-rname]]))

(defn update-users
  "Updates users"
  []
  (let [document-admin-id (:_id
                            (mon/mongodb-find-one
                              role-cname
                              {:role-name document-admin-rname}))
        working-area-user-id (:_id
                               (mon/mongodb-find-one
                                 role-cname
                                 {:role-name working-area-user-rname}))
        test-privileges-id (:_id
                             (mon/mongodb-find-one
                               role-cname
                               {:role-name test-privileges-rname}))
        ocr-admin-roles [document-admin-id
                         working-area-user-id
                         test-privileges-id]
        ocr-guest-roles [document-admin-id
                         working-area-user-id]]
    (mon/mongodb-update-one
      user-cname
      {:username "admin"}
      {:$addToSet
        {:roles
          {:$each ocr-admin-roles}}
       })
    (mon/mongodb-update-one
      user-cname
      {:username "guest"}
      {:$addToSet
        {:roles
          {:$each ocr-guest-roles}}
       }))
 )

