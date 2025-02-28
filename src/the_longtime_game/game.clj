(ns the-longtime-game.game
  (:gen-class)
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [the-longtime-game.core :as core]
            [the-longtime-game.repl :as repl]
            [the-longtime-game.text :as text]
            [the-longtime-game.help :as help]))

(def save-path-re #"longtime_save_(.+)\.edn")

(defn save-path
  [path]
  (str "longtime_save_" path ".edn"))

(defn save-game
  [herd path]
  (spit (save-path path) herd))

(defn load-game
  [path]
  (edn/read-string (slurp (save-path path))))

(defn delete-game
  [path]
  (io/delete-file (save-path path)))

(defn end-game
  [{:keys [spirit]}]
  (println
   (text/wrap-quote-text
    "After a year of growling bellies, the herd disbands.
     Starvation terrifies the masses, and the stampede that once encompassed you
     now spills across the land, unmarshaled, uncontrolled.
     Adults and children alike flee to seek more fecund ways
     among other herds. The shared dream you embody shatters.
     Eternity welcomes the wreckage of you back into its folds,
     to be resewn into new visions and stranger futures
     for another, luckier, pluckier herd.
     [GAME OVER]"))
  (delete-game spirit))

(defn new-game
  [& {:keys [forbidden]
      :or {forbidden #{}}}]
  (println
   (text/wrap-quote-text
    "Are you there, some enduring era
     of peace and plenty?
     Hard winters and bare summers
     have weakened our ways
     and scattered our herd.
     Only a few dozen of us remain now to walk
     the ancient path.
     I believe in us, you must understand!
     I believe in old age and young laughter,
     in the strength of all that we might share.
     I dream of homes among the stars,
     of a long time for me, for us all.
     Are you out there?
     I believe in you.
     I pray to you:
     tell me your name."
    :width text/default-width))
  (let [spirit (repl/await-text nil "What shall the herd call you?" :forbidden forbidden :default "Longtime")
        herd (core/gen-herd :spirit spirit)]
    (println
     (text/wrap-quote-text
      (str
       "A new thread of fate is woven in the name of the "
       spirit "!")
      :width 50))
    (save-game herd spirit)
    (println
     (text/quote-text
      (str "Game saved as " (save-path spirit))
      :prefix "?"))
    (println (help/introduction herd))
    (repl/await-confirmation herd)
    herd))

(defn prompt-for-game
  [saves]
  (let [new-game-s "[new game]"
        spirit (repl/select-from-options nil "Select a game to load" (cons new-game-s saves))]
    (if (= spirit new-game-s)
      (new-game :forbidden (set saves))
      (load-game spirit))))

(defn launch-game
  []
  (if-let [saves
           (->> (file-seq (io/file "."))
                (map #(re-matches save-path-re (.getName %)))
                (filter some?)
                (map second)
                seq)]
    (prompt-for-game saves)
    (new-game)))

(defn game-loop
  [{:keys [spirit] :as herd}]
  (reduce
   (fn [herd _]
     (if-let [herd* (repl/do-month herd)]
       (do
         (save-game herd* spirit)
         herd*)
       (do
         (end-game herd)
         (reduced nil))))
   herd
   (range)))

(defn -main [& _]
  (println help/credits)
  (game-loop (launch-game)))
