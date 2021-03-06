(ns webapp.helpers.forms
  (:require
   [clojure.string :as str]
   [hiccup.form :as hiccup]
   [ring.util.anti-forgery :refer [anti-forgery-field]]
   [struct.core :as st]))

(defn keys-to-name
  "Given a key returns a name suitable as a name for a form element"
  [k]
  (str/replace-first (str k) ":" ""))

(defn input-class [error]
  (if error
    "is-invalid input form-control"
    "input form-control"))

(defn check-box-class [error]
  (if error
    "is-invalid form-check-input"
    "form-check-input"))

(defn check-box
  [& [attr-map? ks value error]]
  (let [name (keys-to-name ks)]
    [:div.form-check.mb-3
     (hiccup/check-box (assoc attr-map? :class (str (check-box-class error) " " (:class attr-map?)))
                         name (= true value) "true")
     [:label.form-check-label {:for name} (:placeholder attr-map?)]
     [:div.invalid-feedback error]]))


(defn email-field
  [& [attr-map? ks value error]]
  (let [name (keys-to-name ks)]
    [:div.form-floating.mb-3
     (hiccup/email-field (assoc attr-map? :class (str (input-class error) " " (:class attr-map?)))
                         name value)
     [:div.invalid-feedback error]
     [:label {:for name} (:placeholder attr-map?)]]))

(defn password-field
  [& [attr-map? ks value error]]
  (let [name (keys-to-name ks)]
    [:div.form-floating.mb-3
     (hiccup/password-field (assoc attr-map? :class (str (input-class error) " " (:class attr-map?)))
                            name value)
     [:div.invalid-feedback error]
     [:label {:for name} (:placeholder attr-map?)]]))

(defn text-field
  [& [attr-map? ks value error]]
  (let [name (keys-to-name ks)]
    [:div.form-floating.mb-3
     (hiccup/text-field (assoc attr-map? :class (str (input-class error) " " (:class attr-map?)))
                        name value)
     [:div.invalid-feedback error]
     [:label {:for name} (:placeholder attr-map?)]]))

(defn text-area
  [& [attr-map? ks value error]]
  (let [name (keys-to-name ks)]
    [:div.form-floating.mb-3
     (hiccup/text-area (assoc attr-map? :class (str (input-class error) " " (:class attr-map?)))
                       name value)
     [:div.invalid-feedback error]
     [:label {:for name} (:placeholder attr-map?)]]))

(defn drop-down
    [& [attr-map? ks value error options]]
  (let [name (keys-to-name ks)]
    [:div.form-floating.mb-3
     (hiccup/drop-down (assoc attr-map? :class (str (input-class error) " " (:class attr-map?)))
                        name options value)
     [:div.invalid-feedback error]
     [:label {:for name} (:placeholder attr-map?)]]))

(defn default-widget
  "Tries to guess what widget to use for the field `k` in the given `form`.
   Defaults to a plain text-field."
  [form k]
  (let [validators (flatten (-> form :schema k :validation))]
    (or
     (when (some #{st/email} validators) email-field)
     (when (.contains (name k) "password") password-field)
     (when (some #{st/member} validators) drop-down)
     (when (some #{st/boolean-str} validators) check-box)
     text-field)))

(defn options [v]
  (let [coll (apply concat v)   ;; flatten list one level
        idx (.indexOf coll st/member)]
    (if (>= idx 0)
      (nth coll (inc idx))
      nil)))

(defn form-schema [form]
  (into {} (for [[k v] form] [k (:validation v)])))

(defn validate-form [form raw-data]
  (st/validate raw-data (form-schema form) {:strip true}))

(defn form-params
  "Takes the form-params in a request and converts it to a map accessed by keywords.
   ex. {\"a\" 1 \"b\" 2} maps to -> {:a 1 :b 2}"
  [request]
  (reduce (fn [acc [k v]] (assoc acc (keyword k) v)) {} (:form-params request)))

(defn input [req k]
  (let [form (:handler/form req)
        widget (or (-> form :schema k :widget) (default-widget form k))
        label (or (-> form :schema k :label) (str/capitalize (name k)))
        value (or (-> form :data/raw k) (-> form :data/initial k))
        style (-> form :schema k :style)
        opts (options (-> form :schema k :validation))
        error (-> form :errors k)
        required? (some #{st/required} (flatten (-> form :schema k :validation)))
        attr-map (if required?
                   {:placeholder label :required true :style style}
                   {:placeholder label :style style})]
    (if opts
      (widget attr-map k value error opts)
      (widget attr-map k value error))))

(defmacro form-html
  [req & body]
  `(hiccup/form-to [:post (:uri ~req)]
                        (anti-forgery-field)
                        ~@body))

(defn submit-button [label]
  (hiccup/submit-button {:class "btn btn-primary"} label))
