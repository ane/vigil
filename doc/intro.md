# Introduction to vigil

Vigil is a library that creates [Manifold](https://github.com/ztellman/manifold) streams out of
files. Creating a Manifold stream lets you monitor a file as a stream, producing data as content is
appended to the file.

As Manifold streams, the streams can be combined with different asynchronous abstractions: channels,
lazy sequences, promises, and so forth. Here are some examples.

## Examples

Here are some examples on how you would use Vigil.

### Simple file monitoring as a Manifold stream

``` clojure
(require '[vigil.core :as v]
         '[manifold.stream :as s])

;; /foo/bar/baz contains "hello\nworld"
(def stream (v/watch-file "/foo/bar/baz"))

@(s/take! stream)
;; => ("hello" "world")

(spit "/foo/bar/baz" "blah\n" :append true)

;; the new line has now been pushed to the stream
@(s/take! stream)
;; => ("blah")

;; shut down the watcher
(s/close! stream)

```

### Turn a file into a core.async channel

``` clojure
(require '[manifold.stream :as s]
         '[vigil.core :as v]
         '[clojure.core.async :as a])
         
(def fs (v/watch-file "/foo/bar/baz"))
(def ch (a/chan))

;; forward fs into ch
(s/connect fs ch)

(a/go-loop []
  (when-let [stuff (a/<! ch)]
    (println stuff)
    (recur)))
       
(Thread/sleep 60000)

;; append content into /foo/bar/baz (manually)
;; ... and it shall be printed to *out*

;; if you pass :upstream? true to s/connect,
;; closing the channel will close fs, thereby
;; shutting down the watcher
(a/close! ch)
(s/close! fs)

```

#### Deletion or truncation

If you delete the file, the watcher will stop. Truncating the file will make the watcher reset its
*cursor* to the beginning. Truncation occurs when the cursor location has been observed to be
greater than the length of the file.
