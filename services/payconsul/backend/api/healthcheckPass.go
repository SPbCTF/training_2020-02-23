package api

import (
	"backend/handlers"
	"backend/metrics"
	"database/sql"
	"github.com/sirupsen/logrus"
	"github.com/spf13/viper"
	"net/http"
)

func HealthPass(w http.ResponseWriter, r *http.Request) {
	metrics.RequestsCount.Inc()

	if r.Method != http.MethodPost {
		metrics.ErrorCount.Inc()
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	user := handlers.CheckAuth(r)
	if user == nil {
		metrics.ErrorCount.Inc()
		logrus.Warnf("User not found. Need to sign in")
		http.Error(w, "Not authorized", http.StatusUnauthorized)
		return
	}

	err := r.ParseForm()
	if err != nil {
		metrics.ErrorCount.Inc()
		logrus.WithError(err).Errorf("Can't parse form")
		http.Error(w, "Can't parse form", http.StatusInternalServerError)
		return
	}

	serviceName := r.Form.Get("service")

	db, err := sql.Open("sqlite3", viper.GetString("DSN"))
	if err != nil {
		metrics.ErrorCount.Inc()
		logrus.WithError(err).Errorf("Can't open database")
		http.Error(w, "Can't open database", http.StatusInternalServerError)
		return
	}

	defer db.Close()

	_, err = db.Exec("UPDATE services SET healthy=1 WHERE name=$1", serviceName)
	if err != nil {
		metrics.ErrorCount.Inc()
		http.Error(w, "Can't pass healthcheck", http.StatusInternalServerError)
		return
	}
}
