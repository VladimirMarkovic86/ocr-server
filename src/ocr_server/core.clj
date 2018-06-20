(ns ocr-server.core
 (:require [server-lib.core :as srvr]
           [websocket-server-lib.core :as ws-srvr]
           [utils-lib.core :as utils]
           [mongo-lib.core :as mon]
           [ajax-lib.http.entity-header :as eh]
           [ajax-lib.http.response-header :as rsh]
           [ajax-lib.http.mime-type :as mt]
           [ajax-lib.http.status-code :as stc]
           [ocr-lib.core :as ocr])
 (:import [java.util Base64]
          [java.io ByteArrayInputStream
                   ByteArrayOutputStream]
          [java.awt.image BufferedImage]
          [javax.imageio ImageIO]))

(def db-name "ocr-db")

(def base64-decoder (Base64/Decoder/getDecoder))

(def base64-encoder (Base64/Encoder/getEncoder))

(defn random-uuid
  "Generate uuid"
  []
  (def uuid (.toString (java.util.UUID/randomUUID))
   )
  uuid)

(def users-map
  [{:email "markovic.vladimir86@gmail.com"
    :password "123"}
   {:email "123"
    :password "123"}])

(defn get-pass-for-email
  "Get password for supplied email"
  [itr
   entity-map
   result]
  (if (< itr (count users-map))
   (let [db-user        (nth users-map itr)
         same-email     (= (:email db-user) (:email entity-map))
         same-password  (= (:password db-user) (:password entity-map))]
        (if same-email
            (if same-password
                (swap! result conj {:status   "success"
                                    :email    "success"
                                    :password "success"})
                (swap! result conj {:email "success"}))
            (recur (inc itr) entity-map result))
    )
   @result))

(defn login-authentication
  "Login authentication"
  [entity-body]
  (let [result (get-pass-for-email 0
                                   entity-body 
                                   (atom {:status   "error"
                                          :email    "error"
                                          :password "error"}))]
   (if (= (:status result)
          "success")
       {:status  (stc/ok)
        :headers {(eh/content-type) (mt/text-plain)
                  (rsh/set-cookie)   (str "session=" (random-uuid) "; "
                                           "Expires=Wed, 30 Aug 2019 00:00:00 GMT; "
                                           "Path=/"
                                           ;"Domain=localhost:1612; "
                                           ;"Secure; "
                                           ;"HttpOnly"
                                           )}
        :body    (str result)}
       {:status  (stc/unauthorized)
        :headers {(eh/content-type) (mt/text-plain)}
        :body    (str result)})
   ))

; Expires=Wed, 30 Aug 2019 00:00:00 GMT
; Max-age=5000
; Domain=localhost:1612
; Path=/
; Secure
; HttpOnly
; SameSite=Strict
; SameSite=Lax

(defn am-i-logged-in
  "Check if user is logged in"
  [session-uuid]
  (if (= session-uuid
         uuid)
      {:status  (stc/ok)
       :headers {(eh/content-type) (mt/text-plain)}}
      {:status  (stc/unauthorized)
       :headers {(eh/content-type) (mt/text-plain)}}))

(defn get-cookie-by-name
  "Reurn cookie value by cookie name"
  [cookies
   cookie-name
   cookie-index]
  (if (< cookie-index (count cookies))
   (let [[cname value] (cookies cookie-index)]
    (if (= cookie-name
           cname)
     (:value value)
     (recur cookies cookie-name (inc cookie-index))
     ))
   nil))


(defn get-cookie
  "Read cookie from request"
  [request
   cookie-name]
  (get-cookie-by-name (into [] (:cookies request))
                      cookie-name
                      0))

(defn build-projection
  "Build projection for db interaction"
  [vector-fields
   include]
  (let [projection  (atom {})]
   (doseq [field vector-fields]
    (swap! projection assoc field include))
   @projection))

(defn get-entities
 "Prepare data for table"
 [request-body]
 (if (empty? (:entity-filter request-body))
  (if (:pagination request-body)
   (let [current-page     (:current-page request-body)
         rows             (:rows request-body)
         count-entities   (mon/mongodb-count
                           (:entity-type request-body)
                           (:entity-filter request-body))
         number-of-pages  (if (:pagination request-body)
                           (utils/round-up count-entities rows)
                           nil)
         current-page     (if (= current-page number-of-pages)
                           (dec current-page)
                           current-page)
         entity-type    (:entity-type request-body)
         entity-filter  (:entity-filter request-body)
         projection-vector  (:projection request-body)
         projection-include  (:projection-include request-body)
         projection     (build-projection projection-vector
                                          projection-include)
         qsort          (:qsort request-body)
         collation      (:collation request-body)
         final-result   (atom [])
         db-result      (mon/mongodb-find
                         entity-type
                         entity-filter
                         projection
                         qsort
                         rows
                         (* current-page
                            rows)
                         collation)]
    (if (not= -1 current-page)
     (doseq [single-result db-result]
      (let [ekeys  (if projection-include
                    projection-vector
                    (keys single-result))
            entity-as-vector  (atom [])]
       (swap! entity-as-vector conj (:_id single-result))
       (doseq [ekey ekeys]
        (swap! entity-as-vector conj (ekey single-result))
        )
       (swap! final-result conj @entity-as-vector))
      )
     nil)
    {:status  (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body    (str {:status  "success"
                    :data       @final-result
                    :pagination {:current-page     current-page
                                 :rows             rows
                                 :total-row-count  count-entities}
                    })})
   (let [entity-type    (:entity-type request-body)
         entity-filter  (:entity-filter request-body)
         projection-vector  (:projection request-body)
         projection-include  (:projection-include request-body)
         projection     (build-projection projection-vector
                                          projection-include)
         qsort          (:qsort request-body)
         collation      (:collation request-body)
         ;final-result   (atom [])
         db-result      (mon/mongodb-find
                         entity-type
                         entity-filter
                         projection
                         qsort)]
    {:status  (stc/ok)
     :headers {(eh/content-type) (mt/text-plain)}
     :body    (str {:status "success"
                    :data db-result})}
    )
   )
  
  {:status  (stc/bad-request)
   :headers {(eh/content-type) (mt/text-plain)}
   :body    (str {:status  "error"
                  :error-message "404 Bad request"})}))

(defn get-entity
 "Prepare requested entity for response"
 [request-body]
 (let [entity  (mon/mongodb-find-by-id (:entity-type request-body)
                                       (:_id (:entity-filter request-body))
                )
       entity  (assoc entity :_id (str (:_id entity))
                )]
  (if entity
   {:status (stc/ok)
    :headers {(eh/content-type) (mt/text-plain)}
    :body   (str {:status  "success"
                  :data  entity})}
   {:status (stc/not-found)
    :headers {(eh/content-type) (mt/text-plain)}
    :body   (str {:status  "error"
                  :error-message "There is no entity, for given criteria."})}))
 )

(defn update-entity
 "Update entity"
 [request-body]
 (try
  (mon/mongodb-update-by-id (:entity-type request-body)
                            (:_id request-body)
                            (:entity request-body))
  {:status  (stc/ok)
   :headers {(eh/content-type) (mt/text-plain)}
   :body    (str {:status "success"})}
  (catch Exception ex
   (println (.getMessage ex))
   {:status  (stc/internal-server-error)
    :headers {(eh/content-type) (mt/text-plain)}
    :body    (str {:status "error"})}))
 )

(defn insert-entity
 "Insert entity"
 [request-body]
 (try
  (mon/mongodb-insert-one (:entity-type request-body)
                          (:entity request-body))
  {:status  (stc/ok)
   :headers {(eh/content-type) (mt/text-plain)}
   :body    (str {:status "success"})}
  (catch Exception ex
   (println (.getMessage ex))
   {:status  (stc/internal-server-error)
    :headers {(eh/content-type) (mt/text-plain)}
    :body    (str {:status "error"})}))
 )

(defn delete-entity
 "Delete entity"
 [request-body]
 (try
  (mon/mongodb-delete-by-id (:entity-type request-body)
                            (:_id (:entity-filter request-body))
   )
  {:status  (stc/ok)
   :headers {(eh/content-type) (mt/text-plain)}
   :body    (str {:status "success"})}
  (catch Exception ex
   (println (.getMessage ex))
   {:status  (stc/internal-server-error)
    :headers {(eh/content-type) (mt/text-plain)}
    :body    (str {:status "error"})}))
 )

(defn process-image
 "Process image with parameters from front end"
 [light-value
  contrast-value
  image
  image-mime-type]
 (let [height (.getHeight image)
       width (.getWidth image)
       image (ocr/grayscale-contrast-fn
               image
               width
               height
               light-value
               contrast-value)
       image-os (ByteArrayOutputStream.)
       debug (ImageIO/write image "jpg" image-os)
       new-image-byte-array (.toByteArray image-os)
       new-image-base64 (.encodeToString base64-encoder new-image-byte-array)
       new-image-base64 (str image-mime-type "base64," new-image-base64)]
   new-image-base64))

(defn process-images
 "Process image with parameters from front end"
 [request-body]
 ;PROGRESS BAR
 (ocr/process-images-reset-progress-value-fn)
 (let [light-value (read-string (:light-value request-body))
       contrast-value (read-string (:contrast-value request-body))
       image-srcs (:image-srcs request-body)
       images (atom [])
       new-images-base64 (atom [])]
  (doseq [image-src image-srcs]
   (let [splitted-base64 (clojure.string/split image-src #"base64,")
         image-base64 (get splitted-base64 1)
         image (ocr/read-base64-image-fn
                 image-base64)
         height (.getHeight image)
         width (.getWidth image)]
    ;PROGRESS BAR
    (ocr/increase-total-pixels
      width
      height)
    (swap!
      images
      conj
      [image
       (get splitted-base64 0)]))
   )
  (doseq [[image
           image-mime-type] @images]
   (swap!
     new-images-base64
     conj
     (process-image
       light-value
       contrast-value
       image
       image-mime-type))
   )
  ;PROGRESS BAR
  (ocr/process-images-reset-progress-value-fn)
  {:status  (stc/ok)
   :headers {(eh/content-type) (mt/text-plain)}
   :body    (str {:status "success"
                  :srcs @new-images-base64})}))

(defn get-document-signs
 "Get signs from particular document"
 [_id]
 (let [signs (:signs (mon/mongodb-find-by-id
                       "document"
                       _id))
       decoded-signs (atom {})]
  (doseq [sign signs]
   (let [sign-value (:value sign)
         map-elem ((keyword sign-value) @decoded-signs)
         splitted-base64 (clojure.string/split (:image sign) #"base64,")
         image-base64 (get splitted-base64 1)
         map-elem (if (vector? map-elem)
                   (conj
                     map-elem
                     (.decode base64-decoder image-base64))
                   [(.decode base64-decoder image-base64)])]
    (swap!
     decoded-signs
     assoc
     (keyword sign-value)
     map-elem))
   )
  @decoded-signs))

(defn save-parameters
 "Save parameters for reading calibration"
 [request-body]
 (let [_id (:_id request-body)
       light-value (read-string (:light-value request-body))
       contrast-value (read-string (:contrast-value request-body))
       space-value (read-string (:space-value request-body))
       hooks-value (read-string (:hooks-value request-body))
       matching-value (read-string (:matching-value request-body))
       threads-value (read-string (:threads-value request-body))
       rows-threads-value (read-string (:rows-threads-value request-body))]
  (try
   (mon/mongodb-update-by-id
     "document"
     _id
     {:light light-value
      :contrast contrast-value
      :space space-value
      :hooks hooks-value
      :matching matching-value
      :threads threads-value
      :rows-threads rows-threads-value})
   {:status  (stc/ok)
    :headers {(eh/content-type) (mt/text-plain)}
    :body    (str {:status "success"})}
   (catch Exception ex
     (println ex)
     {:status  (stc/internal-server-error)
      :headers {(eh/content-type) (mt/text-plain)}
      :body    (str {:status  "error"
                     :error-message (.getMessage ex)})}))
  ))

(defn read-image-ws
 ""
 [websocket]
 (let [{websocket-message :websocket-message
        websocket-output-fn :websocket-output-fn} websocket
       request-body (read-string websocket-message)
       _id (:_id request-body)
       light-value (read-string (:light-value request-body))
       contrast-value (read-string (:contrast-value request-body))
       space-value (read-string (:space-value request-body))
       hooks-value (read-string (:hooks-value request-body))
       matching-value (read-string (:matching-value request-body))
       threads-value (read-string (:threads-value request-body))
       rows-threads-value (read-string (:rows-threads-value request-body))
       document-signs (get-document-signs _id)
       splitted-base64 (clojure.string/split (:image-src request-body) #"base64,")
       image-base64 (get splitted-base64 1)
       image-byte-array (.decode base64-decoder image-base64)
       [read-text
        unknown-signs-images] (ocr/read-image-fn
                                image-byte-array
                                light-value
                                contrast-value
                                space-value
                                hooks-value
                                matching-value
                                threads-value
                                rows-threads-value
                                document-signs)
       unknown-signs-images-atom (atom [])]
   (doseq [sign-image unknown-signs-images]
     (let [image-os (ByteArrayOutputStream.)
           debug (ImageIO/write sign-image "jpg" image-os)
           new-image-byte-array (.toByteArray image-os)
           new-image-base64 (.encodeToString base64-encoder new-image-byte-array)
           new-image-base64 (str (get splitted-base64 0) "base64," new-image-base64)]
       (swap!
         unknown-signs-images-atom
         conj
         new-image-base64))
    )
   (websocket-output-fn
     (str
       {:status "success"
        :action "read-image"
        :images @unknown-signs-images-atom
        :read-text read-text}))
   (websocket-output-fn
     (str
       {:status "close"})
     -120)
  ))

(defn save-sign
 "Save sign with document"
 [{entity-type :entity-type
   {_id :_id} :entity-filter
   sign-value :sign-value
   sign-image :sign-image}]
 (let [document (mon/mongodb-find-by-id entity-type _id)
       signs (:signs document)
       signs (if (nil? signs)
                [{:value sign-value
                  :image sign-image}]
                (conj signs {:value sign-value
                             :image sign-image}))]
  (mon/mongodb-update-by-id
    entity-type
    _id
    {:signs signs})
  {:status  (stc/ok)
   :headers {(eh/content-type) (mt/text-plain)}
   :body    (str {:status "success"})}))

(defn not-found
 "Requested action not found"
 []
 {:status  (stc/not-found)
  :headers {(eh/content-type) (mt/text-plain)}
  :body    (str {:status  "error"
                 :error-message "404 not found"})})

(defn parse-body
 "Read entity-body from request, convert from string to clojure data"
 [request]
 (read-string (:body request)))

(defn routing
 "Routing function"
 [request-start-line
  request]
 (println
   (str
     "\n"
     (dissoc
       request
       :body
       :websocket))
  )
 (case request-start-line
   "POST /login" (login-authentication (parse-body request))
   "POST /am-i-logged-in" (am-i-logged-in (get-cookie request "session"))
   "POST /get-entities" (get-entities (parse-body request))
   "POST /get-entity" (get-entity (parse-body request))
   "POST /update-entity" (update-entity (parse-body request))
   "POST /insert-entity" (insert-entity (parse-body request))
   "DELETE /delete-entity" (delete-entity (parse-body request))
   "POST /process-images" (process-images (parse-body request))
   "ws GET /read-image" (read-image-ws (:websocket request))
   "POST /save-sign" (save-sign (parse-body request))
   "POST /save-parameters" (save-parameters (parse-body request))
   {:status 404
    :headers {(eh/content-type) (mt/text-plain)}
    :body (str {:status  "success"})}))

(defn start-server
 "Start server"
 []
 (try
   (srvr/start-server
     routing
     {(rsh/access-control-allow-origin) #{"https://ocr:8451"
                                          "http://ocr:8453"
                                          "https://127.0.0.1:8451"
                                          "http://127.0.0.1:8453"
                                          "http://localhost:3449"
                                          "https://192.168.1.5:8451"}
      (rsh/access-control-allow-methods) "GET, POST, DELETE, PUT"}
     1606)
   (ws-srvr/start-server
     routing
     1607)
   (mon/mongodb-connect db-name)
   (catch Exception e
     (println (.getMessage e))
    ))
 )

(defn stop-server
 "Stop server"
 []
 (try
   (srvr/stop-server)
   (ws-srvr/stop-server)
   (mon/mongodb-disconnect)
   (catch Exception e
     (println (.getMessage e))
    ))
 )

(defn unset-restart-server
 "Stop server, unset server atom to nil
  reload project, start new server instance"
 []
 (stop-server)
 (use 'ocr-server.core :reload)
 (start-server))

(defn -main [& args]
  (start-server))

