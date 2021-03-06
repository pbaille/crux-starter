(ns scripts.clj-to-md
  (:require [clojure.string :as str]))

;; a very crude way to turn a commented clojure file into a markdown file

(defn blocks [s]
  (str/split s #"\n\n"))

(defn lines [s]
  (str/split s #"\n"))

(defn text-line? [s]
  (str/starts-with? s ";"))

(defn sep? [s]
  (re-matches #";+ *-+" s))

(defn remove-starting-semicolons [s]
  (str/replace s #";+ " ""))

(defmacro block-guard [name sym test]
  `(defn ~name [~sym]
     (if-let [tret# ~test]
       [~(keyword (clojure.core/name name))
        (if (true? tret#)
          ~sym
          tret#)])))

(block-guard text s
             (every? text-line? (lines s)))

(block-guard code s
             (some (complement text-line?) (lines s)))

(block-guard h1 s
             (let [[l1 l2 l3 & rs :as ls] (lines s)]
               (and (= 3 (count ls))
                    (sep? l3) (sep? l1)
                    (remove-starting-semicolons l2))))

(block-guard h2 s
             (let [[l1 l2 & rs :as ls] (lines s)]
               (and (= 2 (count ls))
                    (sep? l2)
                    (remove-starting-semicolons l1))))

(block-guard h3 s
             (and (= 1 (count (lines s)))
                  (if-let [[_ t] (re-matches #";+ (.+) -+" s)]
                    t)))

(defn title [x]
  (or (h1 x) (h2 x) (h3 x)))

(defn marked-block [s]
  (or (title s)
      (text s)
      (code s)))

(defn join-adjacent-code-blocks [marked-blocks]
  (->> marked-blocks
       (partition-by #(= :code (first %)))
       (mapcat (fn [xs] (if (= :code (ffirst xs))
                          [[:code (str/join "\n\n" (map second xs))]]
                          xs)))))

(defn clean-text-blocks [xs]
  (map (fn [[type content :as block]]
         (if (= :code type)
           block
           [type (remove-starting-semicolons content)]))
       xs))

(defn add-linebreaks [xs]
  (map (fn [[type content :as block]]
         (if (= type :text)
           [:text (str content "  \n")]
           block))
       xs))

(defn remove-text-blocks-inner-linebreaks [xs]
  (map (fn [[type content :as block]]
         (if (= type :text)
           [:text (apply str (lines content))]
           block))
       xs))

(defn to-md [marked-blocks]
  (str/join "\n"
            (keep (fn [[t c]]
                    (when-not (= t :sep)
                      (case t
                        :code (str "``` clojure \n" c "\n```")
                        :h1 (str "# " c)
                        :h2 (str "## " c)
                        :h3 (str "### " c)
                        :text c)))
                  marked-blocks)))

(defn clj->md [input output]
  (->> (slurp input)
       blocks
       (map marked-block)
       join-adjacent-code-blocks
       clean-text-blocks
       remove-text-blocks-inner-linebreaks
       add-linebreaks
       to-md
       (spit output)))

(defn emit-article []
  (as-> (slurp "doc/template.md") _
        (str/replace _ #"\{\{(.*)\}\}" (fn [[_ match]] (slurp match)))
        (spit "doc/article.md" _)))

(defn -main [& args]
  (println "compiling article.md")
  (clj->md "src/crux_starter/p00_setup.clj" "doc/setup.md")
  (clj->md "src/crux_starter/p01_transactions.clj" "doc/transactions.md")
  (clj->md "src/crux_starter/p02_queries.clj" "doc/queries.md")
  (emit-article))



(comment
  (-main)
  (h3 ";; yop ---")
  (->> (slurp "src/crux_starter/p01_transactions.clj")
       blocks
       (map marked-block)))