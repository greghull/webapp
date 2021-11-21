(ns webapp.handlers.user-login
  (:require [ring.util.response :as response]
            [webapp.users.core :as u]
            [webapp.forms.core :as forms :refer [input submit-button form-html]]
            [webapp.views.core :refer [with-layout]]
            [struct.core :as st]
            [webapp.handlers.docs :refer [type-handler document document-handler form view error? save]]))

(def login-form
  {:email
   {:label "Your Email Address"
    :validation [st/required st/email]}
   :password
   {:label "Your Password"
    :validation [st/required]}})

(defn login-view [req]
  (let [form (:form req)]
    (with-layout req "User Login"
      [:div.login-form
       [:h2 "Login with existing account"]
       (when-let [error (-> form :errors :auth)] [:div.alert.alert-danger error])
       (form-html req
                  (input req :email)
                  (input req :password)
                  (submit-button "Login"))])))

(defmethod document :user-login [req]
  (if (some? (get-in req [:route-params :id]))
    nil
    (assoc req :doc {:meta/type "user"})))

(defmethod form :user-login [req]
  (assoc req :form {:schema login-form}))

(defmethod save :user-login [req]
  (if-let [user (u/auth-user (-> req :form :cleaned-data :email) (-> req :form :cleaned-data :password))]
    (assoc req :doc user)
    (assoc-in req [:form :errors :auth] "Invalid email or password")))

(defmethod view [:get :user-login] [req]
  (login-view req))

(defmethod view [:post :user-login] [req]
  (if (error? req)
    (login-view req)
    (merge (response/redirect "/")
           {:flash (str "Welcome back " (-> req :doc :user/first-name)
                       " " (-> req :doc :user/last-name) ".")
            :session (assoc (:session req) :id (-> req :doc :meta/id))})))

(defmethod type-handler [:get :user-login] [req]
  (document-handler req))

(defmethod type-handler [:post :user-login] [req]
  (document-handler req))

(defmethod type-handler [:get :user-logout] [_]
  (merge (response/redirect "/") {:session nil :flash "You are now logged out."}))