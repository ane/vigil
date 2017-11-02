---
layout: default
---

# User guide
{:.no_toc}

* This will be automatically removed
{:toc}

### Introduction

Vigil turns files into asynchronous streams. A stream of data is an eventual source of data. This
source can be subscribed to, and subscribers will be notified of new data. This is known as 
[stream processing](https://en.wikipedia.org/wiki/Stream_processing).

The idea is that we are interested in new content being appended to a file. We are specifically
interested in the appended content, like observing a log file. But we don't want to limit ourselves
in how we consume this data. We just want to expose it as asynchronous, or rather, eventual. 

This is the essence of Vigil. You place a *watcher* on a filesystem file and then you can stream
content as it is appended to that file.


### Mechanism

Vigil uses [Manifold](http://github.io/ztellman/aleph) to expose files as streams.

Manifold is a Clojure library for creating asynchronous streams. Manifold *streams* are
interoperable with a wide number of abstractions, such as
[core.async](https://github.com/clojure/core.async) channels, [lazy
sequences](https://clojure.org/reference/sequences),
[promises](https://clojuredocs.org/clojure.core/promise), and
[more](http://aleph.io/manifold/rationale.html).

### Usage

The watcher can be stopped at any time by closing the stream. If you don't want to receive the
initial content, pass `false` to the `initial` parameter in `watch-file`. If you delete or truncate
the file, the watcher will stop and the stream will be closed.

### Examples

#### Monitor a file

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

#### Convert a file into a channel

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

## License

Copyright © [Antoine Kalmbach](mailto:ane@iki.fi). All rights reserved.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
