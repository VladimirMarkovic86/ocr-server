(ns ocr-server.core
  (:gen-class)
  (:require [session-lib.core :as ssn]
            [server-lib.core :as srvr]
            [utils-lib.core :refer [parse-body]]
            [mongo-lib.core :as mon]
            [ocr-server.scripts :as scripts]
            [common-server.core :as rt]
            [ocr-middle.functionalities :as omfns]
            [ocr-middle.request-urls :as orurls]
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

(def db-uri
     (or (System/getenv "MONGODB_URI")
         (System/getenv "PROD_MONGODB")
         "mongodb://admin:passw0rd@127.0.0.1:27017/admin"))

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
        light-value (read-string
                      (:light-value
                        request-body))
        contrast-value (read-string
                         (:contrast-value
                           request-body))
        space-value (read-string
                      (:space-value
                        request-body))
        hooks-value (read-string
                      (:hooks-value
                        request-body))
        matching-value (read-string
                         (:matching-value
                           request-body))
        threads-value (read-string
                        (:threads-value
                          request-body))
        rows-threads-value (read-string
                             (:rows-threads-value
                               request-body))
        unknown-sign-count-limit-value (read-string
                                         (:unknown-sign-count-limit-value
                                           request-body))]
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
        :rows-threads rows-threads-value
        :unknown-sign-count-limit unknown-sign-count-limit-value})
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
        request-body (read-string
                       websocket-message)
        _id (:_id request-body)
        light-value (read-string
                      (:light-value
                        request-body))
        contrast-value (read-string
                         (:contrast-value
                           request-body))
        space-value (read-string
                      (:space-value
                        request-body))
        hooks-value (read-string
                      (:hooks-value
                        request-body))
        matching-value (read-string
                         (:matching-value
                           request-body))
        threads-value (read-string
                        (:threads-value
                          request-body))
        rows-threads-value (read-string
                             (:rows-threads-value
                               request-body))
        unknown-sign-count-limit-value (read-string
                                         (:unknown-sign-count-limit-value
                                           request-body))
        unknown-sign-count-limit-per-thread (when (and (number?
                                                         unknown-sign-count-limit-value)
                                                       (number?
                                                         rows-threads-value)
                                                       (< 0
                                                          unknown-sign-count-limit-value)
                                                       (< 0
                                                          rows-threads-value))
                                              (int
                                                (/ unknown-sign-count-limit-value
                                                   rows-threads-value))
                                             )
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
                                 document-signs
                                 unknown-sign-count-limit-per-thread)
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

(defn response-routing-fn
  "Custom routing function"
  [request]
  (let [{request-uri :request-uri
         request-method :request-method} request]
    (cond
      (= request-method
         "ws GET")
        (cond
          (= request-uri
             orurls/process-images-ws-url)
            (process-images-ws (:websocket request))
          (= request-uri
             orurls/read-image-ws-url)
            (read-image-ws (:websocket request))
          :else
            nil)
      (= request-method
         "POST")
        (cond
          (= request-uri
             orurls/save-sign-url)
            (save-sign (parse-body request))
          (= request-uri
             orurls/save-parameters-url)
            (save-parameters (parse-body request))
          :else
            nil)
      :else
        nil))
 )

(defn allow-action-routing-fn
  "Check if action is allowed for logged in user"
  [request]
  (let [allowed-functionalities (rt/get-allowed-actions
                                  request)
        {request-uri :request-uri
         request-method :request-method} request]
    (cond
      (= request-method
         "ws GET")
        (cond
          (= request-uri
             orurls/process-images-ws-url)
            (contains?
              allowed-functionalities
              omfns/process-images)
          (= request-uri
             orurls/read-image-ws-url)
            (contains?
              allowed-functionalities
              omfns/read-image)
          :else
            false)
      (= request-method
         "POST")
        (cond
          (= request-uri
             orurls/save-sign-url)
            (contains?
              allowed-functionalities
              omfns/save-sign)
          (= request-uri
             orurls/save-parameters-url)
            (contains?
              allowed-functionalities
              omfns/save-parameters)
          :else
            false)
      :else
        false))
 )

(defn routing
  "Routing function"
  [request]
  (let [response (rt/routing
                   request
                   (response-routing-fn
                     request)
                   (allow-action-routing-fn
                     request))]
    (audit
      request
      response)
    response))

(defn start-server
  "Start server"
  []
  (try
    (let [port (System/getenv "PORT")
          port (if port
                 (read-string
                   port)
                 1602)
          access-control-allow-origin #{"https://ocr:8451"
                                        "https://ocr:1612"
                                        "http://ocr:1612"
                                        "https://ocr:1602"
                                        "http://ocr:1602"
                                        "http://ocr:8453"}
          access-control-allow-origin (if (System/getenv "CLIENT_ORIGIN")
                                        (conj
                                          access-control-allow-origin
                                          (System/getenv "CLIENT_ORIGIN"))
                                        access-control-allow-origin)
          access-control-map {(rsh/access-control-allow-origin) access-control-allow-origin
                              (rsh/access-control-allow-methods) "OPTIONS, GET, POST, DELETE, PUT"
                              (rsh/access-control-allow-credentials) true}
          certificates {:keystore-file-path
                         "certificate/ocr_server.jks"
                        :keystore-password
                         "ultras12"}
          certificates (when-not (System/getenv "CERTIFICATES")
                         certificates)
          thread-pool-size (System/getenv "THREAD_POOL_SIZE")]
      (when thread-pool-size
        (reset!
          srvr/thread-pool-size
          (read-string
            thread-pool-size))
       )
      (srvr/start-server
        routing
        access-control-map
        port
        certificates))
    (mon/mongodb-connect
      db-uri
      db-name)
    (scripts/initialize-db-if-needed)
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

