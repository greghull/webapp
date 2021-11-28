(ns webapp.views.user-login
  (:require [ring.util.response :as response]
            [struct.core :as st]
            [webapp.db.user :as u]
            [webapp.helpers.forms :refer [input submit-button form-html]]
            [webapp.helpers.layout :refer [with-layout]]
            [webapp.handlers.core :refer [view-handler]]
            [webapp.handlers.form :as form]))

(def schema
  {:email
   {:label "Your Email Address"
    :validation [st/required st/email]}
   :password
   {:label "Your Password"
    :validation [st/required]}})

(defn template [req]
  (with-layout req "User Login"
    [:div.login-form
     [:h2 "Login with existing account"]
     (when-let [error (-> req :handler/form :errors :auth)]
       [:div.alert.alert-danger error])
     (form-html req
                (input req :email)
                (input req :password)
                (submit-button "Login"))]))

(defn save [req]
  (if-let [user (u/auth-user (-> req form/cleaned-data :email)
                             (-> req form/cleaned-data :password))]
    (assoc req :user user)
    (assoc-in req [:handler/form :errors :auth] "Invalid email or password")))

(defn success [req]
  (merge (response/redirect (or (-> req :session :referer) "/"))
         {:flash (str "Welcome back " (-> req :user :user/first-name)
                      " " (-> req :user :user/last-name) ".")
          :session (assoc (:session req) :id (-> req :user :meta/id))}))

(defmethod view-handler :user-login [req]
  (form/handler req {:schema schema
                     :template template
                     :save save
                     :success success}))

(defmethod view-handler :user-logout [_]
  (merge (response/redirect "/") {:session nil :flash "You are now logged out."}))