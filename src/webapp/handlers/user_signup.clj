(ns webapp.handlers.user-signup
  (:require [ring.util.response :as response]
            [webapp.views.forms :refer [input submit-button form-html]]
            [webapp.views.layout :refer [with-layout]]
            [struct.core :as st]
            [webapp.db.core :as db]
            [buddy.hashers :as hashers]
            [webapp.db.validators :refer [does-not-exist]]
            [webapp.handlers.views :refer [view-handler]]
            [webapp.handlers.docs :refer [document document-handler form-schema render template success save]]))

(defmethod form-schema :user-signup [_]
  {:user/first-name
   {:label "Your First Name"
    :validation [st/required st/string]}
   :user/last-name
   {:label "Your Last Name"
    :validation [st/required st/string]}
   :user/email
   {:label "Email"
    :validation [st/required st/email [does-not-exist :user/email]]}
   :user/password
   {:label "Password"
    :validation [st/required [st/min-count 8]]}
   :user/confirm-password
   {:label "Confirm Password"
    :validation [st/required [st/identical-to :user/password]]}})

(defmethod template :user-signup [req]
  (with-layout req "User Registration"
    [:div.registration-form
     [:h2 "Register for a new account"]
     (form-html req
                (input req :user/first-name)
                (input req :user/last-name)
                (input req :user/email)
                (input req :user/password)
                (input req :user/confirm-password)
                (submit-button "Create Account"))]))

(defmethod document :user-signup [req]
    (assoc req :doc {:meta/type "user"}))

(defmethod save :user-signup [req]
  (if (-> req :form :errors)
    req
    (let [doc (-> req
                  :doc
                  (merge (-> req :form :cleaned-data))
                  (dissoc :user/confirm-password)
                  (assoc :user/password (hashers/derive (-> req :form :cleaned-data :user/password)))
                  (assoc :user/email-confirmed? false))]
      (try (assoc req :doc (db/put! doc))
           (catch Exception e (assoc req :error (ex-message e)))))))

(defmethod success :user-signup [req]
  (let [doc (:doc req)]
    (prn "!! DOC !!  " doc)
    (merge (response/redirect "/")
     {:flash (str "Welcome " (:user/first-name doc)
                " " (:user/last-name doc) ".  You are now registered.")
       :session {:id (:meta/id doc)}})))

(defmethod view-handler [:get :user-signup] [req]
  (document-handler req))

(defmethod view-handler [:post :user-signup] [req]
  (db/transaction (document-handler req)))