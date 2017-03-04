(ns cljs.nodejs.shell-test
  (:require [clojure.test :refer [deftest are is run-tests]]
            [clojure.string :as s]
            [cljs.nodejs :as nodejs]
            [cljs.nodejs.shell :as sh :include-macros true]))

(nodejs/enable-util-print!)

(deftest shell
  (let [result (sh/sh "echo" "hello")]
    (are [x y] (= x y)
      (type (result :out)) js/Buffer
      (.toString (result :out)) "hello\n"

      (type (result :err)) js/Buffer
      (.toString (result :err)) ""

      (result :exit) 0)))

(deftest capture-exit-value
  (are [x y] (= x y)
    0 (:exit (sh/sh "sh" "-c" "exit 0"))
    1 (:exit (sh/sh "sh" "-c" "exit 1"))
    17 (:exit (sh/sh "sh" "-c" "exit 17"))))

(deftest with-sh-dir-test
  (are [op x y] (op x y)
    ;; /tmp\n or /private/tmp\n
    s/ends-with? (.toString (:out (sh/with-sh-dir "/tmp" (sh/sh "pwd")))) "/tmp\n"
    not= (:exit (sh/with-sh-dir "bogus" (sh/sh "pwd"))) 0))

(deftest with-sh-env-test
  (is (= "FOO=BAR\n" (.toString (:out (sh/with-sh-env {"FOO" "BAR"}
                                        (sh/sh "env")))))))

(defn -main [& args]
  (try
    (run-tests)
    (catch js/Error e
      (println (.-stack e)))))

(set! *main-cli-fn* -main)
