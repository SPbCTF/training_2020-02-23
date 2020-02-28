package api

import (
	"backend/handlers"
	"backend/metrics"
	"database/sql"
	"github.com/sirupsen/logrus"
	"github.com/spf13/viper"
	"net/http"
)

func Buy(w http.ResponseWriter, r *http.Request) {
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
	token := r.Form.Get("token")

	if token != viper.GetString("CHECKER_TOKEN") && user.Quota < 2 {
		metrics.ErrorCount.Inc()
		logrus.Warnf("Not enough quota to buy info")
		http.Error(w, "Not enough quota", http.StatusForbidden)
		return
	}

	db, err := sql.Open("sqlite3", viper.GetString("DSN"))
	if err != nil {
		metrics.ErrorCount.Inc()
		logrus.WithError(err).Errorf("Can't open database")
		http.Error(w, "Can't open database", http.StatusInternalServerError)
		return
	}

	defer db.Close()

	var meta string

	row := db.QueryRow("SELECT meta FROM services WHERE name=$1", serviceName)
	if err = row.Scan(&meta); err != nil {
		metrics.ErrorCount.Inc()
		http.Error(w, "Can't get info about service", http.StatusInternalServerError)
		return
	}

	_, err = db.Exec("UPDATE users SET quota=quota-2 WHERE name=$1", user.Name)
	if err != nil {
		metrics.ErrorCount.Inc()
		http.Error(w, "Can't decrease qouta of user", http.StatusInternalServerError)
		return
	}

	w.Write([]byte(meta))
	return
}

