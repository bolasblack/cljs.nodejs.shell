(ns cljs.nodejs.shell
  (:require-macros [cljs.nodejs.shell])
  (:require [cljs.nodejs :as nodejs]
            ["child_process" :as child-process]
            ["iconv-lite" :as iconv]))

(def ^:dynamic *sh-dir* nil)
(def ^:dynamic *sh-env* nil)

(defn sh
  "Passes the given strings to child-process.spawnSync() to launch a
  sub-process.

  Options are
  :in      js/Buffer or String, to be fed to the sub-process's stdin.
  :in-enc  option may be given followed by a String. If a String is
           given, it will be used as a character encoding name (for example
           \"UTF-8\" or \"ISO-8859-1\") to convert the input string
           specified by the :in option to the sub-process's stdin.
           Defaults to UTF-8 if :in is String, or do nothing if :in is
           js/Buffer.
  :out-enc option may be given followed by a String. If a String is
           given, it will be used as a character encoding name (for example
           \"UTF-8\" or \"ISO-8859-1\") to convert the sub-process's stdout
           to a String which is returned.
           Defaults do nothing and return js/Buffer.
  :env     override the process env with a map.
  :dir     override the process dir with a String.
  You can bind :env or :dir for multiple operations using with-sh-env
  and with-sh-dir.
  sh returns a map of
    :exit => sub-process's exit code
    :out  => sub-process's stdout (as js/Buffer or String)
    :err  => sub-process's stderr (String via platform default encoding)"
  [& args]
  (let [[cmd {:keys [in in-enc out-enc env dir]}] (split-with string? args)
        decoded-in (if (nil? in-enc) in (.decode iconv in in-enc))
        spawn-opts {:input decoded-in :env (or env *sh-env*) :cwd (or dir *sh-dir*)}
        spawn-result (.spawnSync child-process
                                 (first cmd)
                                 (clj->js (or (next cmd) []))
                                 (clj->js spawn-opts))
        exit (.-status spawn-result)
        raw-out (.-stdout spawn-result)
        raw-err (.-stderr spawn-result)
        decoded-out (if (and raw-out out-enc) (.decode iconv raw-out out-enc) raw-out)
        decoded-err (if (and raw-err out-enc) (.decode iconv raw-err out-enc) raw-err)]
    {:exit exit :out decoded-out :err decoded-err}))
