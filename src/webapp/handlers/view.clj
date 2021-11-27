(ns webapp.handlers.view
  (:require [webapp.handlers.table :refer [table-handler]]))

(defmulti view-handler :handler/view)
(defmulti document-handler :handler/view)

(defmethod view-handler :default [req]
  (table-handler req))