(ns webapp.handlers.all
  (:require [clojure.pprint :refer [pprint *print-right-margin*]]
            [clojure.edn :as edn]
            [webapp.db.core :as db]
            [struct.core :as st]
            [webapp.views.helpers :refer [title-for]]
            [webapp.views.forms :refer [text-area input submit-button validate-form form-params form-html]]
            [webapp.views.layout :refer [with-layout table]]
            [webapp.handlers.guards :refer [require-login require-admin]]

            [webapp.handlers.table :refer [table-template document-list]]
            [webapp.handlers.form :refer [form-schema template error?]]
            [webapp.handlers.document :refer [default-document document save]]))

;;; Methods for editing the EDN source of any document in a form

(defmethod form-schema :all [_]
  {:doc/text
   {:label "Edit Document"
    :widget text-area
    :style "height: 500px;"
    :validation [st/required]}})

(defmethod template :all [req]
  (with-layout req "Edit Document"
    [:div.login-form
     [:h2 "Edit Document"]
     [:p "Your are editing this document in "
      [:a {:href "https://github.com/edn-format/edn"} "EDN"]
      " format.  Each document is saved to the database in "
      [:a {:href "https://www.w3schools.com/whatis/whatis_json.asp"} "JSON"]
      " format so some type information may be lost."]
     (form-html req
                (input req :doc/text)
                (submit-button "Save"))]))

(defn doc-as-str [d]
  (with-out-str (binding [*print-right-margin* 80] (pprint d))))

(defmethod default-document :all [_]
  {:meta/type "unknown"})

(defmethod document :all [req]
  (let [id (-> req :route-params :id)
        doc (if (= id "new")
              (default-document req)
              (db/fetch id))]
    (when doc (assoc req :handler/doc {:doc/text (doc-as-str (into (sorted-map) doc))}))))

(defmethod save :all [req]
    (if (error? req)
      req
      (if-let [doc (-> req :handler/form :cleaned-data :doc/text edn/read-string)]
        (try (assoc req :handler/doc (db/put! doc))
             (catch Exception e (assoc req :error (ex-message e))))
        (assoc req :error "Document cannot be blank!"))))


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