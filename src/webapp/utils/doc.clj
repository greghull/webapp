(ns webapp.utils.doc
  (:require [webapp.settings :refer [settings]]))

(def doc-titles (atom {}))
(def doc-handlers (atom {}))
(def list-handlers (atom {}))
(def new-doc-handlers (atom {}))

(defn default-doc-title [d]
  (str "A "  (:meta/type d)  " Document"))

(defn title-for [d]
  ((get @doc-titles (:meta/type d) default-doc-title) d))

(defn url-for
  "Returns the URL for accessing a document `d`.
   If `d` is a map, look for the :meta/id key, otherwise assume that `d` is an id."
  [d]
  (if (map? d)
    (str (get settings :doc-root) "/"  (or (:meta/type d) "doc")   "/" (:meta/id d))
    (str (get settings :doc-root) "/doc/edn/" d)))