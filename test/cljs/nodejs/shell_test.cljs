(ns cljs.nodejs.shell-test
  (:require [clojure.test.check]
            [clojure.test.check.generators]
            [clojure.test.check.properties]
            [cljs.test :refer [deftest are is] :refer-macros [async]]
            [clojure.string :as str]
            [cljs.nodejs.shell :as sh :include-macros true]))

(deftest shell
  (let [result (sh/sh "echo" "hello")]
    (are [x y] (= x y)
      (type (:out result)) js/Buffer
      (.toString (:out result)) "hello\n"
      (type (:err result)) js/Buffer
      (.toString (:err result)) ""
      (:exit result) 0
      (:signal result) nil)))

(deftest shell-async
  (async
   done
   (sh/sh-async
    "echo" "hello"
    (fn [result]
      (are [x y] (= x y)
        (type (:out result)) js/Buffer
        (.toString (:out result)) "hello\n"
        (type (:err result)) js/Buffer
        (.toString (:err result)) ""
        (:exit result) 0
        (:signal result) nil)
      (done)))))

(deftest sh-capture-exit-value
  (are [x y] (= x y)
    1 (:exit (sh/sh "sh" "-c" "exit 1"))))
(deftest sh-async-capture-exit-value-1
  (async
   done
   (sh/sh-async
    "sh" "-c" "exit 1"
    (fn [result]
      (is (= 1 (:exit result)))
      (done)))))

(deftest sh-with-sh-dir-test
  (is (str/ends-with? (.toString (:out (sh/with-sh-dir "/tmp" (sh/sh "pwd")))) "/tmp\n"))
  (let [res (sh/with-sh-dir "bogus" (sh/sh "pwd"))]
    (is (not= (:exit res) 0))
    (is (= "spawnSync pwd ENOENT" (.-message (:error res))))))
(deftest sh-async-with-sh-dir-test-exist-bin
  (async
   done
   (sh/with-sh-dir
     "/tmp"
     (sh/sh-async
      "pwd"
      (fn [res]
        (is (str/ends-with? (.toString (:out res)) "/tmp\n"))
        (done))))))
(deftest sh-async-with-sh-dir-test-non-exist-bin
  (async
   done
   (sh/with-sh-dir
     "bogus"
     (sh/sh-async
      "pwd"
      (fn [res]
        (is (not= (:exit res) 0))
        (is (= "spawn pwd ENOENT" (.-message (:error res))))
        (done))))))

(deftest sh-with-sh-env-test
  (is (thrown-with-msg? js/Error #"cljs\.spec\.alpha"
                        (sh/with-sh-env {"FOO" {}}
                          (sh/sh "env"))))
  (is (= "FOO=BAR\n" (.toString (:out (sh/with-sh-env {"FOO" "BAR"}
                                        (sh/sh "env")))))))
(deftest sh-async-with-sh-env-test
  (async
   done
   (is (thrown-with-msg? js/Error #"cljs\.spec\.alpha"
                         (sh/with-sh-env {"FOO" {}}
                           (sh/sh-async
                            "env"
                            (fn [res]
                              (is (= "FOO=BAR\n" (.toString (:out res))))
                              (done))))))
   (sh/with-sh-env {"FOO" "BAR"}
     (sh/sh-async
      "env"
      (fn [res]
        (is (= "FOO=BAR\n" (.toString (:out res))))
        (done))))))
