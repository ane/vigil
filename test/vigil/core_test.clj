(ns vigil.core-test
  (:require [clojure.java.io :as io :refer [as-file]]
            [clojure.test :refer :all]
            [vigil.core :as v]
            [manifold.stream :as s]))

(def test-file (atom ""))

(defn create-temp-file [testfn]
  (reset! test-file (.getCanonicalPath (java.io.File/createTempFile "derp" ".txt")))
  (testfn))

(defn- write-test-content [cont]
  (spit @test-file cont))

(defn- append-test-content [cont]
  (spit @test-file cont :append true))

(deftest reads-to-end
  (write-test-content "hello\nthere")
  (is (= (first (v/read-to-end @test-file 0))
         '("hello" "there"))))

(deftest reads-to-end-after-append
  (write-test-content "hello\nthere")
  (let [[cont pos] (v/read-to-end @test-file 0)]
    (append-test-content "asdf")
    (let [[newcont pos2] (v/read-to-end @test-file pos)]
      (is (= (concat cont newcont) '("hello" "there" "asdf") ))
      (append-test-content "ding dong")
      (let [[newcont2 _] (v/read-to-end @test-file pos2)]
      (is (= (concat cont newcont newcont2) '("hello" "there" "asdf" "ding dong")))))))

(deftest read-update-works
  (let [s (s/stream 1) ; accept exactly one message
        pos 0
        file @test-file
        freq 10
        teststr "i'm\non\na\nhorse"]
    (write-test-content teststr)
    (let [newpos (v/read-update s file 0 freq)]
      (is (= (.length teststr) newpos)))))

(deftest periodic-read-updates
  (let [s (s/stream 1) ; we'll take two
        pos 0
        file @test-file
        freq 10
        teststr "i'm\non\na\nhorse"
        another-str "well\nyeah"]
    (write-test-content teststr)
    (let [newpos (v/read-update s file 0 freq)]
      (is (= (.length teststr) newpos))
      @(s/take! s) ; yank the deferred out
      (append-test-content another-str)
      (let [again (v/read-update s file newpos freq)]
        (is (= (+ newpos (.length another-str)) again))))))

(use-fixtures :each create-temp-file)
