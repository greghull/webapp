(ns webapp.handlers.document
  (:require [webapp.db.core :as db]
            [webapp.handlers.form :as form]))

(defn initial [req]
  (let [id (get-in req [:route-params :id])
        type (:handler/view req)
        doc (if (or (nil? id) (= id "new"))
              {:meta/type (:handler/view req)}
              (db/fetch id type))]
    (when doc (-> req
                  (assoc-in [:handler/form :data/initial] doc)))))

(defn save [req]
  (if (form/error? req)
    req
    (try (assoc req :handler/doc (db/put! (form/final-data req)))
         (catch Exception e (assoc req :error (ex-message e))))))

(def form-defaults
  {:initial initial
   :save save})

(defn handler [req form]
  (form/handler req (merge form-defaults form)))