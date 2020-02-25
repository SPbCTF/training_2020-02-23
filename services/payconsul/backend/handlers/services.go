package handlers

import (
	"backend/metrics"
	"database/sql"
	"github.com/sirupsen/logrus"
	"github.com/spf13/viper"
	"html/template"
	"net/http"
)

func Services(w http.ResponseWriter, r*http.Request) {
	metrics.RequestsCount.Inc()

	user := CheckAuth(r)
	if user == nil {
		metrics.ErrorCount.Inc()
		logrus.Warnf("User not found. Redirect to /login")
		http.Redirect(w, r, "/login", http.StatusSeeOther)
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

	rows, err := db.Query("SELECT name, address, port, healthy FROM services ORDER BY creation DESC LIMIT 50")
	if err != nil {
		metrics.ErrorCount.Inc()
		logrus.WithError(err).Error("Can't get services list")
		http.Error(w, "Can't get services list", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var services []Service
	for rows.Next() {
		var service Service
		err = rows.Scan(&service.Name, &service.Address, &service.Port, &service.Healthy)
		if err != nil {
			metrics.ErrorCount.Inc()
			logrus.WithError(err).Errorf("Can't scan service")
			http.Error(w, "Can't scan service", http.StatusInternalServerError)
			return
		}

		services = append(services, service)
	}

	tmpl, err := template.New("services.html").ParseFiles("frontend/services.html")
	if err != nil {
		metrics.ErrorCount.Inc()
		logrus.WithError(err).Errorf("Can't parse services.html")
		http.Error(w, "Can't parse template", http.StatusInternalServerError)
		return
	}

	err = tmpl.Execute(w, Result{
		Services: services,
		User:     *user,
		Version:  viper.GetInt64("ID"),
	})
	if err != nil {
		metrics.ErrorCount.Inc()
		logrus.WithError(err).Errorf("Can't execute services.html")
		http.Error(w, "Can't execute template", http.StatusInternalServerError)
		return
	}
}
