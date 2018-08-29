(ns ocr-server.core
  (:require [session-lib.core :as ssn]
            [server-lib.core :as srvr]
            [websocket-server-lib.core :as ws-srvr]
            [utils-lib.core :as utils]
            [mongo-lib.core :as mon]
            [dao-lib.core :as dao]
            [language-lib.core :as lang]
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

(def db-name
     "ocr-db")

(def base64-decoder
     (Base64/Decoder/getDecoder))

(def base64-encoder
     (Base64/Encoder/getEncoder))

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

(defn process-images-ws
  "Process image with parameters from front end"
  [websocket]
  ;PROGRESS BAR
  (ocr/process-images-reset-progress-value-fn)
  (let [{websocket-message :websocket-message
         websocket-output-fn :websocket-output-fn} websocket
        request-body (read-string
                       websocket-message)
        light-value (read-string
                      (:light-value request-body))
        contrast-value (read-string
                         (:contrast-value request-body))
        image-srcs (:image-srcs request-body)
        images (atom [])
        new-images-base64 (atom [])
        update-progress-thread
          (future
            (let [progress-value (atom
                                   (ocr/process-images-calculate-progress-value-fn))]
              (while (< @progress-value
                        100)
                (reset!
                  progress-value
                  (ocr/process-images-calculate-progress-value-fn))
                (websocket-output-fn
                  (str
                    {:action "update-progress"
                     :progress-value @progress-value}))
                (Thread/sleep 500))
              ;PROGRESS BAR
              (ocr/process-images-reset-progress-value-fn))
           )]
    (doseq [image-src image-srcs]
      (let [splitted-base64 (clojure.string/split
                              image-src
                              #"base64,")
            image-base64 (get
                           splitted-base64
                           1)
            image (ocr/read-base64-image-fn
                    image-base64)
            height (.getHeight
                     image)
            width (.getWidth
                    image)]
        ;PROGRESS BAR
        (ocr/increase-total-pixels
          width
          height)
        (swap!
          images
          conj
          [image
           (get
             splitted-base64
             0)])
       ))
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
    (future-cancel
      update-progress-thread)
    (websocket-output-fn
      (str
        {:action "update-progress"
         :progress-value 100}))
    (websocket-output-fn
      (str
        {:action "image-processed"
         :srcs @new-images-base64}))
    (websocket-output-fn
      (str
        {:status "close"})
      -120))
 )

(defn get-document-signs
  "Get signs from particular document"
  [_id]
  (let [signs (:signs (mon/mongodb-find-by-id
                        "document"
                        _id))
        decoded-signs (atom {})]
   (doseq [sign signs]
     (let [sign-value (:value sign)
           map-elem ((keyword
                       sign-value)
                     @decoded-signs)
           splitted-base64 (clojure.string/split
                             (:image sign)
                             #"base64,")
           image-base64 (get
                          splitted-base64
                          1)
           map-elem (if (vector? map-elem)
                      (conj
                        map-elem
                        (.decode
                          base64-decoder
                          image-base64))
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
                       :error-message (.getMessage ex)})}
      ))
   ))

(defn read-image-ws
  "Read image sent through websocket
   and return results back to client"
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
     :body    (str {:status "success"})})
 )

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
  (read-string
    (:body request))
 )

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
  (if (ssn/am-i-logged-in-fn request)
    (let [[cookie-key
           cookie-value] (ssn/refresh-session
                           request)
          response
           (case request-start-line
             "POST /am-i-logged-in" (ssn/am-i-logged-in request)
             "POST /get-entities" (dao/get-entities (parse-body request))
             "POST /get-entity" (dao/get-entity (parse-body request))
             "POST /update-entity" (dao/update-entity (parse-body request))
             "POST /insert-entity" (dao/insert-entity (parse-body request))
             "DELETE /delete-entity" (dao/delete-entity (parse-body request))
             "ws GET /process-images" (process-images-ws (:websocket request))
             "ws GET /read-image" (read-image-ws (:websocket request))
             "POST /save-sign" (save-sign (parse-body request))
             "POST /save-parameters" (save-parameters (parse-body request))
             "POST /logout" (ssn/logout request)
             "POST /get-labels" (lang/get-labels request)
             "POST /set-language" (lang/set-language
                                    request
                                    (parse-body request))
             {:status (stc/not-found)
              :headers {(eh/content-type) (mt/text-plain)}
              :body (str {:status  "success"})})]
      (update-in
        response
        [:headers]
        assoc
        cookie-key
        cookie-value))
    (case request-start-line
      "POST /login" (ssn/login-authentication
                      (parse-body
                        request)
                      (:user-agent request))
      "POST /sign-up" (dao/insert-entity (parse-body request))
      "POST /am-i-logged-in" (ssn/am-i-logged-in request)
      "POST /get-labels" (lang/get-labels request)
      {:status (stc/unauthorized)
       :headers {(eh/content-type) (mt/text-plain)}
       :body (str {:status  "success"})})
   ))

(defn start-server
  "Start server"
  []
  (try
    (srvr/start-server
      routing
      {(rsh/access-control-allow-origin) #{"https://ocr:8451"
                                           "http://ocr:8453"}
       (rsh/access-control-allow-methods) "GET, POST, DELETE, PUT"}
      1606)
    (ws-srvr/start-server
      routing
      1607)
    (mon/mongodb-connect
      db-name)
    (ssn/create-indexes)
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

