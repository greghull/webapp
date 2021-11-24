(ns webapp.handlers.core
  (:require [compojure.core :refer [GET ANY routes context]]
            [clojure.pprint :refer [pprint]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.logger :as logger]
            [mount.core :as mount]
            [webapp.settings :refer [settings]]
            [webapp.views.layout :refer [with-layout not-found]]
            [webapp.handlers.guards :refer [wrap-user wrap-guardian]]

            [webapp.handlers.document :refer [document-handler]]
            [webapp.handlers.view :refer [view-handler]]

            [webapp.handlers.all]
            [webapp.handlers.user]
            [webapp.handlers.user-login]
            [webapp.handlers.user-signup]
            [webapp.handlers.user-password]))

(defn assoc-view [req]
  (assoc req :handler/view (keyword (get-in req [:route-params :view]))))

(defn doc-routes []
  (routes
    (ANY "/:view" req
         (-> req assoc-view view-handler))
    (ANY "/:view/:id" req
         (-> req assoc-view document-handler))))

(defn define-routes []
  (routes
   (GET "/" req
     (with-layout req "Home Page"
       [:h1 "Hello World"]))

   (GET "/session" req
     (with-layout req "Your Session"
       [:div
        [:h1 "Your Session"]
        [:pre (with-out-str (pprint (:session req)))]]))

   (GET "/request" req
     (with-layout req "Your Request"
       [:div
        [:h1 "Your Request"]
        [:pre (with-out-str (pprint req))]]))

   (GET "/settings" req
     (with-layout req "Site Settings"
       [:div
        [:h1 "Active Site Settings"]
        [:pre (with-out-str (pprint settings))]]))

   (GET "/request/:testing" req
     (with-layout req "Your Request"
       [:div
        [:h1 "Your Request"]
        [:pre (with-out-str (pprint req))]]))

   (context (get settings :doc-root) []
     (doc-routes))
   
   (route/not-found (not-found))))

(defn define-app []
  (-> (define-routes)
      (wrap-user)
      (wrap-guardian)
      (logger/wrap-with-logger)
      (wrap-defaults site-defaults)))

(declare app)
(mount/defstate app
  :start (define-app)
  :stop nil)
