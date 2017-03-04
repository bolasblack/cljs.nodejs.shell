(ns cljs.nodejs.shell
  (:require-macros [cljs.nodejs.shell])
  (:require [cljs.nodejs :as nodejs]
            cljsjs.iconv-lite))

(def ^:private child-process (nodejs/require "child_process"))
(def ^:private iconv js/iconv)

(def ^:dynamic *sh-dir* nil)
(def ^:dynamic *sh-env* nil)

(defn sh
  "Passes the given strings to child-process.spawnSync() to launch a
  sub-process.

  Options are
  :in      may be given followed by any legal input source for
           Buffer or String, to be fed to the sub-process's stdin.
  :in-enc  option may be given followed by a String, used as a character
           encoding name (for example \"UTF-8\" or \"ISO-8859-1\") to
           convert the input string specified by the :in option to the
           sub-process's stdin.  Defaults to UTF-8.
           If the :in option provides a byte array, then the bytes are passed
           unencoded, and this option is ignored.
  :out-enc option may be given followed by a String. If a String is
           given, it will be used as a character encoding name (for example
           \"UTF-8\" or \"ISO-8859-1\") to convert the sub-process's stdout
           to a String which is returned. Defaults to UTF-8.
  :env     override the process env with a map.
  :dir     override the process dir with a String.
  You can bind :env or :dir for multiple operations using with-sh-env
  and with-sh-dir.
  sh returns a map of
    :exit => sub-process's exit code
    :out  => sub-process's stdout (as Buffer or String)
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
