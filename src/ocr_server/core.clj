(ns ocr-server.core
 (:use [compojure.core :only [defroutes GET POST DELETE OPTIONS]]
       [clojure.data :only [diff]])
 (:require
      [clojure.java.io :refer [file]]
      [compojure.handler :as chandler]
      [compojure.route :as route]
      [ring.adapter.jetty :refer [run-jetty]]
      [ring.middleware.cors :refer [wrap-cors]]
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
  (println request-body)
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
 [request-body]
 (let [light-value (read-string (:light-value request-body))
       contrast-value (read-string (:contrast-value request-body))
       splitted-base64 (clojure.string/split (:image-src request-body) #"base64,")
       image-base64 (get splitted-base64 1)
       image-byte-array (.decode base64-decoder image-base64)
       image-is (ByteArrayInputStream. image-byte-array)
       image (ImageIO/read image-is)
       height (.getHeight image)
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
       new-image-base64 (str (get splitted-base64 0) "base64," new-image-base64)]
  {:status  (stc/ok)
   :headers {(eh/content-type) (mt/text-plain)}
   :body    (str {:status "success"
                  :src new-image-base64})}))

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
       matching-value (read-string (:matching-value request-body))]
  (try
   (mon/mongodb-update-by-id
     "document"
     _id
     {:light light-value
      :contrast contrast-value
      :space space-value
      :hooks hooks-value
      :matching matching-value})
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

(defn read-image
 "Read text from image"
 [request-body]
 (let [_id (:_id request-body)
       light-value (read-string (:light-value request-body))
       contrast-value (read-string (:contrast-value request-body))
       space-value (read-string (:space-value request-body))
       hooks-value (read-string (:hooks-value request-body))
       matching-value (read-string (:matching-value request-body))
       document-signs (get-document-signs _id)
       splitted-base64 (clojure.string/split (:image-src request-body) #"base64,")
       image-base64 (get splitted-base64 1)
       image-byte-array (.decode base64-decoder image-base64)
       signs-images (ocr/read-image-fn
                      image-byte-array
                      light-value
                      contrast-value
                      space-value
                      hooks-value
                      matching-value
                      document-signs)
       signs-images-atom (atom [])]
  (doseq [sign-image signs-images]
   (if (instance? BufferedImage
                  sign-image)
    (let [image-os (ByteArrayOutputStream.)
          debug (ImageIO/write sign-image "jpg" image-os)
          new-image-byte-array (.toByteArray image-os)
          new-image-base64 (.encodeToString base64-encoder new-image-byte-array)
          new-image-base64 (str (get splitted-base64 0) "base64," new-image-base64)]
     (swap!
       signs-images-atom
       conj
       new-image-base64))
     (swap!
       signs-images-atom
       conj
       sign-image))
   )
  {:status  (stc/ok)
   :headers {(eh/content-type) (mt/text-plain)}
   :body    (str {:status "success"
                  :images @signs-images-atom})}))

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
 (read-string (slurp (:body request))
  ))

(defroutes app-routes
 (POST "/login"
       request
       (println request)
       (login-authentication (parse-body request))
  )
 (POST "/am-i-logged-in"
       request
       (println request)
       (am-i-logged-in (get-cookie request "session"))
  )
 (POST "/get-entities"
       request
       (println request)
       (get-entities (parse-body request))
  )
 (POST "/get-entity"
       request
       (println request)
       (get-entity (parse-body request))
  )
 (POST "/update-entity"
       request
       (println request)
       (update-entity (parse-body request))
  )
 (POST "/insert-entity"
       request
       (println request)
       (insert-entity (parse-body request))
  )
 (DELETE "/delete-entity"
       request
       (println request)
       (delete-entity (parse-body request))
  )
 (POST "/process-image"
       request
       (println request)
       (process-image (parse-body request))
  )
 (POST "/read-image"
       request
       (println request)
       (read-image (parse-body request))
  )
 (POST "/save-sign"
       request
       (println request)
       (save-sign (parse-body request))
  )
 (POST "/save-parameters"
       request
       (println request)
       (save-parameters (parse-body request))
  )
 (route/resources "/")
 (route/not-found (not-found))
; (POST "*"
;  request
;  (println request)
;  (hello-world "hi"))
  )

(def handler (-> (chandler/site
                  (wrap-cors
                   app-routes
                   :access-control-allow-origin    [#"https://ocr:8451"
                                                    #"http://ocr:8453"
                                                    #"https://127.0.0.1:8451"
                                                    #"http://127.0.0.1:8453"
                                                    #"http://localhost:3449"
                                                    #"https://192.168.1.5:8451"]
                   :access-control-allow-methods   [:get :post :delete]
                   ;:access-control-allow-credentials  "true"
                   ))
               ))

(defonce server (atom nil))

(def db-name "ocr-db")

(defn start-server
  "Start server"
  []
  (if @server
      (let []
       (println "Server instance exists")
       (try
        (.start @server)
        (mon/mongodb-connect db-name)
        (catch Exception ex
               (println (.getMessage ex))
         ))
       )
      (let []
       (println "Server instance does not exist")
       (try
        (reset! server (run-jetty handler { :port 1606 :join? false}))
        (mon/mongodb-connect db-name)
        (catch Exception ex
               (println ex))
        ))
   ))

(defn stop-server
  "Stop server"
  []
  (if @server
   (let []
    (println "Server stopping")
    (try
     (.stop @server)
     (mon/mongodb-disconnect)
     (println "Server stopped")
     (catch Exception ex
            (println ex))
     ))
   (println "Server not initialized"))
  )

(defn atom-server-unset
  "Unset server from handler"
  []
  (reset! server nil))

(defn unset-restart-server
  "Stop server, unset server atom to nil
   reload project, start new server instance"
  []
  (stop-server)
  (atom-server-unset)
  (use 'ocr-server.core :reload)
  (start-server))

(defn -main [& args]
  (start-server))

