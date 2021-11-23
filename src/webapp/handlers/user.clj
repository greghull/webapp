(ns webapp.handlers.user
  (:require [ring.util.response :as response]
            [webapp.views.forms :refer [input submit-button form-html]]
            [webapp.views.layout :refer [with-layout table]]
            [struct.core :as st]
            [webapp.settings :refer [url-for]] ;[webapp.handlers.views :refer [render]]]
            [webapp.handlers.docs :refer [template document-handler form-schema success error?]]
            [webapp.db.core :refer [transaction]]
            [webapp.db.validators :refer [unique-to]]))

(defmethod form-schema :user [_]
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

(defmethod template :user [req]
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

(defmethod success :user [req]
  (assoc (response/redirect (url-for :user))
    :flash (str "Your changes to " (-> req :doc :user/first-name)
                " " (-> req :doc :user/last-name) "'s profile have been saved.")))


(defmethod document-handler [:post :user]
;;   "User document handler needs to be run in a transaction to make sure there
;; isn't a race condition between checking if an email address is already in 
;; use and letting the user claim that email address."
  [req]
  (let [handler (get-method document-handler [:post ::default])]
    (transaction (handler req))))


;(defmethod render :user [req]
;  (with-layout req "User List"
;    [:div
;     (table (:document-list req)
;            :heading "Users"
;            :caption "List of Users"
;            :labels {:user/first-name "First Name"
;                     :user/last-name "Last Name"}
;            :keys [:user/first-name :user/last-name :user/email :address/zip])]))
