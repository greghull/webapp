(ns webapp.handlers.user-password
  (:require [ring.util.response :as response]
            [webapp.views.forms :refer [input submit-button form-html]]
            [webapp.views.layout :refer [with-layout]]
            [struct.core :as st]
            [webapp.db.user :as u]

            [webapp.handlers.guards :refer [require-login]]
            [webapp.handlers.view :refer [view-handler]]
            [webapp.handlers.form :refer [form-handler form-schema success template task]]))


(defmethod form-schema :user-password [_]
  {:password
   {:label "Old Password"
    :validation [st/required]}
   :new-password
   {:label "New Password"
    :validation [st/required [st/min-count 8]]}
   :confirm-password
   {:label "Confirm New Password"
    :validation [st/required [st/identical-to :new-password]]}})

(defmethod template :user-password [req]
  (with-layout req "Change Your Password"
    [:div.login-form
     [:h2 "Change Password for " (-> req :user :user/first-name) " " (-> req :user :user/last-name)]
     (form-html req
                (input req :password)
                (input req :new-password)
                (input req :confirm-password)
                (submit-button "Change Password"))]))

(defmethod task :user-password [req]
  (if (-> req :handler/form :errors)
    req
    (if (u/update-password! (:user req)
                            (-> req :handler/form :cleaned-data :password)
                            (-> req :handler/form :cleaned-data :new-password))
      req
      (assoc-in req [:handler/form :errors :old-password] "Your old password is incorrect."))))

(defmethod success :user-password [_]
    (assoc (response/redirect "/")
           :flash "Your password has been updated."))

(defmethod view-handler :user-password [req]
  (-> req require-login form-handler))