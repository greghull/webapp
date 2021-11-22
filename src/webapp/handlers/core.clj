(ns webapp.handlers.core
  (:require [compojure.core :refer [GET routes context]]
            [clojure.pprint :refer [pprint]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.logger :as logger]
            [webapp.handlers.docs :refer [doc-routes]]
            [mount.core :as mount]
            [webapp.settings :refer [settings]]
            [webapp.views.layout :refer [with-layout not-found]]
            [webapp.handlers.middleware :refer [wrap-user]]

            [webapp.handlers.user]
            [webapp.handlers.user-login]
            [webapp.handlers.user-signup]
            [webapp.handlers.user-password]))

(defn define-routes []
  (routes
   (GET "/" request
     (with-layout request "Home Page"
       [:h1 "Hello World"]))

   (GET "/session" request
     (with-layout request "Your Session"
       [:div
        [:h1 "Your Session"]
        [:pre (with-out-str (pprint (:session request)))]]))

   (GET "/request" request
     (with-layout request "Your Request"
       [:div
        [:h1 "Your Request"]
        [:pre (with-out-str (pprint request))]]))

   (GET "/settings" request
     (with-layout request "Site Settings"
       [:div
        [:h1 "Active Site Settings"]
        [:pre (with-out-str (pprint settings))]]))

   (GET "/request/:testing" request
     (with-layout request "Your Request"
       [:div
        [:h1 "Your Request"]
        [:pre (with-out-str (pprint request))]]))

   (context (get settings :doc-root) []
     (doc-routes))
   
   (route/not-found (not-found))))

(defn define-app []
  (-> (define-routes)
      (wrap-user)
      (logger/wrap-with-logger)
      (wrap-defaults site-defaults)))

(declare app)
(mount/defstate app
  :start (define-app)
  :stop nil)