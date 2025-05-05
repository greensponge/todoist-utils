(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'todoist-utils)
(def version "0.1.0")
(def jar-file (str "todoist-utils-" version ".jar"))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uberjar [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src"] :target-dir "target/classes"})
  (b/compile-clj {:basis (b/create-basis {}) :src-dirs ["src"] :class-dir "target/classes"})
  (b/uber {:class-dir "target/classes"
           :uber-file jar-file
           :basis (b/create-basis {})
           :main 'todoist-utils.core}))

