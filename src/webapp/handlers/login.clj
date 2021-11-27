(ns webapp.handlers.login
  (:require [ring.util.response :as response]
            [struct.core :as st]
            [webapp.db.user :as u]
            [webapp.views.forms :refer [input submit-button form-html]]
            [webapp.views.layout :refer [with-layout]]
            [webapp.handlers.view :refer [view-handler]]
            [webapp.handlers.form2 :as form]))

(def schema
  {:email
   {:label      "Your Email Address"
    :validation [st/required st/email]}
   :password
   {:label      "Your Password"
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
  (if-let [user (u/auth-user (-> req :handler/form :cleaned-data :email)
                             (-> req :handler/form :cleaned-data :password))]
    (assoc req :user user)
    (assoc-in req [:handler/form :errors :auth] "Invalid email or password")))

(defn success [req]
  (merge (response/redirect (or (-> req :session :referer) "/"))
         {:flash (str "Welcome back " (-> req :user :user/first-name)
                      " " (-> req :user :user/last-name) ".")
          :session (assoc (:session req) :id (-> req :user :meta/id))}))

(defmethod view-handler :login [req]
  (form/handler req {:schema schema
                          :template template
                          :save save
                          :success success}))