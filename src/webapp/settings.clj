(ns webapp.settings
  (:require [webapp.db.core :as db]
            [mount.core :as mount]))

(def default-settings {:port 3000
                       :doc-root "/db"
                       :auth-root "/auth"})

(declare ^:dynamic settings)
(mount/defstate settings
  :start (merge default-settings (db/fetch "settings"))
  :stop default-settings)