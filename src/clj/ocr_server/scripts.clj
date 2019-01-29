(ns ocr-server.scripts
  (:require [mongo-lib.core :as mon]
            [utils-lib.core :as utils]
            [common-middle.collection-names :refer [db-updates-cname
                                                    language-cname
                                                    role-cname
                                                    user-cname
                                                    preferences-cname]]
            [common-middle.role-names :refer [user-admin-rname
                                              user-mod-rname
                                              language-admin-rname
                                              language-mod-rname
                                              role-admin-rname
                                              role-mod-rname
                                              test-privileges-rname
                                              chat-rname]]
            [ocr-middle.role-names :refer [document-admin-rname
                                           document-mod-rname
                                           working-area-user-rname]]
            [common-middle.functionalities :as fns]
            [ocr-middle.functionalities :as omfns]))

(defn initialize-db
  "Initialize database"
  []
  (mon/mongodb-insert-many
    language-cname
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
     { :code 27, :english "Functionality", :serbian "Функционалност" }
     { :code 28, :english "Role name", :serbian "Назив улоге" }
     { :code 29, :english "Functionalities", :serbian "Функционалности" }
     { :code 30, :english "Roles", :serbian "Улоге" }
     { :code 31, :english "No entities", :serbian "Нема ентитета" }
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
     { :code 1020, :english "Unknown signs list", :serbian "Листа непознатих знакова" }])
  (mon/mongodb-insert-many
    role-cname
    [{:role-name user-admin-rname
      :functionalities [fns/user-create
                        fns/user-read
                        fns/user-update
                        fns/user-delete]}
     {:role-name user-mod-rname
      :functionalities [fns/user-read
                        fns/user-update]}
     {:role-name language-admin-rname
      :functionalities [fns/language-create
                        fns/language-read
                        fns/language-update
                        fns/language-delete]}
     {:role-name language-mod-rname
      :functionalities [fns/language-read
                        fns/language-update]}
     {:role-name role-admin-rname
      :functionalities [fns/role-create
                        fns/role-read
                        fns/role-update
                        fns/role-delete]}
     {:role-name role-mod-rname
      :functionalities [fns/role-read
                        fns/role-update]}
     {:role-name document-admin-rname
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
      :functionalities [omfns/test-document-entity]}])
  (let [user-admin-id (:_id
                        (mon/mongodb-find-one
                          role-cname
                          {:role-name user-admin-rname}))
        language-admin-id (:_id
                            (mon/mongodb-find-one
                              role-cname
                              {:role-name language-admin-rname}))
        role-admin-id (:_id
                        (mon/mongodb-find-one
                          role-cname
                          {:role-name role-admin-rname}))
        document-admin-id (:_id
                            (mon/mongodb-find-one
                              role-cname
                              {:role-name document-admin-rname}))
        working-area-user-id (:_id
                               (mon/mongodb-find-one
                                 role-cname
                                 {:role-name working-area-user-rname}))
        test-admin-id (:_id
                        (mon/mongodb-find-one
                          role-cname
                          {:role-name test-privileges-rname}))
        encrypted-password (utils/encrypt-password
                             (or (System/getenv "ADMIN_USER_PASSWORD")
                                 "123"))]
    (mon/mongodb-insert-one
      user-cname
      {:username "admin"
       :email "123@123"
       :password encrypted-password
       :roles [user-admin-id
               language-admin-id
               role-admin-id
               document-admin-id
               working-area-user-id
               test-admin-id]}))
  (let [user-id (:_id
                  (mon/mongodb-find-one
                    user-cname
                    {}))]
    (mon/mongodb-insert-one
      preferences-cname
      {:user-id user-id
       :language "serbian"
       :language-name "Srpski"}))
  (mon/mongodb-insert-one
    db-updates-cname
    {:initialized true
     :date (java.util.Date.)})  
 )

(defn db-update-1
  "Database update 1"
  []
  (mon/mongodb-insert-many
    language-cname
    [{ :code 1021, :english "Test document entity", :serbian "Тестирај ентитет документ" }])
  (mon/mongodb-insert-one
    db-updates-cname
    {:update 1
     :date (java.util.Date.)})
 )

(defn db-update-2
  "Database update 2"
  []
  (mon/mongodb-insert-many
    language-cname
    [{:code 1022, :english "Book page", :serbian "Страница књиге"}
     {:code 1023, :english "Typewriter", :serbian "Куцаћа машина"}
     ])
  (mon/mongodb-insert-one
    db-updates-cname
    {:update 1
     :date (java.util.Date.)})
 )

(defn db-update-3
  "Database update 3"
  []
  (mon/mongodb-insert-many
    language-cname
    [{:code 32, :english "Administration", :serbian "Администрација" }
     {:code 33, :english "- Select -", :serbian "- Одабери -" }
     {:code 34, :english "first", :serbian "прва" }
     {:code 35, :english "previous", :serbian "претходна" }
     {:code 36, :english "next", :serbian "следећа" }
     {:code 37, :english "last", :serbian "задња" }
     {:code 38, :english "Bad input", :serbian "Погрешан унос" }
     {:code 39, :english "Custom error", :serbian "Грешка" }
     {:code 40, :english "Pattern mismatch", :serbian "Не поклапање шаблона" }
     {:code 41, :english "Range overflow", :serbian "Прекорачење опсега" }
     {:code 42, :english "Range underflow", :serbian "Не достигнут опсег" }
     {:code 43, :english "Step mismatch", :serbian "Не поклапање корака" }
     {:code 44, :english "Too long", :serbian "Предуго" }
     {:code 45, :english "Too short", :serbian "Прекратко" }
     {:code 46, :english "Type mismatch", :serbian "Не поклапање типа" }
     {:code 47, :english "Value missing", :serbian "Недостатак вредности" }
     {:code 48,
      :english "Please fill out this field.",
      :serbian "Молим попуните ово поље." }
     {:code 49,
      :english "Please enter a number.",
      :serbian "Молим унесите број." }
     {:code 50,
      :english "Please select one of these options.",
      :serbian "Молим одаберите једну од ових опција." }
     {:code 51,
      :english "Please select an item in the list.",
      :serbian "Молим одаберите ставку у листи." }
     {:code 52,
      :english "Please enter an email address.",
      :serbian "Молим унесите адресу е-поште." }
     {:code 53,
      :english "Please use at least $0 characters (you are currently using $1 characters).",
      :serbian "Молим искористите бар $0 карактера (тренутно користите $1 карактера)." }
     {:code 54,
      :english "Please select a valid value. The two nearest valid values are $0 and $1.",
      :serbian "Молим одаберите исправну вредност. Две најближе исправне вредности су $0 и $1." }
     {:code 55,
      :english "Please select a value that is no less than $0.",
      :serbian "Молим одаберите вредност која није мања од $0." }
     {:code 56,
      :english "Please select a value that is no more than $0.",
      :serbian "Молим одаберите вредност која није већа од $0." }
     {:code 57,
      :english "Please match the requested format.",
      :serbian "Молим држите се захтеваног формата." }
     {:code 58,
      :english "Given email doesn't exist.",
      :serbian "Прослеђена адреса е-поште не постоји." }
     {:code 59,
      :english "Incorrect password for given email.",
      :serbian "Нетачна лозинка за прослеђену адресу е-поште." }
     {:code 60,
      :english "Confirm password does not match with password.",
      :serbian "Потврдна лозинка се не поклапа са лозинком." }
     {:code 61,
      :english "Username and/or email already exists.",
      :serbian "Корисничко име и/или адреса е-поште већ постоји." }
     {:code 62,
      :english "Sample App",
      :serbian "Sample App" }
     {:code 63,
      :english "Sample App is prototype project which can be used to quickly get head start in building your Web Apps.",
      :serbian "Sample App је прототип пројекат који може да се искористи за брзи почетак у изградњи ваше веб апликације." }
     {:code 64,
      :english "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character @$!%*?&..",
      :serbian "Лозинка мора да садржи барем једно велико слово, једно мало слово, један број и један специјални карактер @$!%*?&.." }
     {:code 65,
      :english "Please select a file.",
      :serbian "Молим одаберите фајл." }
     ])
  (mon/mongodb-insert-one
    db-updates-cname
    {:update 3
     :date (java.util.Date.)})
 )

(defn db-update-4
  "Database update 4"
  []
  (mon/mongodb-insert-many
    language-cname
    [{:code 66, :english "Type your message here", :serbian "Упишите вашу поруку овде" }
     {:code 67, :english "Send", :serbian "Пошаљи" }
     {:code 68, :english "Chat", :serbian "Ћаскање" }
		   {:code 69, :english "Refresh", :serbian "Освежи" }
     ])
		(mon/mongodb-insert-many
    role-cname
    [{:role-name chat-rname
      :functionalities [fns/chat]}
     ])
  (mon/mongodb-insert-one
    db-updates-cname
    {:update 4
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
    (when-not (mon/mongodb-exists
                db-updates-cname
                {:update 1})
      (db-update-1))
    (when-not (mon/mongodb-exists
                db-updates-cname
                {:update 2})
      (db-update-2))
    (when-not (mon/mongodb-exists
                db-updates-cname
                {:update 3})
      (db-update-3))
    (when-not (mon/mongodb-exists
                db-updates-cname
                {:update 4})
      (db-update-4))
    (catch Exception e
      (println e))
   ))

