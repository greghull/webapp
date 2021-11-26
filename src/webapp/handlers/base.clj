(ns webapp.handlers.base
  (:require [webapp.views.layout :refer [with-layout not-found]]))

(defmulti template :handler/view)

(defmethod template :default [_]
  nil)

(defmethod template :example [req]
  (with-layout req "Documents"
               [:div
                [:h2 "Base Handler"]
                [:p "This handler is for :handler/view "
                 (str (:handler/view req))]]))

(defmulti base-handler
          (fn [req] [(:request-method req) (:handler/view req)]))

(defmethod base-handler :default [req]
  (let [recover (get-method base-handler [(:request-method req) ::default])]
    ;; Prevent infinite loop:
    (if (and recover (not (= (get-method base-handler :default) recover)))
      (recover req)
      :default)))

(defmethod base-handler [:get ::default] [req]
  (-> req
      template))