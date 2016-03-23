(ns vigil.core-test
  (:require [clojure.java.io :as io :refer [as-file]]
            [clojure.test :refer :all]
            [vigil.core :as v]))

(def ^:dynamic *test-file* (->> "testfile"
                                io/resource
                                as-file
                                .getCanonicalPath))

(defn- write-test-content [cont]
  (spit *test-file* cont))

(defn- append-test-content [cont]
  (spit *test-file* (str cont "\n") :append true))

(deftest reads-to-end
  (write-test-content "hello\nthere")
  (is (= (first (v/read-to-end *test-file* 0))
         '("hello" "there"))))
