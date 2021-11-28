(ns webapp.handlers.user-password
  (:require [ring.util.response :as response]
            [webapp.helpers.forms :refer [input submit-button form-html]]
            [webapp.helpers.layout :refer [with-layout]]
            [struct.core :as st]
            [webapp.db.user :as u]

            [webapp.handlers.guards :refer [require-login]]
            [webapp.handlers.core :refer [view-handler]]
            [webapp.handlers.form :as form]))


(def schema
  {:password
   {:label "Old Password"
    :validation [st/required]}
   :new-password
   {:label "New Password"
    :validation [st/required [st/min-count 8]]}
   :confirm-password
   {:label "Confirm New Password"
    :validation [st/required [st/identical-to :new-password]]}})

(defn template [req]
  (with-layout req "Change Your Password"
    [:div.login-form
     [:h2 "Change Password for " (-> req :user :user/first-name) " " (-> req :user :user/last-name)]
     (form-html req
                (input req :password)
                (input req :new-password)
                (input req :confirm-password)
                (submit-button "Change Password"))]))

(defn save [req]
  (if (-> req :handler/form :errors)
    req
    (if (u/update-password! (:user req)
                            (-> req :handler/form :data/cleaned :password)
                            (-> req :handler/form :data/cleaned :new-password))
      req
      (assoc-in req [:handler/form :errors :password] "Your old password is incorrect."))))

(defn success [_]
    (assoc (response/redirect "/")
           :flash "Your password has been updated."))

(defmethod view-handler :user-password [req]
  (-> req require-login (form/handler {:schema schema
                                       :template template
                                       :save save
                                       :success success})))