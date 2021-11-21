(ns webapp.users.core
  (:require
   [webapp.db.core :as db]
   [buddy.hashers :as hashers]))

(defn auth-user
  "Searches the database for a user with the specified `email` address and `password`.
   Returns the found user or nil."
  [email password]
  (when-let [user (first (db/query [:= :user/email email]))]
    (when (hashers/verify password (:user/password user))
      user)))

(defn update-password!
  "Given a User `user`, assigns them a new password of `password` if `old-password` matches
   their current password.  Returns true on success and nil on failure."
  [user old-password password]
  (when (hashers/verify old-password (:user/password user))
    (db/put! (assoc user :user/password (hashers/derive password)))))