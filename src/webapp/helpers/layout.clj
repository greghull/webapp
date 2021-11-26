(ns webapp.helpers.layout
  (:require [clojure.string :as str]
            [hiccup.page :refer [html5 include-css include-js]]
            [webapp.settings :refer [url-for]]))

(defn guest-menu []
  (list
   [:ul {:class "nav navbar-nav navbar-right"}
    [:li
     [:a {:class "login" :href (url-for :user-login)} "Login"]]
    [:li
     [:a {:class "Register" :href (url-for :user-signup)} "Register"]]]))

(defn user-menu [user]
  (list
   [:ul {:class "nav navbar-nav navbar-right"}
    [:li
     [:a {:class "profile" :href (url-for user)} (str (-> user :user/first-name) "'s Profile")]]
    [:li
     [:a {:class "logout" :href (url-for :user-logout)} "Logout"]]]))

(defn base [title & content]
  (html5
   [:head
    [:meta {:http-equiv "content-type" :content "text/html; charset=UTF-8"}]
    [:meta {:name "description" :content "Sample application"}]
    [:meta {:name "keywords" :content "images pictures"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:title title]
    [:link {:rel "icon" :href "/img/favicon.ico" :type "image/x-icon"}]
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css")
    (include-css "/css/site.css")
    (include-js "https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/js/bootstrap.min.js")
    (include-js "/js/site.js")]
   [:body content]))

(defn with-layout [& [req title content]]
  (base title
        [:header.navbar.navbar-default.navbar-static-top.navbar-default
         [:div.container
          [:div.navbar-header
           [:a.navbar-brand {:href "/"}
            [:strong "webapp"]]]
          (if (:user req)
            (user-menu (:user req))
            (guest-menu))]]
        (when (:flash req)
          [:div.container
           [:div.alert.alert-info.alert-dismissible {:role "info"} (:flash req)
            [:button.btn-close {:type "button" :data-bs-dismiss "alert" :aria-label "Close"}]]])
        (when (:error req)
          [:div.container
           [:div.alert.alert-danger.alert-dismissible {:role "info"} (:error req)
            [:button.btn-close {:type "button" :data-bs-dismiss "alert" :aria-label "Close"}]]])
        [:div.container content]))

(defn table [docs & {:keys [keys labels caption heading]}]
  [:div
   (when heading [:h2 heading])
   [:table.table.table-responsive.table-hover
    (when caption [:caption caption])
    [:thead
     [:tr
      (for [k keys] [:th (or (get labels k) (-> k str (str/replace ":" "") (str/replace "/" " ") str/capitalize))])]]
    [:tbody
     (for [doc docs]
         [:tr {:role "button" :onclick (str "window.location='" (url-for doc) "'")}
          (for [k keys] [:td (k doc)])])]]])

(defn not-found [& [request]]
  (with-layout request "Not Found"
    [:center
     [:h1 "Error 404"
      [:p "Page not found!"]]]))
