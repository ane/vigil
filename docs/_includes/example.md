### Example

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
