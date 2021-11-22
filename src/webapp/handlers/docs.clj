(ns webapp.handlers.docs
  (:require [ring.util.response :as response]
            [compojure.core :refer [ANY routes]]
            [clojure.pprint :refer [pprint *print-right-margin*]]
            [clojure.edn :as edn]
            [webapp.db.core :as db]
            [struct.core :as st]
            [webapp.utils.doc :refer [title-for]]
            [webapp.views.forms :refer [text-area input submit-button validate-form form-params form-html]]
            [webapp.views.layout :refer [with-layout table]]
            [webapp.handlers.guards :refer [require-user]]))

(def doc-form
  {:doc
   {:label "Edit Document"
    :widget text-area
    :style "height: 500px;"
    :validation [st/required]}})

(defn doc-view [req]
  (with-layout req "Edit Document"
    [:div.login-form
     [:h2 "Edit Document"]
     [:p "Your are editing this document in "
      [:a {:href "https://github.com/edn-format/edn"} "EDN"]
      " format.  Each document is saved to the databse in "
      [:a {:href "https://www.w3schools.com/whatis/whatis_json.asp"} "JSON"]
      " format so some type information may be lost."]
     (form-html req
                (input req :doc)
                (submit-button "Save"))]))

(defn error? [req]
  (or (-> req :form :errors) (req :error)))

(defn doc-as-str [d]
  (with-out-str (binding [*print-right-margin* 80] (pprint d))))

(defmulti default-document #(-> % :route-params :type keyword))
(defmethod default-document :default [req]
  {:meta/type (get-in req [:route-params :type])})

(defmulti document #(-> % :route-params :type keyword))
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

(defmulti form #(-> % :route-params :type keyword))
(defmethod form :edn [req]
    (assoc req :form {:schema doc-form :initial (:doc req)}))

(defmulti validate #(-> % :route-params :type keyword))
(defmethod validate :default [req]
  (let [raw-data (form-params req)
        [errors cleaned-data] (validate-form (-> req :form :schema) (merge (:doc req) raw-data))
        form (merge (:form req) {:raw-data raw-data :errors errors :cleaned-data cleaned-data})]
    (assoc req :form form)))

(defmulti save #(-> % :route-params :type keyword))
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

(defmulti view (fn [req] [(:request-method req) (-> req :route-params :type keyword)]))

(defmethod view [:get :edn] [req]
  (doc-view req))

(defmethod view [:post :edn] [req]
  (if (error? req)
    (doc-view req)
    (assoc (response/redirect (str "/db/" (-> req :route-params :type))) 
           :flash (str "Your changes to doc #" (:meta/id (:doc req))  " have been saved."))))

(defmulti document-handler
  (fn [req] [(:request-method req) (-> req :route-params :type keyword)]))

(defmethod document-handler :default [req]
  (let [recover (get-method document-handler [(:request-method req) ::default])]
    ;; Prevent infinite loop:
    (if (and recover (not (= (get-method document-handler :default) recover)))
      (recover req)
      :default)))

(defmethod document-handler [:get ::default] [req]
  (some-> req
          document
          form
          view))

(defmethod document-handler [:post ::default] [req]
  (some-> req
          document
          form
          validate
          save
          view))

(defmulti document-list #(-> % :route-params :type keyword))
(defmethod document-list :default [req]
  (let [type (-> req :route-params :type)
        docs (db/query [:= :meta/type type])]
    (assoc req :document-list docs)))

(defmethod document-list :edn [req]
    (assoc req :document-list (db/query)))

(defmulti list-view #(-> % :route-params :type keyword))

(defmethod list-view :default [req]
  (with-layout req "Documents"
    [:div
     (table (:document-list req)
            :heading "Documents"
            :caption "List of Documents"
            :labels {:meta/id "ID"
                     :meta/type "Type"
                     title-for "Description"}
            :keys [:meta/id :meta/type title-for])]))

(defmulti type-handler
  (fn [req] [(:request-method req) (-> req :route-params :type keyword)]))

(defmethod type-handler :default [req]
  (let [recover (get-method type-handler [(:request-method req) ::default])]
    ;; Prevent infinite loop:
    (if (and recover (not (= (get-method type-handler :default) recover)))
      (recover req)
      :default)))

(defmethod type-handler [:get ::default] [req]
  (some-> req
          require-user
          document-list
          list-view))

(defn doc-routes []
  (routes
   (ANY "/:type" request
     (type-handler request))
   (ANY "/:type/:id" request
     (document-handler request))))

