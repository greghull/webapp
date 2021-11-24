(ns webapp.handlers.table
  (:require [webapp.handlers.guards :refer [require-login require-admin]]
            [webapp.views.helpers :refer [title-for]]
            [webapp.views.forms :refer [text-area input submit-button validate-form form-params form-html]]
            [webapp.views.layout :refer [with-layout table]]
            [webapp.db.core :as db]))


(defmulti table-template :handler/view)
(defmethod table-template :default [_]
  nil)

(defmulti document-list :handler/view)
(defmethod document-list :default [req]
  (let [docs (db/query [:= :meta/type (:handler/view req)])]
    (assoc req :handler/document-list docs)))

(defmulti table-handler :handler/view)
(defmethod table-handler :default [req]
  (-> req
      document-list
      table-template))