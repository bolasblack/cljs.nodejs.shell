(def +project+ 'org.clojars.c4605/cljs.nodejs.shell)
(def +version+ "0.1.0")
(def +description+ "A clojurescript replacement for clojure.java.shell in nodejs runtime.")

(set-env!
 :resource-paths #{"src"}
 :source-paths #{"test"}
 :dependencies '[[org.clojure/clojurescript "1.9.473" :scope "test"]
                 [org.clojure/clojure "1.8.0" :scope "test"]
                 [adzerk/boot-cljs "1.7.228-2" :scope "test"]
                 [adzerk/bootlaces "0.1.13" :scope "test"]
                 [cljsjs/iconv-lite "0.4.15-0"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.bootlaces :refer [push-release]])

(task-options!
 pom {:project +project+
      :version +version+
      :description +description+
      :license {"MIT" "http://opensource.org/licenses/MIT"}
      :scm {:url "https://github.com/bolasblack/cljs.nodejs.shell"}})

(deftask build-cljs []
  (cljs :compiler-options {:target :nodejs}))

(deftask build-jar []
  (comp
    (pom)
    (jar)
    (install)
    (target)))

(deftask deploy []
  (set-env!
    :repositories #(conj % ["clojars" {:url "https://clojars.org/repo/"}]))
  (comp
    (build-jar)
    (push-release)))
