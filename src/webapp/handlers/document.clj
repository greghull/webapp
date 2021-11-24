(ns webapp.handlers.document
  (:require [webapp.db.core :as db]
            [webapp.handlers.form :refer [template success render validate task form]]))

;; TODO Should the document validate method merge the cleaned-data to
;; :handler/doc?  That probably makes more sense than doing that in
;; the save method

(defn error? [req]
  (or (-> req :handler/form :errors not-empty) (req :error)))

(defmulti default-document :handler/view)
(defmethod default-document :default [req]
  {:meta/type (:handler/view req)})

(defmulti document :handler/view)
(defmethod document :default [req]
  (let [id (get-in req [:route-params :id])
        type (:handler/view req)
        doc (if (or (nil? id) (= id "new"))
              (default-document req)
              (db/fetch id type))]
    (when doc (assoc req :handler/doc doc))))

(defmulti save :handler/view)
(defmethod save :default [req]
  (if (-> req :handler/form :errors)
    req
    (let [doc (merge (:handler/doc req) (-> req :handler/form :cleaned-data))]
      (try (assoc req :handler/doc (db/put! doc))
           (catch Exception e (assoc req :error (ex-message e)))))))

(defmulti document-handler
          (fn [req] [(:request-method req) (:handler/view req)]))

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
          render))

(defmethod document-handler [:post ::default] [req]
  (db/transaction
    (some-> req
            document
            form
            validate
            task
            save
            render)))

