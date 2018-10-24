(defproject org.clojars.vladimirmarkovic86/ocr-server "0.2.0"
  :description "Optical character recognition server"
  :url "http://github.com/VladimirMarkovic86/ocr-server"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojars.vladimirmarkovic86/server-lib "0.3.0"]
                 [org.clojars.vladimirmarkovic86/mongo-lib "0.2.0"]
                 [org.clojars.vladimirmarkovic86/utils-lib "0.2.0"]
                 [org.clojars.vladimirmarkovic86/ajax-lib "0.1.0"]
                 [org.clojars.vladimirmarkovic86/ocr-lib "0.1.0"]
                 [org.clojars.vladimirmarkovic86/session-lib "0.2.0"]
                 [org.clojars.vladimirmarkovic86/common-server "0.2.0"]
                 [org.clojars.vladimirmarkovic86/ocr-middle "0.1.0"]
                 ]

  :min-lein-version "2.0.0"
  
  :main ^:skip-aot ocr-server.core
  
  :uberjar-name "ocr-server-standalone.jar"
  :profiles {:uberjar {:aot :all}}
  :repl-options {:port 8602})

