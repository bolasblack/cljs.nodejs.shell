(ns cljs.nodejs.shell
  (:require-macros [cljs.nodejs.shell])
  (:require [cljs.spec.alpha :as s]
            ["child_process" :as child-process]
            ["iconv-lite" :as iconv]))

(def ^:dynamic *sh-dir* nil)
(def ^:dynamic *sh-env* nil)

(defn- deserialize-opts [cmd opts]
  (let [{:keys [in in-enc out-enc env dir]} opts
        decoded-in (if (nil? in-enc) in (iconv/decode in in-enc))
        spawn-opts {:input decoded-in
                    :env (or env *sh-env*)
                    :cwd (or dir *sh-dir*)}]
    (when-not (s/valid? (s/nilable ::string-string-map?) (:env spawn-opts))
      (throw (js/Error. (s/explain-str ::string-string-map? (:env spawn-opts)))))
    {:bin (first cmd)
     :args (or (next cmd) '())
     :options spawn-opts}))

(defn- deserialize-spawn-result
  [{:keys [exit signal error stdout stderr] :as args}
   {:keys [out-enc]}]
  (let [decoded-out (if (and stdout out-enc) (iconv/decode stdout out-enc) stdout)
        decoded-err (if (and stderr out-enc) (iconv/decode stderr out-enc) stderr)]
    {:exit exit
     :signal signal
     :error error
     :out decoded-out
     :err decoded-err}))

(defn sh
  "Passes the given strings to child_process.spawnSync() to launch a
  sub-process.

  Parameters: cmd, <options>, cb
  cmd      the command(s) (Strings) to execute. will be concatenated together.
  options  optional keyword arguments-- see below.

  Options are:
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

  if the command can be launched, sh returns a map of
    :exit   => The exit code of the child process (Number)
    :signal => The signal used to kill the child process (String)
    :error  => The error object if the child process failed or timed out (Error)
    :out  => sub-process's stdout (as js/Buffer or String)
    :err  => sub-process's stderr (as js/Buffer or String)"
  [& args]
  (let [{:keys [cmd opts]} (s/conform ::sh-args args)
        {:keys [bin args options]} (deserialize-opts cmd opts)
        spawn-result (child-process/spawnSync bin (clj->js args) (clj->js options))]
    (deserialize-spawn-result
     {:exit spawn-result.status
      :signal spawn-result.signal
      :error spawn-result.error
      :stdout spawn-result.stdout
      :stderr spawn-result.stderr}
     opts)))

(defn sh-async
  "Passes the given strings to child_process.spawn() to launch a
  sub-process.

  Parameters: cmd, <options>, cb
  cmd      the command(s) (Strings) to execute. will be concatenated together.
  options  optional keyword arguments-- see below.
  cb       the callback to call upon completion

  Options are:
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

  if the command can be launched, sh-async calls back with a map of
    :exit   => The exit code of the child process (Number)
    :signal => The signal used to kill the child process (String)
    :error  => The error object if the child process failed or timed out (Error)
    :out    => sub-process's stdout (as js/Buffer or String)
    :err    => sub-process's stderr (as js/Buffer or String)"
  [& args]
  (let [{:keys [cmd opts cb]} (s/conform ::sh-async-args args)
        {:keys [bin args options]} (deserialize-opts cmd opts)
        process (child-process/spawn bin (clj->js args) (clj->js options))
        empty-buf (js/Buffer.alloc 0)
        spawn-results (volatile! {:finished? false
                                  :stdout empty-buf
                                  :stderr empty-buf})
        stash-result (fn [key]
                       (fn [new-buf]
                         (vswap!
                          spawn-results
                          (fn [result]
                            (update result key (fn [curr-buf]
                                                 (js/Buffer.concat
                                                  #js [curr-buf new-buf]
                                                  (+ curr-buf.length new-buf.length))))))))
        stdout-callback (stash-result :stdout)
        stderr-callback (stash-result :stderr)]

    (.. process -stdout (on "data" stdout-callback))
    (.. process -stderr (on "data" stderr-callback))

    (.once
     process
     "error"
     (fn [err]
       (when-not (:finished? @spawn-results)
         (vswap! spawn-results #(assoc % :finished? true))
         (.. process -stdout (removeListener "data" stdout-callback))
         (.. process -stderr (removeListener "data" stderr-callback))
         (cb (deserialize-spawn-result
              {:exit nil
               :signal nil
               :error err
               :stdout (:stdout @spawn-results)
               :stderr (:stderr @spawn-results)}
              opts)))))

    (.once
     process
     "exit"
     (fn [code signal]
       (when-not (:finished? @spawn-results)
         (vswap! spawn-results #(assoc % :finished? true))
         (.. process -stdout (removeListener "data" stdout-callback))
         (.. process -stderr (removeListener "data" stderr-callback))
         (cb (deserialize-spawn-result
              {:exit code
               :signal signal
               :error nil
               :stdout (:stdout @spawn-results)
               :stderr (:stderr @spawn-results)}
              opts)))))))



(s/def ::string-string-map?
  (s/and map? (s/every-kv string? string?)))

(s/def ::js-buffer?
  (s/with-gen #(instance? js/Buffer %)
    #(s/gen #{(js/Buffer.from "Generated Buffer")})))

(s/def ::exit integer?)
(s/def ::signal string?)
(s/def ::error (s/with-gen #(instance? js/Error %)
                 #(s/gen #{(js/Error. "Generated Error")})))
(s/def ::out (s/or :string string?
                   :buffer ::js-buffer?))
(s/def ::err (s/or :string string?
                   :buffer ::js-buffer?))

(s/def ::sh-result
  (s/keys :req-un [::exit ::signal ::error ::out ::err]))

(s/def ::sh-opt
  (s/alt :in (s/cat :key #{:in} :val string?)
         :in-enc (s/cat :key #{:in-enc} :val string?)
         :out-enc (s/cat :key #{:out-enc} :val string?)
         :dir (s/cat :key #{:dir} :val string?)
         :env (s/cat :key #{:env} :val ::string-string-map?)))

(s/def ::sh-args
  (s/cat :cmd (s/+ string?)
         :opts (s/* ::sh-opt)))
(s/def ::sh-async-args
  (s/cat :cmd (s/+ string?)
         :opts (s/* ::sh-opt)
         :cb fn?))

(s/fdef sh
  :args ::sh-args
  :ret ::sh-result)
(s/fdef sh-async
  :args ::sh-async-args
  :ret nil?)
