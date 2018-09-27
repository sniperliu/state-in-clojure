(ns example
  (:require
   [taoensso.timbre :as timbre
    :refer [debug]])
  (:refer-clojure :exclude [update]))

;; var
(def ^:dynamic *some-number* 10)

(comment
  (dotimes [n 100]
    @(future
       (binding [*some-number* n]
         (debug "number: " *some-number*))))

  (dotimes [n 100]
    @(future
       (binding [*some-number* nil]
         (set! *some-number* n)
         (debug "number set!: " *some-number*)))))

;; atom
(def DB (atom {}))

(defn update
  ([k v]
   (update DB k v))
  ([db k v]
   (swap! db assoc k v)))

(defn insert
  ([k v]
   (update k v))
  ([db k v]
   (update db k v)))

(defn delete
  ([k]
   (delete DB k))
  ([db k]
   (swap! db dissoc k)))

(comment
  (dotimes [n 100]
    @(future
       (insert n n)))

  @DB

  (let [[old new] (swap-vals! (atom {:a 1 :b 2}) dissoc :a)]
    (debug "old: " old)
    (debug "new: " new)))

;; ref
(def balance-a (ref 1000))
(def balance-b (ref 400))

(defn transit
  [balance1 balance2 amount]
  (dosync
    (when (>= @balance1 amount)
      (alter balance1 - amount)
      (alter balance2 + amount))))

(defn transit'
  [balance1 balance2 amount]
  (dosync
    (when (>= @balance1 amount)
      (commute balance1 - amount)
      (commute balance2 + amount))))

(defn transit''
  [balance1 balance2 amount]
  (dosync
    (ensure balance1)
    (when (>= @balance1 amount)
      (commute balance1 - amount)
      (commute balance2 + amount))))

(comment
  (transit'' balance-a balance-b 100)

  @balance-a
  (deref balance-b)

  (transit' balance-a balance-b 100))

;; agent
(def pipeline (agent 0
                :error-mode :fail))
                ;; :validator #(>= % 0)))

(defn throw-error
  [v]
  (throw (ex-info "On purpose"
           {:reason :on-purpose})))

(comment
  (send pipeline throw-error)

  (agent-error pipeline)

  (restart-agent pipeline 0)

  (send pipeline inc)

  @pipeline)


;; Validator
(comment
  (set-validator! pipeline #(>= % 0))

  (send pipeline dec)

  (agent-error pipeline))

;; Reactive
(comment
  (add-watch pipeline :demo
    (fn [k _ old new]
      (debug "old: " old)
      (debug "new: " new)))

  (send pipeline (constantly :a)))
