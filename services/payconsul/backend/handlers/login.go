package handlers

import (
	"backend/metrics"
	"database/sql"
	"github.com/sirupsen/logrus"
	"github.com/spf13/viper"
	"html/template"
	"net/http"
)

func Login(w http.ResponseWriter, r*http.Request) {
	metrics.RequestsCount.Inc()

	if r.Method == http.MethodGet {
		tmpl, err := template.New("login.html").ParseFiles("frontend/login.html")
		if err != nil {
			metrics.ErrorCount.Inc()
			logrus.WithError(err).Errorf("Can't parse login.html")
			http.Error(w, "Can't parse template", http.StatusInternalServerError)
			return
		}

		err = tmpl.Execute(w, nil)
		if err != nil {
			metrics.ErrorCount.Inc()
			logrus.WithError(err).Errorf("Can't execute login.html")
			http.Error(w, "Can't execute template", http.StatusInternalServerError)
			return
		}
	} else if r.Method == http.MethodPost {
		err := r.ParseForm()
		if err != nil {
			metrics.ErrorCount.Inc()
			logrus.WithError(err).Errorf("Can't parse form")
			http.Error(w, "Can't parse form", http.StatusInternalServerError)
			return
		}

		name := r.Form.Get("name")
		password := r.Form.Get("password")

		db, err := sql.Open("sqlite3", viper.GetString("DSN"))
		if err != nil {
			metrics.ErrorCount.Inc()
			logrus.WithError(err).Errorf("Can't open database")
			http.Error(w, "Can't open database", http.StatusInternalServerError)
			return
		}

		defer db.Close()
		row := db.QueryRow("SELECT session FROM users WHERE name = $1 AND password = $2",
			name, password)

		var sessID string
		err = row.Scan(&sessID)
		if err != nil {
			metrics.ErrorCount.Inc()
			logrus.WithError(err).Errorf("Can't scan from users")
			http.Error(w, "User not found", http.StatusForbidden)
			return
		}

		http.SetCookie(w, &http.Cookie{
			Name:       "session_id",
			Value:      sessID,
		})

		http.Redirect(w, r, "/", http.StatusSeeOther)
		return

	} else {
		http.Error(w, "Not implemented", http.StatusMethodNotAllowed)
	}
}
