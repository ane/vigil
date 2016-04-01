# ![Vigil](./doc/vigil.png) [![Build Status](https://travis-ci.org/ane/vigil.svg?branch=master)](https://travis-ci.org/ane/vigil) [![Clojars Project](https://img.shields.io/clojars/v/vigil.svg)](https://clojars.org/vigil) [![EPL](https://camo.githubusercontent.com/abf24e4845a7f0721ff08b2a8284ded5d9cfdefa/687474703a2f2f696d672e736869656c64732e696f2f62616467652f6c6963656e73652d45504c2d626c75652e7376673f7374796c653d666c6174)](https://www.eclipse.org/legal/epl-v10.html) 
**Vigil** creates [Manifold](https://github.com/ztellman/manifold) streams from files.  You place a
*watcher* on a file and the stream produces content as new content is appended to the file.
```clojure
[vigil "0.1.1"] ; add this to your project.clj
```
Vigil can be used to monitor logs asynchronously as a stream. If you are creating an event-driven
system, it's easy to create an event filter with Vigil.

Because the produced streams are Manifold streams, which act as a general-purpose compability
layers for asynchronous communication, the file streams can be easily
[connected](https://github.com/ztellman/manifold/blob/master/docs/stream.md) into other 
Clojure constructs, such as [core.async](https://github.com/clojure/core.async), lazy sequences,
promises, the list goes on. See the [example](#example).

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


## Usage

The watcher can be stopped at any time by closing the stream. If you don't want to receive the
initial content, pass `false` to the `initial` parameter in `watch-file`.

### Example: turn a file into a core.async channel

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

## License

Copyright © 2016 Antoine Kalmbach <ane@iki.fi>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
