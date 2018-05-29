(defproject ocr-server "0.1.0"
  :description "Optical character recognition server"
  :url "http://gitlab:1610/VladimirMarkovic86/ocr-server"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure			   "1.9.0"]
         									;	https://clojure.org/api/api
         								[compojure													   "1.6.0"]
         									;	http://weavejester.github.io/compojure/
         								[ring/ring-core								   "1.6.3"]
         									;	https://ring-clojure.github.io/ring/index.html
         								[ring/ring-jetty-adapter	 "1.6.3"]
         									;	https://ring-clojure.github.io/ring/ring.adapter.jetty.html
         								[ring-cors "0.1.12"]
         								 ;https://github.com/r0man/ring-cors
         									;	[ring/ring-ssl "0.3.0"];https://github.com/ring-clojure/ring-ssl
         								[org.vladimir/mongo-lib "0.1.0"]
         								[org.vladimir/utils-lib "0.1.0"]
         								[org.vladimir/ajax-lib "0.1.0"]
         								[org.vladimir/ocr-lib "0.1.0"]
         								]
  
  ; AOT - Compailation ahead of time
  :main ^:skip-aot ocr-server.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
