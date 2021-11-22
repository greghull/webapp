(ns webapp.handlers.user
  (:require [ring.util.response :as response]
            [webapp.views.forms :refer [input submit-button form-html]]
            [webapp.views.layout :refer [with-layout table]]
            [struct.core :as st]
            [webapp.handlers.docs :refer [document-handler form list-view view error?]]
            [webapp.db.core :refer [transaction]]
            [webapp.db.validators :refer [unique-to]]))

(def profile-form
  {:meta/id
   {:validation [st/required]}
   :user/first-name
   {:label "Your First Name"
    :validation [st/required]}
   :user/last-name
   {:label "Your Last Name"
    :validation [st/required]}
   :user/email
   {:label "Email"
    :validation [st/required st/email [unique-to :meta/id :user/email]]}
   :address/street-1
   {:label "Street Address Line 1"
    :validation [st/required]}
   :address/street-2
   {:label "Street Address Line 2"
    :validation [st/string]}
   :address/city
   {:label "City"
    :validation [st/required]}
   :address/state
   {:label "State"
    :validation [st/required]}
   :address/zip
   {:label "Zip Code"
    :validation [st/required [st/min-count 5]]}})

(defn profile [req]
  (with-layout req "User Profile"
    [:div.profile-form
     (form-html req
                [:h2 "User Profile"]
                (input req :user/first-name)
                (input req :user/last-name)
                (input req :user/email)

                [:h2 "Address"]
                (input req :address/street-1)
                (input req :address/street-2)
                [:div.row
                 [:div.col-lg-5 (input req :address/city)]
                 [:div.col-lg-3 (input req :address/state)]
                 [:div.col-lg-4 (input req :address/zip)]]

                (submit-button "Save Changes"))]))

(defmethod form :user [req]
  (assoc req :form {:schema profile-form :initial (:doc req)}))

(defmethod view [:get :user] [req]
  (profile req))

(defmethod view [:post :user] [req]
  (if (error? req)
    (profile req)
    (assoc (response/redirect (str "/db/" (-> req :route-params :type)))
           :flash (str "Your changes to " (-> req :doc :user/first-name)
                       " " (-> req :doc :user/last-name) "'s profile have been saved."))))

(defmethod list-view :user [req]
  (with-layout req "User List"
    [:div
     (table (:document-list req)
            :heading "Users"
            :caption "List of Users"
            :labels {:user/first-name "First Name"
                     :user/last-name "Last Name"}
            :keys [:user/first-name :user/last-name :user/email :address/zip])]))

(defmethod document-handler [:post :user]
  "User document handler needs to be run in a transaction to make sure there
isn't a race condition between checking if an email address is already in 
use and letting the user claim that email address."
  [req]
  (let [handler (get-method document-handler [:post ::default])]
    (transaction (handler req))))
