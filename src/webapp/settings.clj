(ns webapp.settings
  (:require
   [mount.core :as mount]
   [webapp.db.core :as db]))

(def default-settings {:port 3000
                       :doc-root "/db"})

(declare ^:dynamic settings)
(mount/defstate settings
  :start (merge default-settings (db/fetch "settings"))
  :stop default-settings)

(defn url-for
  "Returns a URL for the given object.  If `x` is a keyword, returns a url for the associated
   type handler.  If x is a document returns a url to the doc."
  [x]
  (cond
    (keyword? x) (str (get settings :doc-root) "/" (name x))
    (map? x) (str (get settings :doc-root) "/"  (or (:meta/type x) "edn")   "/" (:meta/id x))))

