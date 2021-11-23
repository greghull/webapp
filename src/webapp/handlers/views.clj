(ns webapp.handlers.views
  (:require [webapp.handlers.guards :refer [require-user require-admin]]
            [webapp.views.helpers :refer [title-for]]
            [webapp.views.forms :refer [text-area input submit-button validate-form form-params form-html]]
            [webapp.views.layout :refer [with-layout table]]
            [webapp.db.core :as db]))


(defn assoc-type [req]
  (assoc req ::type (keyword (get-in req [:route-params :type]))))


(defmulti document-list ::type)
(defmethod document-list :default [req]
  (let [type (-> req :route-params :type)
        docs (db/query [:= :meta/type type])]
    (assoc req :document-list docs)))

(defmethod document-list :edn [req]
  (assoc req :document-list (db/query)))


(defmulti render ::type)

(defmethod render :default [req]
  (with-layout req "Documents"
               [:div
                (table (:document-list req)
                       :heading "Documents"
                       :caption "List of Documents"
                       :labels {:meta/id "ID"
                                :meta/type "Type"
                                title-for "Description"}
                       :keys [:meta/id :meta/type title-for])]))


(defn base-view-handler [req]
  (some-> req
          assoc-type
          document-list
          render))

(defn user-view-handler [req]
  (some-> req
          require-user
          base-view-handler))

(defn admin-view-handler [req]
  (some-> req
          require-user
          require-admin
          base-view-handler))


(defmulti view-handler
          (fn [req] [(:request-method req) (-> req :route-params :type keyword)]))

(defmethod view-handler :default [req]
  (let [recover (get-method view-handler [(:request-method req) ::default])]
    ;; Prevent infinite loop:
    (if (and recover (not (= (get-method view-handler :default) recover)))
      (recover req)
      :default)))

(defmethod view-handler [:get ::default] [req]
  (admin-view-handler req))