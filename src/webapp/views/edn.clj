(ns webapp.views.edn
  (:require [clojure.pprint :refer [pprint *print-right-margin*]]
            [clojure.edn :as edn]
            [webapp.db.core :as db]
            [struct.core :as st]
            [webapp.helpers.forms :refer [text-area input submit-button form-html]]
            [webapp.helpers.layout :refer [with-layout]]

            [webapp.handlers.core :refer [id-handler]]
            [webapp.handlers.document :as document]
            [webapp.handlers.form :as form]))

;;; Methods for editing the EDN source of any document in a form

(def schema
  {:doc/text
   {:label "Edit Document"
    :widget text-area
    :style "height: 500px;"
    :validation [st/required]}})

(defn template [req]
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

(defn initial [req]
  (let [id (get-in req [:route-params :id])
        doc (if (or (nil? id) (= id "new"))
              {:meta/type "unknown"}
              (db/fetch id))]
    (when doc {:doc/text (doc-as-str doc)})))

(defn save [req]
  (if (form/error? req)
    req
    (if-let [doc (-> req form/cleaned-data :doc/text edn/read-string)]
      (try (assoc req :handler/doc (db/put! doc))
           (catch Exception e (assoc req :error (ex-message e))))
      (assoc req :error "Document cannot be blank!"))))

(defmethod id-handler :all [req]
  (document/handler req {:schema schema
                         :save save
                         :initial initial
                         :template template}))
