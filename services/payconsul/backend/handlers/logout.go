package handlers

import (
	"backend/metrics"
	"github.com/sirupsen/logrus"
	"net/http"
	"time"
)

func Logout(w http.ResponseWriter, r*http.Request) {
	metrics.RequestsCount.Inc()

	user := CheckAuth(r)
	if user == nil {
		logrus.Warnf("User not found. Redirect to /login")
		http.Redirect(w, r, "/login", http.StatusSeeOther)
		return
	}

	http.SetCookie(w, &http.Cookie{
		Name:       "session_id",
		Expires:    time.Unix(0, 0),
		MaxAge:     -1,
	})

	http.Redirect(w, r, "/login", http.StatusSeeOther)
}

