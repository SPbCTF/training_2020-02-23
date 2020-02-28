package api

import (
	"backend/handlers"
	"backend/metrics"
	"database/sql"
	"github.com/sirupsen/logrus"
	"github.com/spf13/viper"
	"net/http"
)

func Deregister(w http.ResponseWriter, r *http.Request) {
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

	tx, err := db.Begin()
	if err != nil {
		metrics.ErrorCount.Inc()
		logrus.WithError(err).Errorf("Can't start transaction")
		http.Error(w, "Can't start transaction", http.StatusInternalServerError)
		return
	}

	_, err = tx.Exec("DELETE FROM services WHERE name=$1 AND author=$2",
		serviceName, user.Name)
	if err != nil {
		metrics.ErrorCount.Inc()
		http.Error(w, "Can't deregister service", http.StatusInternalServerError)
		return
	}

	_, err = tx.Exec("UPDATE users SET quota=quota+1 WHERE name=$1", user.Name)
	if err != nil {
		metrics.ErrorCount.Inc()
		http.Error(w, "Can't deregister service", http.StatusInternalServerError)
		return
	}

	err = tx.Commit()
	if err != nil {
		metrics.ErrorCount.Inc()
		logrus.WithError(err).Errorf("Can't commit transaction")
		http.Error(w, "Can't commit transaction", http.StatusInternalServerError)
		return
	}
}
