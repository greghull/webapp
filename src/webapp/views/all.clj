(ns webapp.views.all
  (:require  [webapp.db.core :as db]
             [webapp.helpers.titles :refer [title-for]]
             [webapp.helpers.layout :refer [with-layout table]]
             [webapp.handlers.core :refer [view-handler]]
             [webapp.handlers.table :refer [table-handler table-template document-list]]

             [webapp.views.edn]
             [webapp.views.user]
             [webapp.views.user-login]
             [webapp.views.user-signup]
             [webapp.views.user-password]))

;;; Table of All Documents methods
(defmethod table-template :all [req]
  (with-layout req "Documents"
    [:div
     (table (:handler/document-list req)
            :heading "Documents"
            :caption "List of Documents"
            :labels {:meta/id "ID"
                     :meta/type "Type"
                     title-for "Description"}
            :keys [:meta/id :meta/type title-for])]))

(defmethod document-list :all [req]
  (assoc req :handler/document-list (db/query)))

(defmethod view-handler :all [req]
  (table-handler req))
