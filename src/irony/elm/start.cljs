(ns irony.elm.start
  "Functions and configuation for starting a basic Elm application."
  (:refer-clojure :exclude [update])
  (:require
    [irony.elm :as elm]
    [schema.core :as s]
    [quiescent.core :as q])
  (:import
    (goog.async AnimationDelay)))

(defn make
  [root target]
  (let [{:keys [model action init update view]} (elm/as-record root)
        validate-model! (s/validator model)
        validate-action! (s/validator action)
        state (atom (validate-model! init))
        is-dirty? (atom true)
        commit! (fn [action]
                  (println action)
                  (let [old-state @state
                        new-state (update (validate-action! action) old-state)]
                    (when-not (identical? new-state old-state)
                      (validate-model! new-state)
                      (reset! is-dirty? true)
                      (reset! state new-state))))
        try-render! (fn []
                      (if @is-dirty?
                        (do
                          (q/render (view @state commit!) target)
                          (reset! is-dirty? false)
                          true)
                        false))
        updater (atom nil)
        stop! (fn []
                (when-let [^AnimationDelay timer @updater]
                  (.stop timer)
                  (.dispose timer)
                  (reset! updater nil)))
        start-fn (atom nil)
        start! (fn []
                 (stop!)
                 (try-render!)
                 (reset! updater (doto (AnimationDelay. @start-fn)
                                   .start)))]

    (reset! start-fn start!) ; Tie the recursive loop!

    {:state state
     :updater updater
     :is-dirty? is-dirty?

     :try-render! try-render!
     :start! start!
     :stop! stop!}))