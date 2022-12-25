(ns screeps.sourcemap (:require [clojure.string :as str]))

(def sourceMapConsumer (.-SourceMapConsumer (js/require "source-map")))
(def consumer (atom nil))

(def cache (atom {}))

(defn get-file-location [this]
  (if (.isNative this)
    "native"
    (let [file-name (.getScriptNameOrSourceURL this)
          line-number (.getLineNumber this)
          column-number (.getColumnNumber this)]
      (cond-> nil
              (and (not file-name) (.isEval this)) (str (.getEvalOrigin this) ", ")
              file-name (str file-name)
              (not file-name) (str "<anonymous>")
              line-number (str ":" line-number)
              (and line-number column-number) (str ":" column-number)))
    )
  )

(defn add-suffix [file-location s]
  (str s " (" file-location ")")
  )

(defn frame->string []
  (this-as this
    (let [
          file-location (get-file-location this)
          line ""
          function-name (.getFunctionName this)
          method-name (.getMethodName this)
          type-name (let [name (.getTypeName this)]
                      (if (= name "[object Object]") "null" name))
          is-constructor (.isConstructor this)
          is-method-call (not (or (.isToplevel this) is-constructor))]
      (cond
        is-method-call
        (add-suffix file-location
                    (if function-name
                      (cond-> line
                              (and type-name (not= 0 (.indexOf function-name type-name)))
                                (str type-name ".")
                              true
                                (str function-name)
                              (and method-name
                                   (not= (-
                                           (.-length function-name) (.-length method-name) 1)
                                         (.indexOf function-name (str "." method-name))))
                                (str " [as " method-name "]"))
                      (str line type-name "." (or method-name "<anonymous>"))
                      ))
        is-constructor (add-suffix file-location (str line "new " (or function-name "<anonymous>")))
        function-name (add-suffix file-location (str line function-name))
        :else (str line file-location))
      )
    )
  )

(defn copy-frame [frame]
  (let [res #js {}]
    (doseq [name (js/Object.getOwnPropertyNames
                   (js/Object.getPrototypeOf frame))]
      (if (= "function" (js/goog.typeOf (js/goog.object.get frame name)))
        (do
          (js/goog.object.set res name
                              (.bind (js/goog.object.get frame name) frame)))
        (js/goog.object.set res name
                            (js/goog.object.get frame name))))
    (set! (.-toString res) frame->string)
    res)
  )

(defn map-frame
  "docstring"
  [frame]
  (let [column (.getColumnNumber frame)
        line (.getLineNumber frame)
        original (.originalPositionFor @consumer
                                       (clj->js {:column column
                                                 :line   line}))
        frame-copy (copy-frame frame)
        ]
    (if (.-source original)
      (do (set! (.-getFileName frame-copy) (fn [] (.-source original)))
          (set! (.-getLineNumber frame-copy) (fn [] (.-line original)))
          (set! (.-getColumnNumber frame-copy) (fn [] (.-column original)))
          (set! (.-getScriptNameOrSourceURL frame-copy) (fn [] (.-source original)))
          frame-copy)
      (do
        frame))
    )
  )

(defn prepare-stacktrace
  [error stack]
  (let [original-trace (if (isa? js/Error error) (.toString stack) error)]
    (if-let [trace (get @cache original-trace)]
      trace
      (let [
            error-str (str (or (.-name error) "Error")
                           ": " (or (.-message error) ""))

            res (if @consumer
                  (str error-str "\n" (str " at " (str/join "\n at " (map map-frame stack))))
                  (str error-str "\n" (str/join "\n at " stack)))]
        (when @consumer (swap! cache assoc original-trace res))
        res
        )))
  )

(defn enable [src-map]
  (reset! consumer (sourceMapConsumer. src-map))
  (set! js/Error.prepareStackTrace prepare-stacktrace)
  )

