(ns webapp.handlers.user-signup
  (:require [ring.util.response :as response]
            [webapp.views.forms :refer [input submit-button form-html]]
            [webapp.views.layout :refer [with-layout]]
            [struct.core :as st]
            [webapp.db.core :as db]
            [buddy.hashers :as hashers]
            [webapp.db.validators :refer [does-not-exist]]
            [webapp.handlers.view :refer [view-handler]]
            [webapp.handlers.form :refer [error? form-schema render template success]]
            [webapp.handlers.document :refer [document document-handler save]]))

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

(defmethod save :user-signup [req]
  (if (error? req)
    req
    (let [doc (-> req
                  :handler/doc
                  (merge (-> req :handler/form :cleaned-data))
                  (dissoc :user/confirm-password)
                  (assoc :user/password (hashers/derive (-> req :handler/form :cleaned-data :user/password)))
                  (assoc :user/email-confirmed? false))]
      (try (assoc req :handler/doc (db/put! doc))
           (catch Exception e (assoc req :error (ex-message e)))))))

(defmethod success :user-signup [req]
  (let [doc (:handler/doc req)]
    (merge (response/redirect "/")
     {:flash (str "Welcome " (:user/first-name doc)
                " " (:user/last-name doc) ".  You are now registered.")
       :session {:id (:meta/id doc)}})))

(defmethod view-handler :user-signup [req]
  (document-handler req))

