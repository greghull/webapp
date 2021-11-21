(ns webapp.users.middleware
  (:require [ring.util.response :as response]
            [webapp.db.core :as db]))

(def ^:dynamic *user* nil)

(defn wrap-user [handler]
  (fn [request]
    (binding [*user* (some-> request :session :id db/fetch)]
      (handler request))))

(defn wrap-user-required [handler]
  (fn [request]
    (if (:user request)
      (handler request)
      (let [session (:session request)
            uri (:uri request)]
        (merge (response/redirect "/users/login") {:session (assoc session :target uri)
                                                   :flash "Please login to access that page."})))))
