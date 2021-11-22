(ns webapp.db.core
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [cheshire.core :as json]
            [jdbc.pool.c3p0 :as pool]
            [mount.core :as mount]
            [webapp.db.ulid :refer [ulid]]))

(declare ^:dynamic *db*)
(mount/defstate ^:dynamic *db*
  :start (pool/make-datasource-spec
          {:classname   "org.sqlite.JDBC"
           :subprotocol "sqlite"
           :subname     "./testing.db"})
  :stop (.close *db*))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helper Functions

(defn key-to-field [k]
  (str "$." (json/generate-string k)))

(defn parse-doc [x]
  (some-> x :fields (json/parse-string true)))

(defn query-value [x]
  (if (keyword? x)
    (str/replace-first (str x) ":" "")
    x))

(defn do-in-transaction
  "Execute F inside a DB transaction. Prefer macro form `transaction` to using
   this directly."
  [f]
  (jdbc/with-db-transaction [tx *db*]
    (binding [*db* tx]
      (f))))

(defmacro transaction
  "Execute all queries within the body in a single transaction."
  {:arglists '([body] [options & body]), :style/indent 0}
  [& body]
  `(do-in-transaction (fn [] ~@body)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Database creation and deletion

(defn create-db! []
  (jdbc/with-db-transaction [tx *db*]
    (jdbc/execute! tx "CREATE TABLE IF NOT EXISTS docs 
                       (fields JSON, 
                        id TEXT UNIQUE NOT NULL AS (json_extract(fields, '$.meta/id')),
                        type TEXT AS (json_extract(fields, '$.meta/type')));")
    (jdbc/execute! tx "CREATE UNIQUE INDEX IF NOT EXISTS id_index ON docs(id);")
    (jdbc/execute! tx "CREATE INDEX IF NOT EXISTS type_index ON docs(type);")
    (jdbc/execute! tx "CREATE UNIQUE INDEX IF NOT EXISTS email_index 
                       ON docs(json_extract(fields, '$.user/email'));")))

(defn drop-db! []
  (jdbc/with-db-transaction [tx *db*]
    (jdbc/execute! tx "DROP TABLE IF EXISTS docs;")))

(defn reset-db! []
  (transaction
    (drop-db!)
    (create-db!)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; CRUD Operations

(defn delete!
  "Deletes the document from the database with the specified `id`."
  [id]
  (jdbc/execute! *db* ["DELETE FROM docs
                      WHERE id = ?"
                       id]))

(defn new-id
  "Generates a new id for the supplied document.   Uses the id supplied in
   the document if it's present and valid"
  [& [d]]
  (let [id (:meta/id d)]
    (cond
      (nil? id) (ulid)
      (and (string? id) (empty? (str/trim id))) (ulid)
      :else id)))

(defn put!
  "Adds the document `d` to the database.
  
  If a document with the same :meta/id already exists then that document is replaced,
  otherwise a new document is inserted.
  
  If no :meta/id is specified, then the document will be assigned a ulid id.
  
  If a document cannot be updated or inserted do to conflict then an exception is thrown,
  otherwise returns the document."
  [d]
  (let [doc (assoc d :meta/id (new-id d))
        json-string (json/generate-string doc)]
    (jdbc/execute! *db* ["INSERT INTO docs(fields)
                          VALUES(json(?))
                          ON CONFLICT (id)
                          DO UPDATE SET fields = excluded.fields"
                         json-string])
    doc))

(defn fetch
  "Returns the doc with the specified `id`, or the doc with the specified `id` and `type`.
   If `id` doesn't exist then returns nil."
  ([id]
   (-> *db*
       (jdbc/query ["SELECT fields FROM docs
                     WHERE id = ?
                     LIMIT 1"
                    id])
       first
       parse-doc))
  ([id type]
   (if (nil? type)
     (fetch id)
     (-> *db*
         (jdbc/query ["SELECT fields FROM docs
                       WHERE id = ? AND json_extract(fields, '$.meta/type') = ?
                       LIMIT 1"
                      id (query-value type)])
         first
         parse-doc))))


(defn query
  "Runs the simple query:
      WHERE `k` `op` `v`
   and returns are all matching rows.
   
   ex: (query db [:= :user/first-name \"Fred\"]) runs
       WHERE json_extract(fields, '$.user/firstname') = \"Fred\""

  ([[op k v]]
   (let [rows (jdbc/query *db* [(str "SELECT fields FROM docs 
                                      WHERE json_extract(fields, ?)" (name op) "?")
                                (key-to-field k) (query-value v)])]
     (map parse-doc rows)))
  ([]
   (let [rows (jdbc/query *db* ["SELECT fields FROM docs"])]
     (map parse-doc rows))))

(defn exists?
  "Returns true if a document exists with the specified key `k` = `v`"
  [k v]
  (some? (not-empty (jdbc/query *db*
                                [(str "SELECT id FROM docs
                                  WHERE json_extract(fields, ?) = ?
                                  LIMIT 1")
                                 (key-to-field k) v]))))
