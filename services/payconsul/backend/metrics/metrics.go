package metrics

import "github.com/prometheus/client_golang/prometheus"

var (
	RequestsCount = prometheus.NewCounter(prometheus.CounterOpts{
		Name: "requests_count",
		Help: "Total requests count",
	})

	ErrorCount = prometheus.NewCounter(prometheus.CounterOpts{
		Name: "error_count",
		Help: "Error count",
	})

	Version = prometheus.NewGauge(prometheus.GaugeOpts{
		Name: "version",
		Help: "Version of this app",
	})
)

func init() {
	prometheus.MustRegister(RequestsCount)
	prometheus.MustRegister(ErrorCount)
	prometheus.MustRegister(Version)
}
