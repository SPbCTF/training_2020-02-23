package api

import (
	"backend/handlers"
	"backend/metrics"
	"database/sql"
	"github.com/sirupsen/logrus"
	"github.com/spf13/viper"
	"net"
	"net/http"
	"strconv"
	"strings"
)

func Register(w http.ResponseWriter, r *http.Request) {
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
	address := r.Form.Get("address")
	portStr := r.Form.Get("port")
	meta := r.Form.Get("meta")

	port, err := strconv.ParseInt(portStr, 10, 64)
	if err != nil {
		metrics.ErrorCount.Inc()
		logrus.WithError(err).Errorf("Can't parse port")
		http.Error(w, "Can't parse port", http.StatusBadRequest)
		return
	}

	ip := net.ParseIP(address)
	if ip == nil {
		metrics.ErrorCount.Inc()
		http.Error(w, "Not a valid IP addess", http.StatusBadRequest)
		return
	}

	if ip.To4() == nil && ip.To16() == nil {
		metrics.ErrorCount.Inc()
		http.Error(w, "Not a valid IP addess", http.StatusBadRequest)
		return
	}

	if user.Quota == 0 {
		metrics.ErrorCount.Inc()
		logrus.Warnf("User has no qouta.")
		http.Error(w, "Quota exceeded", http.StatusBadRequest)
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

	tx, err := db.Begin()
	if err != nil {
		metrics.ErrorCount.Inc()
		logrus.WithError(err).Errorf("Can't start transaction")
		http.Error(w, "Can't start transaction", http.StatusInternalServerError)
		return
	}
	defer tx.Commit()

	_, err = tx.Exec("INSERT INTO services VALUES($1, $2, $3, $4, $5, 0, strftime('%s','now'))",
		serviceName, ip.String(), port, meta, user.Name)
	if err != nil {
		if strings.HasPrefix(err.Error(), "UNIQUE constraint failed") {
			http.Error(w, "service already exists", http.StatusBadRequest)
			return
		}

		metrics.ErrorCount.Inc()
		http.Error(w, "Can't register service", http.StatusInternalServerError)
		return
	}

	_, err = tx.Exec("UPDATE users SET quota=0 WHERE name=$1", user.Name)
	if err != nil {
		metrics.ErrorCount.Inc()
		http.Error(w, "Can't register service", http.StatusInternalServerError)
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
