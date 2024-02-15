(ns mycc.p2p.ui
  (:require
   [bloom.commons.fontawesome :as fa]
   [clojure.string :as string]
   [modulo.api :as mod]
   [mycc.common.ui :as ui]
   [mycc.p2p.styles :as styles]
   [mycc.p2p.util :as util]))

(defn popover-view
  [content]
  [:div.info
   [fa/fa-question-circle-solid]
   [:div.popover
    content]])

(defn max-limit-preferences-view []
  [:<>
   [ui/row
    {:title "Max pairing sessions per day"}
    [ui/input {:type "number"
               :value @(mod/subscribe [:user-profile-value :user/max-pair-per-day])
               :min 1
               :max 24
               :on-change (fn [e]
                            (mod/dispatch [:set-user-value!
                                           :user/max-pair-per-day
                                           (js/parseInt (.. e -target -value) 10)]))}]]
   [ui/row
    {:title "Max pairing sessions per week"}
    [ui/input {:type "number"
               :value @(mod/subscribe [:user-profile-value :user/max-pair-per-week])
               :min 1
               :max (* 24 7)
               :on-change (fn [e]
                            (mod/dispatch [:set-user-value!
                                           :user/max-pair-per-week
                                           (js/parseInt (.. e -target -value) 10)]))}]]

   [ui/row
    {:title "Max pairing sessions with same person in a week"
     :info "The algorithm will try to maximize the variety of partners you pair with, but may still pair you several times with the same person. This setting lets you limit the maximum number of times you will be scheduled with the same person in a given week."}
    [ui/input {:type "number"
               :value @(mod/subscribe [:user-profile-value :user/max-pair-same-user])
               :min 1
               :max 50
               :on-change (fn [e]
                            (mod/dispatch [:set-user-value!
                                           :user/max-pair-same-user
                                           (js/parseInt (.. e -target -value) 10)]))}]]])

(defn next-day-of-week
  "Calculates next date with day of week as given"
  [now target-day-of-week]
  (let [target-day-of-week ({:monday 1
                             :tuesday 2
                             :wednesday 3
                             :thursday 4
                             :friday 5
                             :saturday 6
                             :sunday 0} target-day-of-week)
        now-day-of-week (.getDay now)
        ;; must be a nice way to do this with mod
        ;; (mod (- 7 now-day-of-week) 7)
        delta-days (get (zipmap (range 0 7)
                                (take 7 (drop (- 7 target-day-of-week)
                                              (cycle (range 7 0 -1)))))
                        now-day-of-week)
        new-date (doto (js/Date. (.valueOf now))
                   (.setDate (+ delta-days (.getDate now))))]
    new-date))

(defn add-days [day delta-days]
  (doto (js/Date. (.valueOf day))
    (.setDate (+ delta-days (.getDate day)))))

(defn format-date [date]
  (.format (js/Intl.DateTimeFormat. "en-US" #js {:weekday "long"
                                                 :month "short"
                                                 :day "numeric"})
           date))

(defn pair-with-view []
  (when (= :role/student @(mod/subscribe [:user-profile-value :user/role]))
    [ui/row
     {:title "Pair with..."}
     [ui/radio-list
      {:choices [[:pair-with/only-mentors "Mentors Only"]
                 [:pair-with/prefer-mentors "Mentors Preferred"]
                 [nil "No Preference"]
                 [:pair-with/prefer-students "Students Preferred"]
                 [:pair-with/only-students "Students Only"]]
       :value @(mod/subscribe [:user-profile-value :user/pair-with])
       :direction :vertical
       :on-change (fn [value]
                    (mod/dispatch [:set-user-value! :user/pair-with value]))}]]))

(defn availability-view []
  [ui/row
   {:title "Availability"
    :info [:<>
           [:div "Click in the calendar grid below to indicate your time availability."]
           [:div "A = available, P = preferred"]]}
   (when-let [availability @(mod/subscribe [:user-profile-value :user/availability])]
     [:table.availability {:tw "mt-4"}
      [:thead
       [:tr
        [:th]
        (let [next-monday (next-day-of-week (js/Date.) :monday)]
          (for [[i day] (map-indexed (fn [i d] [i d]) util/days)]
            (let [[day-of-week date] (string/split (format-date (add-days next-monday i)) #",")]
              ^{:key day}
              [:th.day
               [:div.day-of-week day-of-week]
               [:div.date date]])))]]
      [:tbody
       (doall
        (for [hour util/hours]
          ^{:key hour}
          [:tr
           [:td.hour
            hour]
           (doall
            (for [day util/days]
              ^{:key day}
              [:td
               (let [value (availability [day hour])]
                 [:button
                  {:class (case value
                            :preferred "preferred"
                            :available "available"
                            nil "empty")
                   :on-click (fn [_]
                               (mod/dispatch [:set-availability!
                                              [day hour]
                                              (case value
                                                :preferred nil
                                                :available :preferred
                                                nil :available)]))}
                  [:div.wrapper
                   (case value
                     :preferred "P"
                     :available "A"
                     nil "")]])]))]))]])])

(defn opt-in-view []
  [ui/row
   {:title "Opt-in for pairing next week?"
    :info [:div
           [:div "Do you want to participate this week?"
            [:div "(You need to opt-in every week)"]]]
    :featured? true}
   [ui/radio-list
    {:choices [[true "Yes"]
               [false "No"]]
     :value @(mod/subscribe [:user-profile-value :user/pair-next-week?])
     :on-change (fn [value]
                  (mod/dispatch [:opt-in-for-pairing! value]))}]])

(defn time-zone-view []
  [ui/row
   {:title "Time Zone"
    :info "Your time-zone. If the Auto-Detection is incorrect, email raf@clojure.camp"}
   [:label {:tw "flex gap-2"}
    [:select {:tw "p-1 bg-white border border-gray-300 font-light"
              :value @(mod/subscribe [:user-profile-value :user/time-zone])
              :on-change (fn [e]
                           (mod/dispatch [:set-user-value! :user/time-zone (.. e -target -value)]))}
     (for [timezone (.supportedValuesOf js/Intl "timeZone")]
       ^{:key timezone}
       [:option {:value timezone} timezone])]
    [ui/button
     {:on-click (fn []
                  (mod/dispatch
                   [:set-user-value! :user/time-zone (.. js/Intl DateTimeFormat resolvedOptions -timeZone)]))}
     "Re-Auto-Detect"]]])

(defn subscription-toggle-view []
  [ui/row
   {:title "Participate in P2P Pairing?"
    :info "Set to No to stop receiving weekly opt-in emails."}
   [ui/radio-list
    {:choices [[true "Yes"]
               [false "No"]]
     :value @(mod/subscribe [:user-profile-value :user/subscribed?])
     :on-change (fn [value]
                  (mod/dispatch [:update-subscription! value]))}]])

(defn format-date-2 [date]
  (.format (js/Intl.DateTimeFormat. "default" #js {:day "numeric"
                                                   :month "short"
                                                   :year "numeric"
                                                   :hour "numeric"})
           date))

(defn event-view [heading event]
  (let [guest-name (:user/name (:event/other-guest event))
        other-guest-flagged? (contains? (:event/flagged-guest-ids event)
                                        (:user/id (:event/other-guest event)))]
    [:tr.event {:class (if (< (js/Date.) (:event/at event))
                         "future"
                         "past")}
     [:th heading]
     [:td
      [:span.at (format-date-2 (:event/at event))]
      " with "
      [:span.other-guest (:user/name (:event/other-guest event))]]
     [:td
      [:div.actions
       [:a.link {:href (str "mailto:" (:user/email (:event/other-guest event)))}
        [fa/fa-envelope-solid]]
       [:a.link {:href (util/->event-url event)}
        [fa/fa-video-solid]]
       [:button.flag
        {:class (when other-guest-flagged? "flagged")
         :on-click (fn []
                     (if other-guest-flagged?
                       (mod/dispatch [:flag-event-guest! (:event/id event) false])
                       (when (js/confirm (str "Are you sure you want to report " guest-name " for not showing up?"))
                         (mod/dispatch [:flag-event-guest! (:event/id event) true]))))}
        [fa/fa-flag-solid]]]]]))

(defn events-view []
  [ui/row
   {}
   (let [events @(mod/subscribe [:events])
         [upcoming-events past-events] (->> events
                                            (sort-by :event/at)
                                            reverse
                                            (split-with (fn [event]
                                                          (< (js/Date.) (:event/at event)))))]
     [:table.events
      [:tbody
       (for [[index event] (map-indexed vector upcoming-events)]
         ^{:key (:event/id event)}
         [event-view (when (= 0 index) "Upcoming Sessions") event])
       (for [[index event] (map-indexed vector past-events)]
         ^{:key (:event/id event)}
         [event-view (when (= 0 index) "Past Sessions") event])]])])

(defn p2p-page-view []
  [:div.page.p2p
   [ui/row
    {}
    [:div {:tw "text-sm space-y-2"}
     [:p "Get scheduled weekly for casual 1:1 study sessions with another student or mentor."]
     [:p "How it works: on Friday evening each week, you'll get an email asking if you're available for the next week. If you opt-in, on Sunday night you'll receive an email with your session schedule."]
     [:p "You can configure your availability, max # of sessions, and other settings below."]]]
   (when @(mod/subscribe [:user-profile-value :user/subscribed?])
     [:<>
      [opt-in-view]
      [pair-with-view]
      [availability-view]
      [time-zone-view]
      [max-limit-preferences-view]
      [events-view]])
   [subscription-toggle-view]])

(mod/register-page!
 {:page/id :page.id/p2p
  :page/path "/p2p"
  :page/nav-label "Pairing"
  :page/view #'p2p-page-view
  :page/styles styles/styles
  :page/on-enter! (fn []
                    (mod/dispatch [:p2p/fetch-events!]))})
