(defproject vigil "0.1.1"
  :description "Watch files as continuous streams"
  :url "http://github.com/ane/vigil"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [manifold "0.1.3"]]
  :plugins [[lein-codox "0.9.4"]]
  :codox {:source-paths ["src"]
          :doc-paths ["doc"]
          :namespaces [vigil.core]
          :metadata {:doc/format :markdown}})
