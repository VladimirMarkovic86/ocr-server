(ns ocr-server.config
  (:require [ajax-lib.http.response-header :as rsh]
            [server-lib.core :as srvr]
            [ocr-server.document.entity :as documente]
            [common-server.core :as rt]
            [common-server.preferences :as prf]
            [ocr-server.preferences :as osprf]
            [common-middle.role-names :refer [chat-rname
                                              reports-rname]]
            [ocr-middle.role-names :refer [document-admin-rname
                                           working-area-user-rname]]
            [utils-lib.core-clj :as utilsclj]
            [pdflatex-lib.core :as tex]))

(def db-uri
     (or (System/getenv "MONGODB_URI")
         (System/getenv "PROD_MONGODB")
         "mongodb://admin:passw0rd@127.0.0.1:27017/admin"))

(def db-name
     "ocr-db")

(def project-absolute-path
     "/home/vladimir/workspace/clojure/projects/ocr_server")

(defn define-port
  "Defines server's port"
  []
  (let [port (System/getenv "PORT")
        port (if port
               (read-string
                 port)
               1602)]
    port))

(defn build-access-control-map
  "Build access control map"
  []
  (let [access-control-allow-origin #{"https://ocr:8451"
                                      "https://ocr:1612"
                                      "http://ocr:1612"
                                      "https://ocr:1602"
                                      "http://ocr:1602"
                                      "https://192.168.1.86:1612"
                                      "http://192.168.1.86:1612"
                                      "https://192.168.1.86:1602"
                                      "http://192.168.1.86:1602"
                                      "http://ocr:8453"}
        access-control-allow-origin (if (System/getenv "CLIENT_ORIGIN")
                                      (conj
                                        access-control-allow-origin
                                        (System/getenv "CLIENT_ORIGIN"))
                                      access-control-allow-origin)
        access-control-allow-origin (if (System/getenv "SERVER_ORIGIN")
                                      (conj
                                        access-control-allow-origin
                                        (System/getenv "SERVER_ORIGIN"))
                                      access-control-allow-origin)
        access-control-map {(rsh/access-control-allow-origin) access-control-allow-origin
                            (rsh/access-control-allow-methods) "OPTIONS, GET, POST, DELETE, PUT"
                            (rsh/access-control-allow-credentials) true
                            (rsh/access-control-allow-headers) "Content-Type"
                            (rsh/access-control-expose-headers) (rsh/set-visible-cookie)}]
    access-control-map))

(defn build-certificates-map
  "Build certificates map"
  []
  (let [certificates {:keystore-file-path
                       "certificate/ocr_server.jks"
                      :keystore-password
                       "ultras12"}
        certificates (when-not (System/getenv "CERTIFICATES")
                       certificates)]
    certificates))

(defn set-thread-pool-size
  "Set thread pool size"
  []
  (let [core-pool-size (System/getenv "CORE_POOL_SIZE")
        maximum-pool-size (System/getenv "MAXIMUM_POOL_SIZE")
        array-blocking-queue-size (System/getenv "ARRAY_BLOCKING_QUEUE_SIZE")]
    (when core-pool-size
      (reset!
        srvr/core-pool-size
        (read-string
          core-pool-size))
     )
    (when maximum-pool-size
      (reset!
        srvr/maximum-pool-size
        (read-string
          maximum-pool-size))
     )
    (when array-blocking-queue-size
      (reset!
        srvr/maximum-pool-size
        (read-string
          array-blocking-queue-size))
     ))
 )

(def audit-action-a
     (atom false))

(defn set-audit
  "Sets audit from AUDIT_ACTIONS environment variable"
  []
  (let [audit-actions (System/getenv "AUDIT_ACTIONS")
        audit-actions (when audit-actions
                        (let [audit-actions (read-string
                                              audit-actions)]
                          (when (instance?
                                  Boolean
                                  audit-actions)
                            audit-actions))
                       )]
    (reset!
      audit-action-a
      audit-actions))
 )

(defn add-custom-entities-to-entities-map
  "Adds custom entities for this project into entities map from common-server"
  []
  (swap!
    rt/entities-map
    assoc
    :document {:reports documente/reports}))

(defn set-report-paths
  "Sets report paths"
  []
  (let [absolute-path (:out (utilsclj/execute-shell-command
                              "pwd"))
        path-prefix (if (= absolute-path
                           project-absolute-path)
                      ""
                      (str
                        project-absolute-path
                        "/"))]
    (reset!
      tex/reports-templates-path
      (or (System/getenv
            "REPORTS_TEMPLATES_PATH")
          (str
            path-prefix
            @tex/reports-templates-path))
     )
    (reset!
      tex/reports-generated-path
      (or (System/getenv
            "REPORTS_GENERATED_PATH")
          (str
            path-prefix
            @tex/reports-generated-path))
     ))
 )

(defn read-sign-up-roles
  "Reads and fills sign up roles vector in common-server.core namespace"
  []
  (let [role-names [chat-rname
                    reports-rname
                    document-admin-rname
                    working-area-user-rname]]
    (rt/read-sign-up-role-ids
      role-names))
 )

(defn setup-e-mail-account
  "Sets up email account with email address and password"
  []
  (let [email-address (or (System/getenv
                            "NO_REPLY_EMAIL_ADDRESS")
                          "markovic.vladimir86.no.reply@gmail.com")
        email-password (or (System/getenv
                             "NO_REPLY_EMAIL_PASSWORD")
                           "secret")]
    (reset!
      rt/email-address
      email-address)
    (reset!
      rt/email-password
      email-password))
 )

(defn setup-e-mail-templates-path
  "Sets up email templates path"
  []
  (let [reset-password-mail-template-path
         (or (System/getenv
               "RESET_PASSWORD_EMAIL_TEMPLATE_PATH")
             "resources/mails/reset_password_template.html")]
    (reset!
      rt/reset-password-mail-template-path
      reset-password-mail-template-path))
 )

(defn bind-set-specific-preferences-fn
  "Binds set specific preferences to common server atom"
  []
  (reset!
    prf/set-specific-preferences-a-fn
    osprf/set-specific-preferences-fn))

