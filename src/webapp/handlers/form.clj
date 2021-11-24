(ns webapp.handlers.form
  (:require [ring.util.response :as response]
            [compojure.core :refer [ANY routes]]
            [clojure.pprint :refer [pprint *print-right-margin*]]
            [webapp.views.helpers :refer [title-for]]
            [webapp.views.forms :refer [text-area input submit-button validate-form form-params form-html]]
            [webapp.views.layout :refer [with-layout table]]
            [webapp.handlers.guards :refer [require-login require-admin]]))

(defn error? [req]
  (or (-> req :handler/form :errors not-empty) (req :error)))

(defmulti template :handler/view)

(defmulti form-schema :handler/view)

(defmulti form :handler/view)
(defmethod form :default
  ([req]
    (assoc req :handler/form {:schema (form-schema req) :initial (:handler/doc req)})))

(defmulti validate :handler/view)
(defmethod validate :default [req]
  (let [raw-data (form-params req)
        [errors cleaned-data] (validate-form (-> req :handler/form :schema) (merge (:handler/doc req) raw-data))
        form (merge (:handler/form req) {:raw-data raw-data :errors errors :cleaned-data cleaned-data})]
    (assoc req :handler/form form)))

(defmulti task :handler/view)
(defmethod task :default [req]
    req)

(defmulti success :handler/view)
(defmethod success :default [req]
  (response/redirect (webapp.settings/url-for (:handler/view req))))

(defmulti render (fn [req] [(:request-method req) (:handler/view req)]))

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

(defmulti form-handler
          (fn [req] [(:request-method req) (:handler/view req)]))

(defmethod form-handler :default [req]
  (let [recover (get-method form-handler [(:request-method req) ::default])]
    ;; Prevent infinite loop:
    (if (and recover (not (= (get-method form-handler :default) recover)))
      (recover req)
      :default)))

(defmethod form-handler [:get ::default] [req]
  (some-> req
          form
          render))

(defmethod form-handler [:post ::default] [req]
  (some-> req
          form
          validate
          task
          render))

