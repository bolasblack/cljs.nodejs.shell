# cljs.nodejs.shell

Read [https://clojuredocs.org/clojure.java.shell](https://clojuredocs.org/clojure.java.shell) and [doc-string](https://github.com/bolasblack/cljs.nodejs.shell/blob/master/src/cljs/nodejs/shell.cljs#L11) for usage.

## Difference between cljs.nodejs.shell, [clojure.java.shell](https://clojuredocs.org/clojure.java.shell), [planck.shell](http://planck-repl.org/planck-namespaces.html)

&nbsp;|cljs.nodejs.shell | clojure.java.shell | planck.shell
-----|-----|-----|-----
`:in` in option | `String` or `js/Buffer` | [any legal input source for clojure.java.io/copy](https://github.com/clojure/clojure/blob/fe0cfc71e6ec7b546066188c555b01dae0e368e8/src/clj/clojure/java/shell.clj#L84) | `String` or [file url scheme](https://github.com/mfikes/planck/blob/f16c065ca09e24c9d73191805c402985137e83a9/planck-cljs/src/planck/shell.cljs#L57)
`:out` in result | `String` or `js/Buffer` | `byte[]` or `String` | `String`
spawn command asynchronously | no | no | [`sh-async`](https://github.com/mfikes/planck/blob/f16c065ca09e24c9d73191805c402985137e83a9/planck-cljs/src/planck/shell.cljs#L80)

## Install

```clojure
;; deps.edn
{:deps {cljs.nodejs.shell {:git/url "https://github.com/bolasblack/cljs.nodejs.shell"
                           :tag "0.2.0"}}}
```
