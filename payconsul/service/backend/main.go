package main

import (
	"backend/api"
	"backend/handlers"
	"backend/metrics"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/sirupsen/logrus"
	"github.com/spf13/viper"
	"io/ioutil"
	"net/http"
	"os"
)

func main() {
	viper.AutomaticEnv()

	logrus.SetFormatter(&logrus.TextFormatter{
		ForceColors:               true,
	})
	logrus.SetOutput(os.Stdout)

	token, err := ioutil.ReadFile("/token")
	if err != nil {
		logrus.WithError(err).Errorf("Can't read token")
		return
	}

	viper.Set("CHECKER_TOKEN", string(token))

	http.HandleFunc("/", handlers.Index)
	http.HandleFunc("/services", handlers.Services)

	http.HandleFunc("/login", handlers.Login)
	http.HandleFunc("/register", handlers.Register)
	http.HandleFunc("/logout", handlers.Logout)

	http.HandleFunc("/v1/register", api.Register)
	http.HandleFunc("/v1/deregister", api.Deregister)
	http.HandleFunc("/v1/buy", api.Buy)

	http.HandleFunc("/v1/health/pass", api.HealthPass)
	http.HandleFunc("/v1/health/fail", api.HealthFail)

	http.Handle("/metrics", promhttp.Handler())

	metrics.Version.Set(viper.GetFloat64("VERSION"))

	logrus.Fatal(http.ListenAndServe(viper.GetString("LISTEN"), nil))
}
