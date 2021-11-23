(ns webapp.handlers.user-login
  (:require [ring.util.response :as response]
            [struct.core :as st]
            [webapp.db.user :as u]
            [webapp.views.forms :refer [input submit-button form-html]]
            [webapp.views.layout :refer [with-layout]]
            [webapp.handlers.views :refer [view-handler]]
            [webapp.handlers.docs :refer [form-schema template document document-handler form success error? save]]))

(defmethod form-schema :user-login [_]
  {:email
   {:label "Your Email Address"
    :validation [st/required st/email]}
   :password
   {:label "Your Password"
    :validation [st/required]}})

(defmethod template :user-login [req]
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

(defmethod save :user-login [req]
  (if-let [user (u/auth-user (-> req :form :cleaned-data :email) (-> req :form :cleaned-data :password))]
    (assoc req :doc user)
    (assoc-in req [:form :errors :auth] "Invalid email or password")))

(defmethod success :user-login [req]
  (merge (response/redirect (or (-> req :session :referer) "/"))
         {:flash (str "Welcome back " (-> req :doc :user/first-name)
                      " " (-> req :doc :user/last-name) ".")
          :session (assoc (:session req) :id (-> req :doc :meta/id))}))

(defmethod view-handler [:get :user-login] [req]
  (document-handler req))

(defmethod view-handler [:post :user-login] [req]
  (document-handler req))

(defmethod view-handler [:get :user-logout] [_]
  (merge (response/redirect "/") {:session nil :flash "You are now logged out."}))