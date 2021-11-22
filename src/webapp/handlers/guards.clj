(ns webapp.handlers.guards
  (:require [ring.util.response :as response]
            [webapp.db.core :as db]
            [webapp.settings :refer [settings url-for]]))

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
      (catch clojure.lang.ExceptionInfo e
        (prn "!! CAUGHT !!" e)
        (merge (response/redirect (or (-> e ex-data :redirect) "/"))
               {:flash (ex-message e)
                :session (assoc (:session req) :referer (-> e ex-data :referer))})))))

(defn require-user [req]
  (if (nil? (:user req))
    (throw
     (ex-info "You must login to access this page."
              {:redirect (url-for :user-login)
               :referer (:uri req)}))
    req))

(defn require-owner [_])

(defn require-admin [req]
  (let [user (-> req require-user :user)]
    (if (some #{(:user/email user)} (:admins settings))
      req
      (ex-info "You do not have permission to access this page."
               {:redirect "/"}))))

