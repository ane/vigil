# ![Vigil](./doc/vigil.png)

**Vigil** creates [Manifold](https://github.com/ztellman/manifold)
streams of files. You *watch* a file and that stream outputs a dataflow of its line-oriented contents. The
watcher reacts to file system changes and pushes new content via the stream, and updates its cursor
to the new position.

Because the produced streams are Manifold streams, which acts as a general-purpose compability
layer, the file streams can be easily
[connected](https://github.com/ztellman/manifold/blob/master/docs/stream.md) into other 
Clojure constructs, such as [core.async](https://github.com/clojure/core.async), lazy sequences,
promises, the list goes on. See the [example](#example).

``` clojure
(require '[vigil.core :as v])

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


## Usage

The watcher can be stopped at any time by closing the stream. If you don't want to receive the
initial content, pass `:initial? false` to `file`.

### Example

``` clojure
(require '[manifold.stream :as s]
         '[vigil.core :as v]
         '[core.async :as a])
         
(def fs (v/watch-file "/foo/bar/baz"))

(let [ch (s/connect fs (a/chan))]
  (go-loop
    (when-let [stuff (a/<! ch)]
       (println stuff)
       (recur))))
       
;; append content into /foo/bar/baz
       
(Thread/sleep 60000)
(s/close! fs)

```

#### Deletion or truncation

If you delete the file, the watcher will stop. Truncating the file will make the watcher reset its
*cursor* to the beginning. Truncation occurs when the cursor location has been observed to be
greater than the length of the file.

## License

Copyright © 2016 Antoine Kalmbach <ane@iki.fi>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
