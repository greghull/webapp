(ns webapp.handlers.user-login
  (:require [ring.util.response :as response]
            [struct.core :as st]
            [webapp.db.user :as u]
            [webapp.views.forms :refer [input submit-button form-html]]
            [webapp.views.layout :refer [with-layout]]
            [webapp.handlers.view :refer [view-handler]]
            [webapp.handlers.form :refer [form-handler form-schema form success
                                          error? task template]]))

(defmethod form-schema :user-login [_]
  {:email
   {:label "Your Email Address"
    :validation [st/required st/email]}
   :password
   {:label "Your Password"
    :validation [st/required]}})

(defmethod template :user-login [req]
  (with-layout req "User Login"
    [:div.login-form
     [:h2 "Login with existing account"]
     (when-let [error (-> req :handler/form :errors :auth)]
       [:div.alert.alert-danger error])
     (form-html req
                (input req :email)
                (input req :password)
                (submit-button "Login"))]))

(defmethod task :user-login [req]
  (if-let [user (u/auth-user (-> req :handler/form :cleaned-data :email)
                             (-> req :handler/form :cleaned-data :password))]
    (assoc req :user user)
    (assoc-in req [:handler/form :errors :auth] "Invalid email or password")))

(defmethod success :user-login [req]
  (merge (response/redirect (or (-> req :session :referer) "/"))
         {:flash (str "Welcome back " (-> req :user :user/first-name)
                      " " (-> req :user :user/last-name) ".")
          :session (assoc (:session req) :id (-> req :user :meta/id))}))

(defmethod view-handler :user-login [req]
  (form-handler req))

(defmethod view-handler :user-logout [_]
  (merge (response/redirect "/") {:session nil :flash "You are now logged out."}))