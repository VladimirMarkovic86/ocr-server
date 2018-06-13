(defproject ocr-server "0.1.0"
  :description "Optical character recognition server"
  :url "http://gitlab:1610/VladimirMarkovic86/ocr-server"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure			   "1.9.0"]
         									;	https://clojure.org/api/api
         								[org.vladimir/server-lib "0.1.0"]
         								[org.vladimir/mongo-lib "0.1.0"]
         								[org.vladimir/utils-lib "0.1.0"]
         								[org.vladimir/ajax-lib "0.1.0"]
         								[org.vladimir/ocr-lib "0.1.0"]
         								]
  
  ; AOT - Compailation ahead of time
  :main ^:skip-aot ocr-server.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
