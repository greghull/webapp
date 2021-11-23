(ns webapp.handlers.user-password
  (:require [ring.util.response :as response]
            [webapp.views.forms :refer [input submit-button form-html]]
            [webapp.views.layout :refer [with-layout]]
            [struct.core :as st]
            [webapp.db.user :as u]
            [webapp.handlers.views :refer [view-handler]]
            [webapp.handlers.docs :refer [document document-handler form-schema success template save]]))


(defmethod form-schema :user-password [_]
  {:old-password
   {:label "Old Password"
    :validation [st/required]}
   :password
   {:label "New Password"
    :validation [st/required [st/min-count 8]]}
   :confirm-password
   {:label "Confirm New Password"
    :validation [st/required [st/identical-to :password]]}})

(defmethod template :user-password [req]
  (with-layout req "Change Your Password"
    [:div.login-form
     [:h2 "Change Password for " (-> req :user :user/first-name) " " (-> req :user :user/last-name)]
     (form-html req
                (input req :old-password)
                (input req :password)
                (input req :confirm-password)
                (submit-button "Change Password"))]))

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

(defmethod success :user-password [_]
    (assoc (response/redirect "/")
           :flash "Your password has been updated."))

(defmethod view-handler [:get :user-password] [req]
  (document-handler req))

(defmethod view-handler [:post :user-password] [req]
  (document-handler req))