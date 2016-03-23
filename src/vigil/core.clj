(ns vigil.core
  (:require [clojure.java.io :as io :refer [as-file]]
            [clojure.string :refer [split-lines]]
            [manifold.stream :as s])
  (:import java.io.RandomAccessFile
           (java.nio.file FileSystems
                          Path
                          StandardWatchEventKinds
                          WatchService
                          WatchEvent
                          WatchKey)))

(defn read-to-end
  "Reads `path` to the end starting from `pos`."
  [^String path ^Long pos]
  (with-open [src (RandomAccessFile. path "r")]
    (let [length (.length src)]
      (if (<= pos length)
        (with-open [buf (java.io.FileInputStream. (.getFD src))]
          (.skipBytes src (- pos (if (< 0 pos) 1 0)))
          [(filter #(not (empty? %)) (split-lines (slurp buf))) length])
        ["" 0]))))

(defn read-update [stream file old-pos freq]
  "Reads the content of `file` from `pos` and dumps them into `stream`.
Returns the new position where we read, if the stream accepted the
content.  If the stream did not accept the content after `freq`
milliseconds, it returns the old value, so you can keep calling this
function in an idempotent manner."
  (when-not (s/closed? stream)
    (let [[cont new-pos] (read-to-end file old-pos)]
      (if-not (empty? cont)
        (case @(s/try-put! stream cont freq :fail)
          :fail old-pos
          true  new-pos)
        (if (< new-pos old-pos) ; truncated?
          new-pos
          old-pos)))))

(defn- handle-event
  [watch-key file callback]
  (doseq [ev (.pollEvents watch-key)]
    (let [kind (.kind ev)
          context (.context ev)
          abs-path (->> file
                        .getName
                        (.resolve (.watchable watch-key))
                        .toAbsolutePath)
          test-path (.toPath file)]
      (when (.equals test-path abs-path)
        (callback abs-path kind context)))
    (.reset watch-key)))

(defn- make-handler
  [cursor stream file throttle]
  (fn [& _]
    (when-let [value (read-update stream file @cursor throttle)]
      (dosync (ref-set cursor value)))))

(defn- make-watcher
  [file-path cursor s throttle]
  (let [fs (FileSystems/getDefault)
        watcher (.newWatchService fs)
        file (as-file file-path)
        path (.getParent (.toPath file))]
    (.register path watcher (into-array [StandardWatchEventKinds/ENTRY_MODIFY]))
    {:future
     (future
       (loop []
         (when-let [watch-key (.poll watcher)]
           (handle-event watch-key file (make-handler cursor s file-path throttle)))
         (Thread/sleep throttle)
         (recur)))
     :watcher watcher}))

(defn- stop-watcher
  [{:keys [future watcher]}]
  (future-cancel future)
  (.close watcher))

(defn read-initial
  [file initial? s cursor]
  (with-open [src (RandomAccessFile. file "r")
              f (io/reader (java.io.FileInputStream. (.getFD src)))]
    (let [lines (line-seq f)]
      (when initial?
        (s/put! s (doall lines)))
      (dosync
       (ref-set cursor (.getFilePointer src))))))

(defn watch-file
  [^String file & [initial? buffer-size throttle]]
  "Watches `file` for changes on disk and returns a Manifold stream
  representing its content returning sequences of lines. By closing
  the stream you kill the watch process. The watcher contains a cursor
  that updates every time content is pushed successfully into the
  returned stream.

Takes an optional parameters.
|:---|:---
| `initial?` | push the initial contents of the file into the stream, defaults to `true`.
| `buffer-size` | the size of the stream buffer, defaults to 1.
| `throttle` | wait up to n milliseconds when pushing new content to the stream sink, i.e. how long will the watcher wait for the stream to accept new content. If `n` milliseconds pass before new content is accepted, the cursor does not advance, defaults to 1000 ms."
  (let [as-f (as-file file)
        initial? (or initial? true)
        buffer-size (or buffer-size 1)
        throttle (or throttle 1000)]
    (when (.exists as-f)
      (let [s (s/stream buffer-size)]
        (future
          (let [cursor (ref 0)]
            (read-initial file initial? s cursor)
            (try
              (let [watcher (make-watcher file cursor s throttle)]
                (loop []
                  (Thread/sleep throttle)
                  (when (and (not (s/closed? s)) (.exists as-f))
                    (recur)))
                (stop-watcher watcher))
              (catch java.io.IOException e
                (println (.getMessage e))
                (s/close! s)))))
        s)))) 

