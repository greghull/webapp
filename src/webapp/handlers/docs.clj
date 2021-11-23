(ns webapp.handlers.docs
  (:require [ring.util.response :as response]
            [compojure.core :refer [ANY routes]]
            [clojure.pprint :refer [pprint *print-right-margin*]]
            [clojure.edn :as edn]
            [webapp.db.core :as db]
            [struct.core :as st]
            [webapp.views.helpers :refer [title-for]]
            [webapp.views.forms :refer [text-area input submit-button validate-form form-params form-html]]
            [webapp.views.layout :refer [with-layout table]]
            [webapp.handlers.guards :refer [require-user require-admin]]))

(defn assoc-type [req]
  (assoc req ::type (keyword (get-in req [:route-params :type]))))

(defmulti form-schema ::type)
(defmethod form-schema :edn [_]
  {:doc
   {:label "Edit Document"
    :widget text-area
    :style "height: 500px;"
    :validation [st/required]}})

(defmulti template ::type)
(defmethod template :edn [req]
  (with-layout req "Edit Document"
    [:div.login-form
     [:h2 "Edit Document"]
     [:p "Your are editing this document in "
      [:a {:href "https://github.com/edn-format/edn"} "EDN"]
      " format.  Each document is saved to the database in "
      [:a {:href "https://www.w3schools.com/whatis/whatis_json.asp"} "JSON"]
      " format so some type information may be lost."]
     (form-html req
                (input req :doc)
                (submit-button "Save"))]))

(defn error? [req]
  (or (-> req :form :errors not-empty) (req :error)))

(defn doc-as-str [d]
  (with-out-str (binding [*print-right-margin* 80] (pprint d))))

(defmulti default-document ::type)
(defmethod default-document :default [req]
  {:meta/type (get-in req [:route-params :type])})

(defmulti document ::type)
(defmethod document :default [req]
  (let [id (get-in req [:route-params :id])
        type (get-in req [:route-params :type])
        doc (if (or (nil? id) (= id "new"))
              (default-document req)
              (db/fetch id type))]
    (when doc (assoc req :doc doc))))

(defmethod document :edn [req]
  (let [id (get-in req [:route-params :id])
        doc (if (= id "new")
              (default-document :default)
              (db/fetch id))]
    (when doc (assoc req :doc {:doc (doc-as-str (into (sorted-map) doc))}))))

(defmulti form ::type)
(defmethod form :default [req]
    (assoc req :form {:schema (form-schema req) :initial (:doc req)}))

(defmulti validate ::type)
(defmethod validate :default [req]
  (let [raw-data (form-params req)
        [errors cleaned-data] (validate-form (-> req :form :schema) (merge (:doc req) raw-data))
        form (merge (:form req) {:raw-data raw-data :errors errors :cleaned-data cleaned-data})]
    (assoc req :form form)))

(defmulti save ::type)
(defmethod save :edn [req]
    (if (-> req :form :errors)
      req
      (if-let [doc (-> req :form :cleaned-data :doc edn/read-string)]
        (try (assoc req :doc (db/put! doc))
             (catch Exception e (assoc req :error (ex-message e))))
        (assoc req :error "Document cannot be blank!"))))

(defmethod save :default [req]
    (if (-> req :form :errors)
      req
      (let [doc (merge (:doc req) (-> req :form :cleaned-data))]
        (try (assoc req :doc (db/put! doc))
             (catch Exception e (assoc req :error (ex-message e)))))))


(defmulti success ::type)
(defmethod success :default [req]
  (assoc (response/redirect (webapp.settings/url-for (::type req)))
    :flash (str "Your changes to doc #" (:meta/id (:doc req))  " have been saved.")))

(defmulti render (fn [req] [(:request-method req) (::type req)]))

(defmethod render :default [req]
  (let [recover (get-method render [(:request-method req) ::default])]
    ;; Prevent infinite loop:
    (if (and recover (not (= (get-method render :default) recover)))
      (recover req)
      :default)))

(defmethod render [:get ::default] [req]
  (template req))

(defmethod render [:post ::default] [req]
  (if (error? req)
    (template req)
    (success req)))

(defmulti document-handler
  (fn [req] [(:request-method req) (::type req)]))

(defmethod document-handler :default [req]
  (let [recover (get-method document-handler [(:request-method req) ::default])]
    ;; Prevent infinite loop:
    (if (and recover (not (= (get-method document-handler :default) recover)))
      (recover req)
      :default)))

(defmethod document-handler [:get ::default] [req]
  (some-> req
          assoc-type
          document
          form
          render))

(defmethod document-handler [:post ::default] [req]
  (some-> req
          assoc-type
          document
          form
          validate
          save
          render))

