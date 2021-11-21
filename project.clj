(defproject webapp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [mount "0.1.16"]

                 [ring "1.9.4"]
                 [ring/ring-anti-forgery "1.3.0"]
                 [ring/ring-defaults "0.3.2"]
                 [ring-logger "1.0.1"]
                 [compojure "1.6.1"]

                 [org.xerial/sqlite-jdbc "3.36.0.3"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.3"]
                 [cheshire "5.10.1"]
                 
                 [hiccup "1.0.5"]
                 [funcool/struct "1.3.0"]
                 [com.draines/postal "2.0.4"]
                 [crypto-password "0.3.0"]]
  
  :main webapp.core
  
  :repl-options {:init-ns webapp.core})
