(ns webapp.users.core
  (:require
   [webapp.db.core :as db]
   [crypto.password.bcrypt :as password]))

(defn auth-user
  "Searches the database for a user with the specified `email` address and `password`.
   Returns the found user or nil."
  [email password]
  (when-let [user (first (db/query [:= :user/email email]))]
    (when (password/check password (:user/password user))
      user)))

(defn update-password!
  "Given a User `user`, assigns them a new password of `password` if `old-password` matches
   their current password.  Returns true on success and nil on failure."
  [user old-password password]
  (when (password/check old-password (:user/password user))
    (db/put! (assoc user :user/password (password/encrypt password)))))