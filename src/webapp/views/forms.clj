(ns webapp.views.forms
  (:require [clojure.string :as str]
            [struct.core :as st]
            [hiccup.form :as hiccup]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn keys-to-name
  "Given a key returns a name suitable as a name for a form element"
  [k]
  (str/replace-first (str k) ":" ""))

(defn input-class [error]
  (if error
    "is-invalid input form-control"
    "input form-control"))

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

(defn default-widget
  "Tries to guess what widget to use for the field `k` in the given `form`.
   Defaults to a plain text-field."
  [form k]
  (let [validators (flatten (-> form :schema k :validation))]
    (or
     (when (some #{st/email} validators) email-field)
     (when (.contains (name k) "password") password-field)
     text-field)))

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
  (let [form (:form req)
        widget (or (-> form :schema k :widget) (default-widget form k))
        label (or (-> form :schema k :label) (str/capitalize (name k)))
        value (or (-> form :raw-data k) (-> form :initial k))
        style (-> form :schema k :style)
        error (-> form :errors k)
        required? (some #{st/required} (flatten (-> form :schema k :validation)))]
    (if required?
      (widget {:placeholder label :required true :style style} k value error)
      (widget {:placeholder label :style style} k value error))))

(defmacro form-html
  [req & body]
  `(hiccup/form-to [:post (:uri ~req)]
                        (anti-forgery-field)
                        ~@body))

(defn submit-button [label]
  (hiccup/submit-button {:class "btn btn-primary"} label))
