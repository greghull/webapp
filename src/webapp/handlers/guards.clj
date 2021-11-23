(ns webapp.handlers.guards
  (:require [ring.util.response :as response]
            [webapp.db.core :as db]
            [webapp.settings :refer [settings url-for]])
  (:import (clojure.lang ExceptionInfo)))

(defn wrap-user
  "Ring middleware to add a :user document to a request."
  [handler]
  (fn [req]
    (handler (assoc req :user (some-> req :session :id db/fetch)))))

(defn wrap-guardian
  "Ring middleware to catch exceptions thrown by guardian require functions
  and redirect to the appropriate error or login page"
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch ExceptionInfo e
        (merge (response/redirect (or (-> e ex-data :redirect) "/"))
               {:flash (ex-message e)
                :session (assoc (:session req) :referer (-> e ex-data :referer))})))))

(defn user? [req]
  (some? (:user req)))

(defn admin? [req]
  (some? (some #{(some-> req :user :user/email)} (:admins settings))))

(defn require-user [req]
  (if (user? req)
    req
    (throw
      (ex-info "You must login to access this page."
               {:redirect (url-for :user-login)
                :referer  (:uri req)}))))

(defn require-owner [_])

(defn require-admin [req]
    (if (and (require-user req) (admin? req))
      req
      (throw
        (ex-info "You do not have permission to access this page."
               {:redirect "/"}))))

