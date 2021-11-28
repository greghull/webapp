(ns webapp.db.user
  (:require
   [webapp.db.core :as db]
   [buddy.hashers :as hashers]))

(defn verify-password [user guess]
  (some-> guess (hashers/verify (:user/password user)) :valid))

(defn auth-user
  "Searches the database for a user with the specified `email` address and `password`.
   Returns the found user or nil."
  [email password]
  (when-let [user (first (db/query [:= :user/email email]))]
    (when (verify-password user password)
      user)))

(defn update-password!
  "Given a User `user`, assigns them a new password of `password` if `old-password` matches
   their current password.  Returns true on success and nil on failure."
  [user old-password password]
  (when (verify-password user old-password)
    (db/put! (assoc user :user/password (hashers/derive password)))))
