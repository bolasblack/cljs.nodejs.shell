(ns cljs.nodejs.shell)

(defmacro with-sh-dir
  "Sets the directory for use with sh, see sh for details."
  [dir & forms]
  `(binding [*sh-dir* ~dir]
     ~@forms))

(defmacro with-sh-env
  "Sets the environment for use with sh, see sh for details."
  [env & forms]
  `(binding [*sh-env* ~env]
     ~@forms))
