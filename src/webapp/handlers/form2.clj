(ns webapp.handlers.form2
  (:require [ring.util.response :as response]
            [compojure.core :refer [ANY routes]]
            [clojure.pprint :refer [pprint *print-right-margin*]]
            [webapp.views.helpers :refer [title-for]]
            [webapp.views.forms :refer [text-area input submit-button validate-form form-params form-html]]
            [webapp.views.layout :refer [with-layout table]]
            [webapp.handlers.guards :refer [require-login require-admin]]))

(defn error? [req]
  (or (-> req :handler/form :errors not-empty) (req :error)))

; TODO rename this to inital and rename form/initial to form/initial-data
; then from will have :raw-data :initial-data and :cleaned-data keys
; or better yet :data/clean :data/initial :data/raw
(defn get-initial [req]
  (assoc-in req [:handler/form :initial] nil))

(defn validate [req]
  (let [raw-data (form-params req)
        [errors cleaned-data] (validate-form (-> req :handler/form :schema) (merge (:handler/initial req) raw-data))
        form (merge (:handler/form req) {:raw-data raw-data :errors errors :cleaned-data cleaned-data})]
    (assoc req :handler/form form)))

(defn success [req]
  (response/redirect (webapp.settings/url-for (:handler/view req))))

(defmulti render :request-method)

(defmethod render :get [req]
  (template req))

(defmethod render :post [req]
  (if (error? req)
    (template req)
    (success req)))

(defmulti request-handler :request-method)

(defmethod request-handler :get [req]
  (some-> req
          get-initial
          render))

(defmethod request-handler :post [req]
  (some-> req
          get-initial
          validate
          task
          render))

(def form-defaults
  {:schema {}
   :template nil
   :save (fn [req] req)
   :get-initial (fn [req] req)
   :validate validate
   :success success})

(defn form-handler [req form]
  (-> req
      (assoc :handler/form (merge form-defaults form))
      request-handler))