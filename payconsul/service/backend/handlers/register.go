package handlers

import (
	"backend/metrics"
	"database/sql"
	uuid "github.com/satori/go.uuid"
	"github.com/sirupsen/logrus"
	"github.com/spf13/viper"
	"html/template"
	"net/http"
	"strings"
)

func Register(w http.ResponseWriter, r *http.Request) {
	metrics.RequestsCount.Inc()

	if r.Method == http.MethodGet {
		tmpl, err := template.New("register.html").ParseFiles("frontend/register.html")
		if err != nil {
			metrics.ErrorCount.Inc()
			logrus.WithError(err).Errorf("Can't parse register.html")
			http.Error(w, "Can't parse template", http.StatusInternalServerError)
			return
		}

		err = tmpl.Execute(w, nil)
		if err != nil {
			metrics.ErrorCount.Inc()
			logrus.WithError(err).Errorf("Can't execute register.html")
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

		sessID := uuid.NewV4().String()
		_, err = db.Exec("INSERT INTO users (name, quota, password, session) VALUES ($1, $2, $3, $4)",
			name, 1, password, sessID)

		if err != nil {
			if strings.HasPrefix(err.Error(), "UNIQUE constraint failed") {
				http.Error(w, "user already exists", http.StatusBadRequest)
				return
			}

			metrics.ErrorCount.Inc()
			logrus.WithError(err).Errorf("Can't exec SQL")
			http.Error(w, "Can't register user", http.StatusInternalServerError)
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
