(ns webapp.handlers.form
  (:require [ring.util.response :as response]
            [webapp.helpers.titles :refer [title-for]]
            [webapp.helpers.forms :refer [input submit-button validate-form form-params form-html]]
            [webapp.helpers.layout :refer [with-layout]]
            [webapp.settings :as settings]))

(defn error? [req]
  (or (-> req :handler/form :errors not-empty) (req :error)))

(defn initial-data [req]
  (-> req :handler/form :data/initial))

(defn raw-data [req]
  (-> req :handler/form :data/raw))

(defn cleaned-data [req]
  (-> req :handler/form :data/cleaned))

(defn final-data [req]
  (-> req :handler/form :data/final))

(defn initial [req]
  (if-let [initial ((-> req :handler/form :initial) req)]
    (assoc-in req [:handler/form :data/initial] initial)
    nil))

(defn template [req]
  (with-layout req (title-for (initial-data req))
               [:div.profile-form
                (form-html req
                           [:h2 (title-for (initial-data req))]
                           (for [k (keys (-> req :handler/form :schema))]
                                  (input req k))
                           (submit-button "Save Changes"))]))

(defn validate [req]
  (let [raw-data (form-params req)
        initial-data (initial-data req)
        [errors cleaned-data] (validate-form (-> req :handler/form :schema) (merge initial-data raw-data))
        form (merge (:handler/form req) {:data/raw raw-data :errors errors
                                         :data/cleaned cleaned-data
                                         :data/final (merge initial-data cleaned-data)})]
    (assoc req :handler/form form)))

(defn success [req]
  (response/redirect (settings/url-for (:handler/view req))))

(defmulti render :request-method)
(defmethod render :get [req]
  ((-> req :handler/form :template) req))

(defmethod render :post [req]
  (if (error? req)
    ((-> req :handler/form :template) req)
    ((-> req :handler/form :success) req)))

(defmulti request-handler :request-method)

(defmethod request-handler :get [req]
  (some-> req
          initial
          render))

(defmethod request-handler :post [req]
  (some-> req
          initial
          ((-> req :handler/form :validate))
          ((-> req :handler/form :save))
          render))

(def form-defaults
  {:schema {}
   :template template
   :save (fn [req] req)
   :initial (fn [_] {})
   :validate validate
   :success success})

(defn handler [req form]
  (-> req
      (assoc :handler/form (merge form-defaults form))
      request-handler))
