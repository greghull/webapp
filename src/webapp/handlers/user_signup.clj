(ns webapp.handlers.user-signup
  (:require [ring.util.response :as response]
            [webapp.forms.core :as forms :refer [input submit-button form-html]]
            [webapp.views.core :refer [with-layout]]
            [struct.core :as st]
            [webapp.db.core :as db]
            [buddy.hashers :as hashers]
            [webapp.db.validators :refer [does-not-exist]]
            [webapp.handlers.docs :refer [type-handler document document-handler form view error? save]]))

(def signup-form
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

(defn signup-view [req]
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
  (if (some? (get-in req [:route-params :id]))
    nil
    (assoc req :doc {:meta/type "user"})))

(defmethod form :user-signup [req]
  (assoc req :form {:schema signup-form}))

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

(defmethod view [:get :user-signup] [req]
  (signup-view req))

(defmethod view [:post :user-signup] [req]
  (if (error? req)
    (signup-view req)
    (assoc (response/redirect "/")
           :flash (str "Welcome " (-> req :doc :user/first-name)
                       " " (-> req :doc :user/last-name) ".  You are now registered."))))

(defmethod type-handler [:get :user-signup] [req]
  (document-handler req))

(defmethod type-handler [:post :user-signup] [req]
  (db/transaction (document-handler req)))