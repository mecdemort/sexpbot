(ns sexpbot.plugins.haskell
  (:use [sexpbot registry]
        [clojure.contrib.json :only [read-json]]
	[clojure-http.client :only [add-query-params]]
        [clojure.java.shell :only [sh]]
        [sexpbot.gist :only [trim-with-gist]])
  (:require [clojure-http.resourcefully :as res]))

(def tryurl "http://tryhaskell.org/haskell.json")

(defn cull [js]
  (if-let [result (seq (:result js))] result (:error js)))

(def cap 300)

(defn trim [s]
  (trim-with-gist cap "output.hs" s))

(defn eval-haskell [expr]
  (->> (res/get (add-query-params tryurl {"method" "eval" "expr" expr}))
       :body-seq
       first
       read-json
       cull
       (apply str)))

(defn mueval [expr]
  (:out (sh "mueval" "-e" expr)))

(defn ghc-type [expr]
  (->> expr (str ":t ") (sh "ghc" "-e") :out (str "Type: ")))

(defn heval-cmd
  "Build a function suitable for use as a plugin's :cmd key, using the specified haskell evaluation function."
  [evaluator]
  (fn [{:keys [bot args] :as com-m}]
    (send-message
     com-m
     (str (when (= :irc (:protocol @bot)) "\u27F9 ")
          (->> args
               (interpose " ")
               (apply str)
               evaluator)))))

(defplugin
  (:cmd
   "Evaluates some Haskell code. Doesn't print error messages and uses the TryHaskell API."
   #{"tryhaskell"} 
   (heval-cmd eval-haskell))

  (:cmd
   "Evaluates Haskell code with mueval."
   #{"heval" "he"}
   (heval-cmd mueval))

  (:cmd
   "Gets the type of an expression via GHC's :t."
   #{"htype" "ht"}
   (heval-cmd ghc-type)))