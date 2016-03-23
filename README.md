# Vigil

**vigil** exposes files as [Manifold](https://github.com/ztellman/manifold)
streams. You *watch* a file and that stream outputs a dataflow of its line-oriented contents. The
watcher reacts to file system changes and pushes new content via the stream, and updates its cursor
to the new position.

``` clojure
(require [vigil.core :as v])

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

If you delete the file, the watcher will stop. Truncating the file will make the watcher reset its
*cursor* to the beginning. Truncation occurs when the cursor location has been observed to be
greater than the length of the file.

## Usage

The watcher can be stopped at any time by closing the stream. If you don't want to receive the
initial content, pass `:initial? false` to `file`.

## License

Copyright © 2016 Antoine Kalmbach <ane@iki.fi>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
