(ns webapp.handlers.middleware
  (:require [webapp.db.core :as db]))

(defn wrap-user [handler]
  (fn [request]
      (handler (assoc request :user (some-> request :session :id db/fetch)))))

;; (defn wrap-user-required [handler]
;;   (fn [request]
;;     (if (:user request)
;;       (handler request)
;;       (let [session (:session request)
;;             uri (:uri request)]
;;         (merge (response/redirect "/users/login") {:session (assoc session :target uri)
;;                                                    :flash "Please login to access that page."})))))
