(ns ocr-server.scripts
  (:require [mongo-lib.core :as mon]
            [utils-lib.core :as utils]))

(defn initialize-db
  ""
  []
  (mon/mongodb-insert-many
    "language"
    [{ :code 1, :english "Save", :serbian "Сачувај" }
     { :code 2, :english "Log out", :serbian "Одјави се" }
     { :code 3, :english "Home", :serbian "Почетна" }
     { :code 4, :english "Create", :serbian "Креирај" }
     { :code 5, :english "Show all", :serbian "Прикажи све" }
     { :code 6, :english "Details", :serbian "Детаљи" }
     { :code 7, :english "Edit", :serbian "Измени" }
     { :code 8, :english "Delete", :serbian "Обриши" }
     { :code 9, :english "Actions", :serbian "Акције" }
     { :code 10, :english "Insert", :serbian "Упиши" }
     { :code 11, :english "Update", :serbian "Ажурирај" }
     { :code 12, :english "Cancel", :serbian "Откажи" }
     { :code 13, :english "Search", :serbian "Претрага" }
     { :code 14, :english "E-mail", :serbian "Е-пошта" }
     { :code 15, :english "Password", :serbian "Лозинка" }
     { :code 16, :english "Remember me", :serbian "Упамти ме" }
     { :code 17, :english "Log in", :serbian "Пријави се" }
     { :code 18, :english "Sign up", :serbian "Направи налог" }
     { :code 19, :english "Username", :serbian "Корисничко име" }
     { :code 20, :english "Confirm password", :serbian "Потврди лозинку" }
     { :code 21, :english "User", :serbian "Корисник" }
     { :code 22, :english "Role", :serbian "Улога" }
     { :code 23, :english "Language", :serbian "Језик" }
     { :code 24, :english "Label code", :serbian "Код лабеле" }
     { :code 25, :english "English", :serbian "Енглески" }
     { :code 26, :english "Serbian", :serbian "Српски" }
     { :code 1001, :english "Working area", :serbian "Радионица" }
     { :code 1002, :english "Document", :serbian "Документ" }
     { :code 1003, :english "Name", :serbian "Назив" }
     { :code 1004, :english "Document type", :serbian "Тип документа" }
     { :code 1005, :english "Image", :serbian "Слика" }
     { :code 1006, :english "Learning", :serbian "Учење" }
     { :code 1007, :english "Reading", :serbian "Читање" }
     { :code 1008, :english "- Select -", :serbian "- Изабери -" }
     { :code 1009, :english "Light", :serbian "Светлост" }
     { :code 1010, :english "Contrast", :serbian "Контраст" }
     { :code 1011, :english "Space", :serbian "Размак" }
     { :code 1012, :english "Hooks", :serbian "Квачице" }
     { :code 1013, :english "Matching", :serbian "Поклапање" }
     { :code 1014, :english "Threads", :serbian "Нити" }
     { :code 1015, :english "Rows threads", :serbian "Нити редова" }
     { :code 1016, :english "Read", :serbian "Читај" }
     { :code 1017, :english "Process", :serbian "Процесуирај" }
     { :code 1018, :english "Save parameters", :serbian "Сачувај параметре" }
     { :code 1019, :english "Save sign", :serbian "Сачувај знак" }
     { :code 27, :english "Functionality", :serbian "Функционалност" }
     { :code 28, :english "Role name", :serbian "Назив улоге" }
     { :code 29, :english "Functionalities", :serbian "Функционалности" }
     { :code 30, :english "Roles", :serbian "Улоге" }
     { :code 31, :english "No entities", :serbian "Нема ентитета" }])
  (mon/mongodb-insert-many
    "role"
    [{ :role-name "User administrator", :functionalities [ "user-create", "user-read", "user-update", "user-delete" ] }
     { :role-name "User moderator", :functionalities [ "user-read", "user-update" ] }
     { :role-name "Language administrator", :functionalities [ "language-create", "language-read", "language-update", "language-delete" ] }
     { :role-name "Language moderator", :functionalities [ "language-read", "language-update" ] }
     { :role-name "Role administrator", :functionalities [ "role-create", "role-read", "role-update", "role-delete" ] }
     { :role-name "Role moderator", :functionalities [ "role-read", "role-update" ] }
     { :role-name "Document administrator", :functionalities [ "document-create", "document-read", "document-update", "document-delete" ] }
     { :role-name "Document moderator", :functionalities [ "document-read", "document-update" ] }
     { :role-name "Working area user", :functionalities [ "process-images", "read-image", "save-sign", "save-parameters" ] }
     { :role-name "Role moderator 2", :functionalities [ "role-read" ] }])
  (let [user-admin-id (:_id
                        (mon/mongodb-find-one
                          "role"
                          {:role-name "User administrator"}))
        language-admin-id (:_id
                            (mon/mongodb-find-one
                              "role"
                              {:role-name "Language administrator"}))
        role-admin-id (:_id
                        (mon/mongodb-find-one
                          "role"
                          {:role-name "Role administrator"}))
        document-admin-id (:_id
                            (mon/mongodb-find-one
                              "role"
                              {:role-name "Document administrator"}))
        working-area-user-id (:_id
                               (mon/mongodb-find-one
                                 "role"
                                 {:role-name "Working area user"}))
        encrypted-password (utils/encrypt-password
                             (or (System/getenv "ADMIN_USER_PASSWORD")
                                 "123"))]
    (mon/mongodb-insert-one
      "user"
      {:username "admin"
       :email "123@123"
       :password encrypted-password
       :roles [user-admin-id language-admin-id role-admin-id document-admin-id working-area-user-id]}))
  (let [user-id (:_id
                  (mon/mongodb-find-one
                    "user"
                    {}))]
    (mon/mongodb-insert-one
      "preferences"
      {:user-id user-id, :language "serbian", :language-name "Srpski" }))
 )

(defn initialize-db-if-needed
  ""
  []
  (try
    (when-not (mon/mongodb-exists
                "language"
                {:english "Save"})
      (initialize-db))
    (catch Exception e
      (println e))
   ))

