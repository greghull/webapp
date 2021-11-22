(ns webapp.handlers.user-password
  (:require [ring.util.response :as response]
            [webapp.views.forms :as forms :refer [input submit-button form-html]]
            [webapp.views.layout :refer [with-layout]]
            [struct.core :as st]
            [webapp.db.user :as u]
            [webapp.handlers.docs :refer [type-handler document document-handler form view error? save]]))


(def password-change-form
  {:old-password
   {:label "Old Password"
    :validation [st/required]}
   :password
   {:label "New Password"
    :validation [st/required [st/min-count 8]]}
   :confirm-password
   {:label "Confirm New Password"
    :validation [st/required [st/identical-to :password]]}})

(defn password-change-view [req]
  (with-layout req "Change Your Password"
    [:div.login-form
     [:h2 "Change Password for " (-> req :user :user/first-name) " " (-> req :user :user/last-name)]
     (form-html req
                (input req :old-password)
                (input req :password)
                (input req :confirm-password)
                (submit-button "Change Password"))]))

(defmethod form :user-password [req]
  (assoc req :form {:schema password-change-form}))

(defmethod document :user-password [req]
  (assoc req :doc nil))

(defmethod save :user-password [req]
  (if (-> req :form :errors)
    req
    (if (u/update-password! (:user req)
                            (-> req :form :cleaned-data :old-password)
                            (-> req :form :cleaned-data :password))
      req
      (assoc-in req [:form :errors :old-password] "Your old password is incorrect."))))

(defmethod view [:get :user-password] [req]
  (password-change-view req))

(defmethod view [:post :user-password] [req]
  (if (error? req)
    (password-change-view req)
    (assoc (response/redirect "/")
           :flash "Your password has been updated.")))

(defmethod type-handler [:get :user-password] [req]
  (document-handler req))

(defmethod type-handler [:post :user-password] [req]
  (document-handler req))