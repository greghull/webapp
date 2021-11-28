(ns webapp.handlers.table
  (:require [webapp.helpers.titles :refer [title-for]]
            [webapp.helpers.layout :refer [with-layout table]]
            [webapp.db.core :as db]))


(defmulti table-template :handler/view)
(defmethod table-template :default [req]
  (with-layout req "Documents"
    [:div
     (table (:handler/document-list req)
            :heading "Documents"
            :caption "List of Documents"
            :labels {:meta/id "ID"
                     :meta/type "Type"
                     title-for "Description"}
            :keys [:meta/id :meta/type title-for])]))

(defmulti document-list :handler/view)
(defmethod document-list :default [req]
  (let [docs (db/query [:= :meta/type (:handler/view req)])]
    (assoc req :handler/document-list docs)))

(defmulti table-handler :handler/view)
(defmethod table-handler :default [req]
  (-> req
      document-list
      table-template))
